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
  (:use [tbw.util :only [error]])
  (:import [java.util.regex Pattern]))

(def *template-start-marker* "<!--")
(def *template-end-marker* "-->")

(let [start-marker-length (count *template-start-marker*)
      end-marker-length (count *template-end-marker*)]
  (defn- get-one-tmpl-tag-element [string]
    (with-tag-marker-indices [string tmpl-tag-start tmpl-tag-end]
      (println (subs string (+ tmpl-tag-start start-marker-length) tmpl-tag-end))
      (let [matcher (.matcher #"\s*(/?(?i)TMPL_[\w/]+)\s+([\-\w/]*)" (subs string (+ tmpl-tag-start start-marker-length) tmpl-tag-end))]
        (if (.find matcher)
          [(.group matcher 1)
           (.group matcher 2)
           (subs string 0 tmpl-tag-start)
           (subs string (+ tmpl-tag-end end-marker-length))]
          (error "No valid tag definition found." ))))))

(defn- parse-tmpl-tag [string]
  (when-let [[tag-element attribute prev-string next-string] (get-one-tmpl-tag-element string)]
    [{:tag tag-element :attr (if (empty? attribute) nil attribute) :prev-string prev-string} next-string]))

;; (defn create-tmpl-printer0 [string]
;;   ;; development helper
;;   (loop [string string stack []]
;;     (let [[tag-map next-string] (parse-tmpl-tag string)]
;;       (if (:tag tag-map)
;;         ;; tag-map {:tag "TMPL_IF" :prev-string "blah" :attr :foo}
;;         (recur next-string (conj stack tag-map))
;;         (let [final-stack (if (empty? string)
;;                             stack
;;                             (conj stack string))]
;;           final-stack)))))

(defn- tmpl-tag-dispatcher [stack]
  (:tag (peek stack)))

(defmulti maybe-reduce-stack tmpl-tag-dispatcher)

(defmethod maybe-reduce-stack "TMPL_VAR" [stack]
  (let [var-tag (peek stack)]
    (conj (pop stack) (fn [env]
                        (str (:prev-string var-tag) ((keyword (:attr var-tag)) env))))))

(defmethod maybe-reduce-stack "TMPL_IF" [stack]
  stack)

(defmethod maybe-reduce-stack "TMPL_ELSE" [stack]
  (loop [if-stack (pop stack) then-stack `(~(fn [_]
                                              (:prev-string (peek stack))))]
    (let [tag-map (peek if-stack)]
      (cond (fn? tag-map) (recur (pop if-stack) (conj then-stack tag-map))
            (empty? tag-map) (error "TMPL_IF not found for TMPL_ELSE in " (:prev-string (peek stack)))
            ;; FIXME: case insensitive comparison
            (= (:tag tag-map) "TMPL_IF") (conj (pop if-stack) (assoc tag-map :then then-stack))
            :else (recur (pop if-stack) (conj then-stack tag-map))))))

(defn- make-if-function
  ([prev-string env-var then]
     (make-if-function prev-string env-var then nil))
  ([prev-string env-var then else]
     (fn [env]
       (apply str prev-string
              (if (env-var env)
                (then env)
                (when else (else env)))))))

(defmethod maybe-reduce-stack "/TMPL_IF" [stack]
  (loop [if-stack (pop stack) then-or-else-stack `(~(fn [_]
                                                      (:prev-string (peek stack))))]
    (let [tag-map (peek if-stack)]
      (cond (fn? tag-map) (recur (pop if-stack) (conj then-or-else-stack tag-map))

            (empty? tag-map) (error "TMPL_IF not found for /TMPL_IF in " (:prev-string (peek stack)))

            (= (:tag tag-map) "TMPL_IF")
            (conj (pop if-stack)
                  (if-let [then-part (:then tag-map)]
                    (make-if-function (:prev-string tag-map)
                                      (keyword (:attr tag-map))
                                      (apply juxt then-part)
                                      (apply juxt then-or-else-stack))
                    (make-if-function (:prev-string tag-map)
                                      (keyword (:attr tag-map))
                                      (apply juxt then-or-else-stack))))

            :else (recur (pop if-stack) (conj then-or-else-stack tag-map))))))

(defmethod maybe-reduce-stack "TMPL_LOOP" [stack]
  stack)

(defmethod maybe-reduce-stack "/TMPL_LOOP" [stack]
  (loop [loop-stack (pop stack) body-stack `(~(fn [_]
                                                (:prev-string (peek stack))))]
    (let [tag-map (peek loop-stack)]
      (cond (fn? tag-map) (recur (pop loop-stack) (conj body-stack tag-map))

            (empty? tag-map) (error "TMPL_LOOP not found for /TMPL_LOOP in " (:prev-string (peek stack)))

            (= (:tag tag-map) "TMPL_LOOP")
            (let [body-fn (apply juxt body-stack)]
              (conj (pop loop-stack)
                    (fn [env]
                      (loop [[current-env & next-env-list] ((keyword (:attr tag-map)) env) acc []]
                        (if (empty? current-env)
                          (apply str (:prev-string tag-map) acc)
                          ;; Merge {global env} and {local current-env} -
                          ;; {local current-env} will survive when merged
                          (recur next-env-list (concat acc (body-fn (merge env current-env)))))))))

            :else (recur (pop loop-stack) (conj body-stack tag-map))))))

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
          (when-not (every? fn? final-stack)
            ;; FIXME: use more infomative function
            (error "Error - unmatched ??"))
          (fn [env]
            ;; Final stack must be functions only
            (apply str ((apply juxt final-stack) env))))))))

;;; TEMPLATE.CLJ ends here
