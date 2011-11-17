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
      (let [matcher (.matcher #"([\w/]+)\s+([\w/]+)\s*$" (subs string (+ tmpl-tag-start start-marker-length) tmpl-tag-end))]
        (if (.find matcher)
          [(.group matcher 1)
           (.group matcher 2)
           (subs string 0 tmpl-tag-start)
           (subs string (+ tmpl-tag-end end-marker-length))]
          (error "No valid tag definition found." )))))

  (defn- parse-if-like-tmpl-tag [tmpl-tag]
    (let [string (:next-string tmpl-tag)
          tag-string (:tag tmpl-tag)]
      (letfn [(get-matcher [pattern]
                (let [m (.matcher pattern string)]
                  (when (.find m)
                    [(.group m 1) (when (> (.groupCount m) 1) (.group m 2)) (subs string (.end m))])))]
        (if-let [parts (or (get-matcher (Pattern/compile (str "(.+)<!--\\s*TMPL_ELSE\\s*-->(.+)<!--\\s*/"
                                                              tag-string
                                                              "\\s*-->")))
                           (get-matcher (Pattern/compile (str "(.+)<!--\\s*/"
                                                              tag-string
                                                              "\\s*-->"))))]
          parts
          (error "Invalid " tag-string " near " (subs string 0 (min 20 (count string)))))))))

(defmulti %parse-tmpl-tag :tag)

(defn- %simple-parse-tmpl-tag-using-keyword [tmpl-tag]
  (-> tmpl-tag
      (assoc :attr-converter keyword)))

(defmethod %parse-tmpl-tag "TMPL_VAR" [var-tag]
  (%simple-parse-tmpl-tag-using-keyword var-tag))

(defmethod %parse-tmpl-tag "TMPL_IF" [if-tag]
  (let [[then-string else-string next-string] (parse-if-like-tmpl-tag if-tag)]
    (-> if-tag
        (assoc :attr-converter keyword)
        (assoc :then then-string)
        (assoc :else else-string)
        (assoc :next-string next-string))))

(defmethod %parse-tmpl-tag "TMPL_UNLESS" [unless-tag]
  (let [[then-string else-string next-string] (parse-if-like-tmpl-tag unless-tag)]
    (-> unless-tag
        (assoc :attr-converter keyword)
        (assoc :then then-string)
        (assoc :else else-string)
        (assoc :next-string next-string))))

(defn- parse-loop-like-tmpl-tag [tmpl-tag]
  (let [next-string (:next-string tmpl-tag)
        tag-string (:tag tmpl-tag)
        matcher (.matcher (Pattern/compile (str "(.+)<!--\\s*/" tag-string "\\s*-->")) next-string)]
    (when-not (.find matcher)
      (error "Invalid " tag-string " near " (subs next-string 0 (min 20 (count next-string)))))
    [(.group matcher 1) (subs next-string (.end matcher))]))

(defmethod %parse-tmpl-tag "TMPL_LOOP" [loop-tag]
  (let [[loop-string next-string] (parse-loop-like-tmpl-tag loop-tag)]
    (-> loop-tag
        (assoc :attr-converter keyword)
        (assoc :loop loop-string)
        (assoc :next-string next-string))))

(defmethod %parse-tmpl-tag "TMPL_REPEAT" [loop-tag]
  (let [[loop-string next-string] (parse-loop-like-tmpl-tag loop-tag)]
    (-> loop-tag
        (assoc :attr-converter keyword)
        (assoc :loop loop-string)
        (assoc :next-string next-string))))

(defmethod %parse-tmpl-tag "TMPL_INCLUDE" [include-tag]
  (-> include-tag
      (assoc :attr-converter identity)))

(defmethod %parse-tmpl-tag "TMPL_CALL" [call-tag]
  (%simple-parse-tmpl-tag-using-keyword call-tag))

