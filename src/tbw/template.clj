;;;;   -*- Mode: clojure; encoding: utf-8; -*-
;;
;; Copyright (C) 2011 Jong-won Choi
;; All rights reserved.
;; Distributed under the BSD-style license:
;; http://www.opensource.org/licenses/bsd-license.php
;;
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions
;; are met:

;;   * Redistributions of source code must retain the above copyright
;;     notice, this list of conditions and the following disclaimer.

;;   * Redistributions in binary form must reproduce the above
;;     copyright notice, this list of conditions and the following
;;     disclaimer in the documentation and/or other materials
;;     provided with the distribution.

;; THIS SOFTWARE IS PROVIDED BY THE AUTHOR 'AS IS' AND ANY EXPRESSED
;; OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
;; WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
;; ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
;; DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
;; DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
;; GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
;; INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
;; WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
;; NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
;; SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
;;
;;;; Commentary:
;;
;;
;;
;;;; Code:
(ns tbw.template
  (:use [tbw.util :only [error with-existing-file]]
        [clojure.string :only [upper-case]])
  (:import [java.util.regex Pattern]
           [java.io FileInputStream]
           [java.nio.channels FileChannel FileChannel$MapMode]
           [java.nio.charset Charset]))

(def *template-start-marker* "<!--")
(def *template-end-marker* "-->")

