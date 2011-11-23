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

(ns tbw.util
  (:use [clojure.contrib.macro-utils :only [macrolet]])
  (:import [java.util Date]
           [java.util TimeZone]
           [java.io File]
           [java.text SimpleDateFormat]))

(defmacro ignore-errors [& forms]
  `(try (do ~@forms)
        (catch java.lang.Exception _# nil)))

(defn error [& args]
  (throw (Exception. (apply str args))))

(defn warn [& args]
  (apply println "*** Warning:" args))

(def rfc-1123-date-format (doto (SimpleDateFormat. "E, dd MMM yyyy HH:mm:ss z")
                            (.setTimeZone (TimeZone/getTimeZone "GMT+0:0"))))

(defn rfc-1123-date
  ([] (rfc-1123-date (Date.)))
  ([^Date date]
     (.format rfc-1123-date-format date)))

(defmacro with-existing-file [[file path & {:keys [parent cwd]}] & body]
  {:pre [(not (and parent cwd))]}
  (let [path-sym (gensym "path")]
    `(let [~path-sym ~path
           file1# (File. ~path-sym)
           file2# ~(cond parent `(File. ~parent ~path-sym)
                         cwd    `(File. (System/getProperty "user.dir") ~path-sym)
                         :else nil)
           ~file (cond (.exists file1#) file1#
                       (and (not (nil? file2#)) (.exists file2#)) file2#)]
       (when-not ~file
         (error "File does not exist: " (.toString file1#)
                (when file2#
                  (str " or " (.toString file2#)))))
       ~@body)))

;;; UTIL.CLJ ends here