;; tag, element, attribute, value
(defn- parse-tmpl-tag [string]
  (let [[tag-element attribute prev-string next-string] (get-one-tmpl-tag-element string)]
    (if tag-element
      (%parse-tmpl-tag {:tag tag-element :attr attribute :prev-string prev-string :next-string next-string})
      string)))

(defmacro with-tmpl-value [[tmpl-value tmpl-tag] & body]
  `(let [~tmpl-value ((:attr-converter ~tmpl-tag) (:attr ~tmpl-tag))]
     ~@body))

(defmulti make-one-tmpl-printer :tag)

(defn create-tmpl-printer [string]
  (loop [tag-map (parse-tmpl-tag string) printers []]
    (if (string? tag-map)
      (fn [env]
        (if (empty? printers)
          tag-map
          (apply str (conj ((apply juxt printers) env) tag-map))))
      (recur (parse-tmpl-tag (:next-string tag-map)) (conj printers (make-one-tmpl-printer tag-map))))))

(defmethod make-one-tmpl-printer "TMPL_VAR" [var-tag]
  (with-tmpl-value [tmpl-value var-tag]
    (fn [env]
      (str (:prev-string var-tag) (tmpl-value env)))))

(defn- %make-if-unless-printer [if-unless-tag function tag-string]
  (with-tmpl-value [tmpl-value if-unless-tag]
    (let [then-printer (create-tmpl-printer (:then if-unless-tag))
          else-printer (when-let [else-string (:else if-unless-tag)]
                         (create-tmpl-printer else-string))]
      (fn [env]
        (str (:prev-string if-unless-tag)
             (if (function (tmpl-value env))
               (then-printer env)
               (when else-printer
                 (else-printer env))))))))

(defmethod make-one-tmpl-printer "TMPL_IF" [if-tag]
  (%make-if-unless-printer if-tag identity "TMPL_IF"))

(defmethod make-one-tmpl-printer "TMPL_UNLESS" [unless-tag]
  (%make-if-unless-printer unless-tag not "TMPL_UNLESS"))

(defmethod make-one-tmpl-printer "TMPL_LOOP" [loop-tag]
  (with-tmpl-value [tmpl-value loop-tag]
    (let [loop-printer (create-tmpl-printer (:loop loop-tag))]
      (fn [env]
        (loop [[current-env & next-env-list] (tmpl-value env) acc []]
          (if (empty? current-env)
            (apply str (:prev-string loop-tag) acc)
            ;; Merge {global env} and {local current-env} -
            ;; {local current-env} will survive when merged
            (recur next-env-list (conj acc (loop-printer (merge env current-env))))))))))

(defmethod make-one-tmpl-printer "TMPL_REPEAT" [repeat-tag]
  (with-tmpl-value [tmpl-value repeat-tag]
    (let [repeat-printer (create-tmpl-printer (:loop repeat-tag))]
      (fn [env]
        (let [must-be-number (tmpl-value env)]
          (apply str
                 (:prev-string repeat-tag)
                 (when (number? must-be-number)
                   (loop [n must-be-number acc []]
                     (if (pos? n)
                       (recur (dec n) (conj acc (repeat-printer env)))
                       acc)))))))))

(defn- include-tmpl [path]
  ;; valid path?
  "FIXME")

(defmethod make-one-tmpl-printer "TMPL_INCLUDE" [include-tag]
  (with-tmpl-value [tmpl-value include-tag]
    (let [included-string (include-tmpl tmpl-value)]
      (fn [env]
        (str (:prev-string include-tag) included-string)))))

(defmethod make-one-tmpl-printer "TMPL_CALL" [call-tag]
  (with-tmpl-value [tmpl-value call-tag]
    (fn [env]
      (loop [[path current-env & next-parts] (tmpl-value env) string-acc []]
        (if (empty? path)
          (apply str (:prev-string call-tag) string-acc)
          ;; merge {global env} and {local current-env}.
          ;; {local current-env} will survive when merged
          (recur next-parts (conj string-acc ((create-tmpl-printer (include-tmpl path)) (merge env current-env)))))))))

;;; TEMPLATE.CLJ ends here