(defmacro with-tag-marker-indices [[string tag-start tag-end] & body]
  `(let [new-string# ~string
         ~tag-start (.indexOf new-string# *template-start-marker*)]
     (when-not (neg? ~tag-start)
       (let [~tag-end (.indexOf new-string# *template-end-marker* ~tag-start)]
         (when (neg? ~tag-end)
           (error "Invalid tag defintion: no closing tag found."))
         ~@body))))

(let [start-marker-length (count *template-start-marker*)
      end-marker-length (count *template-end-marker*)]
  (defn- get-one-tmpl-tag-element [string]
    (with-tag-marker-indices [string tmpl-tag-start tmpl-tag-end]
      (let [matcher (.matcher #"\s*(/?(?i)TMPL_[\w/]+)\s+([\-\.\w/]*)" (subs string (+ tmpl-tag-start start-marker-length) tmpl-tag-end))]
        (if (.find matcher)
          [(upper-case (.group matcher 1))
           (.group matcher 2)
           (subs string 0 tmpl-tag-start)
           (subs string (+ tmpl-tag-end end-marker-length))]
          (error "No valid tag definition found." ))))))

(defn- parse-tmpl-tag [string]
  (when-let [[tag-element attribute prev-string next-string] (get-one-tmpl-tag-element string)]
    [{:tag tag-element :attr (if (empty? attribute) nil attribute) :prev-string prev-string} next-string]))

(defn- tmpl-tag-dispatcher [stack]
  (:tag (peek stack)))

(defmulti maybe-reduce-stack tmpl-tag-dispatcher)

(defn- validate-final-tmpl-stack [stack]
  (doseq [maybe-map stack]
    (when (map? maybe-map)
      (error "Non closing open tag " (:tag maybe-map) " after " (:prev-string maybe-map)))))

(defn create-tmpl-printer [string]
  (loop [string string stack []]
    (let [[tag-map next-string] (parse-tmpl-tag string)]
      (if tag-map
        ;; tag-map {:tag "TMPL_IF" :prev-string "blah" :attr :foo}
        (recur next-string (maybe-reduce-stack (conj stack tag-map)))
        (let [final-stack (if (empty? string)
                            stack
                            (conj stack (fn [_]
                                          string)))]
          (validate-final-tmpl-stack final-stack)

          (fn [env]
            ;; Final stack must be functions only
            (apply str ((apply juxt final-stack) env))))))))

(defmacro with-reducing-tmpl-stack [[front-stack back-stack tag-map open-tags close-tag] stack body]
  `(loop [~front-stack (pop ~stack) ~back-stack `(~(fn [~'_]
                                                     (:prev-string (peek ~stack))))]
     (let [~tag-map (peek ~front-stack)]
       (cond (fn? ~tag-map) (recur (pop ~front-stack) (conj ~back-stack ~tag-map))

             (empty? ~tag-map) (error "Possisble open tags" ~(seq open-tags) " not found for " ~close-tag " in "
                                      (:prev-string (peek ~stack)))

             (~(set open-tags) (:tag ~tag-map)) ~body
             :else (recur (pop ~front-stack) (conj ~back-stack ~tag-map))))))

(defmethod maybe-reduce-stack "TMPL_VAR" [stack]
  (let [var-tag (peek stack)]
    (conj (pop stack) (fn [env]
                        (str (:prev-string var-tag) ((keyword (:attr var-tag)) env))))))

(defmethod maybe-reduce-stack "TMPL_IF" [stack]
  stack)

(defmethod maybe-reduce-stack "TMPL_ELSE" [stack]
  (with-reducing-tmpl-stack [if-stack then-stack tag-map ["TMPL_IF" "TMPL_UNLESS"] "TMPL_ELSE"] stack
     (conj (pop if-stack) (assoc tag-map :then then-stack))))

(defn- make-if-function
  ([prev-string env-var then]
     (make-if-function prev-string env-var then nil))
  ([prev-string env-var then else]
     (fn [env]
       (apply str prev-string
              (if (env-var env)
                (then env)
                (when else (else env)))))))

(defn %reduce-if-like-tag [stack cond-function open-tag close-tag]
  (with-reducing-tmpl-stack [if-stack then-or-else-stack tag-map [open-tag] close-tag] stack
    (conj (pop if-stack)
          (if-let [then-part (:then tag-map)]
            (make-if-function (:prev-string tag-map)
                              (cond-function (:attr tag-map))
                              (apply juxt then-part)
                              (apply juxt then-or-else-stack))
            (make-if-function (:prev-string tag-map)
                              (cond-function (:attr tag-map))
                              (apply juxt then-or-else-stack))))))

(defmethod maybe-reduce-stack "/TMPL_IF" [stack]
  (%reduce-if-like-tag stack keyword "TMPL_IF" "/TMPL_IF"))

(defmethod maybe-reduce-stack "TMPL_UNLESS" [stack]
  stack)

(defmethod maybe-reduce-stack "/TMPL_UNLESS" [stack]
  (%reduce-if-like-tag stack (comp complement keyword) "TMPL_UNLESS" "/TMPL_UNLESS"))

(defmethod maybe-reduce-stack "TMPL_LOOP" [stack]
  stack)

(defmethod maybe-reduce-stack "/TMPL_LOOP" [stack]
  (with-reducing-tmpl-stack [loop-stack body-stack tag-map ["TMPL_LOOP"] "/TMPL_LOOP"] stack
    (let [body-fn (apply juxt body-stack)]
      (conj (pop loop-stack)
            (fn [env]
              (loop [[current-env & next-env-list] ((keyword (:attr tag-map)) env) acc []]
                (if (empty? current-env)
                  (apply str (:prev-string tag-map) acc)
                  ;; Merge {global env} and {local current-env} -
                  ;; {local current-env} will survive when merged
                  (recur next-env-list (concat acc (body-fn (merge env current-env)))))))))))

(defmethod maybe-reduce-stack "TMPL_REPEAT" [stack]
  stack)

(defmethod maybe-reduce-stack "/TMPL_REPEAT" [stack]
  (with-reducing-tmpl-stack [repeat-stack body-stack tag-map ["TMPL_REPEAT"] "/TMPL_REPEAT"] stack
    (let [body-fn (apply juxt body-stack)]
      (conj (pop repeat-stack)
            (fn [env]
              (let [repeat-n ((keyword (:attr tag-map)) env)]
                (if (integer? repeat-n)
                  (loop [i repeat-n acc []]
                    (if (<= i 0)
                      (apply str (:prev-string tag-map) acc)
                      (recur (dec i) (concat acc (body-fn env)))))
                  (:prev-string tag-map))))))))

(defn- make-include-function [file-path prev-string]
  (with-existing-file [file file-path :cwd true]
    (let [file-channel (.getChannel (FileInputStream. file))]
      (fn [env]
        (str prev-string
             ;; FIXME: is there simple way without using charset?
             ((create-tmpl-printer (.. (Charset/forName "UTF-8")
                                       newDecoder
                                       (decode (.map file-channel FileChannel$MapMode/READ_ONLY 0 (.size file-channel)))
                                       toString))
              env))))))

(defmethod maybe-reduce-stack "TMPL_INCLUDE" [stack]
  (let [include-tag (peek stack)]
    (conj (pop stack) (make-include-function (:attr include-tag) (:prev-string include-tag)))))

(defn- make-call-function [call-tag]
  (fn [env]
    ;; [[path1 {:var11 val11 :var12 val12}] [path2 {:var21 val22 :var21 val22}] ...]
    (loop [[[path current-env] & next-env-list] ((keyword (:attr call-tag)) env) acc []]
      (if (empty? path)
        (apply str (:prev-string call-tag) acc)
        ;; Merge {global env} and {local current-env} -
        ;; {local current-env} will survive when merged
        (recur next-env-list (concat acc ((make-include-function path "") (merge env current-env))))))))

(defmethod maybe-reduce-stack "TMPL_CALL" [stack]
  (let [call-tag (peek stack)]
    (conj (pop stack) (make-call-function call-tag))))

;;; TEMPLATE.CLJ ends here
