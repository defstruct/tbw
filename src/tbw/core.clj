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

(ns tbw.core
  (:use [ring.middleware.params :only [wrap-params]]
        [clojure.contrib.seq :only [positions]]
        [tbw.specials]
        [tbw.template :only [create-tmpl-printer]]
        [tbw.util :only [ignore-errors error warn rfc-1123-date with-existing-file]])
  (:import [java.io File FileInputStream]
           [java.nio.channels FileChannel FileChannel$MapMode]
           [java.nio.charset Charset]
           [java.util Date]
           [java.util.regex Pattern]))

;; Special variable and functions for the request
(def ^{:dynamic true} *request*)
(def ^{:dynamic true} *response*)

;; From ring-devel/src/ring/handler/dump.clj, and same as
;; keywords in build-request-map function (ring-servlet/src/ring/util/servlet.clj) ?
(def request-keys [:server-port :server-name :remote-addr :uri :query-string :scheme :request-method
                   :content-type :content-length :character-encoding :headers :body
                   ;; After calling wrap-params, two additional keys:
                   :query-params :form-params])

(defn- request-reader*-form [key]
  (let [fn-name (symbol (str (name key) "*"))]
    `(defn ~fn-name
       ([] (~fn-name *request*))
       ([~'request] (~key ~'request)))))

(defmacro define-request-readers
  "Define <reader>* functions. When a reader called without any argument, dynamic variable *request* will be used."
  [& r-keys]
  {:pre [(or (nil? r-keys) (= (count r-keys) 1))]}
  (let [r-keys (or (first r-keys) request-keys)]
    `(do ~@(map request-reader*-form r-keys))))

;; Populate request readers
(define-request-readers)

;; Global mapping used in handle-request
(def tbw-sites (ref []))

(defrecord TemplateBasedWebSite [script->html-template uri-prefix default-html-page site-dispatchers])

(defn make-tbw-site [& {:keys [script->html-template uri-prefix default-html-page site-dispatchers]
                         :or {uri-prefix "/"}}]
  {:pre [(identity script->html-template) (identity default-html-page) (identity site-dispatchers) (and (= (get uri-prefix 0) \/)
                                                                       (not (= (get uri-prefix (dec (count uri-prefix)))
                                                                               \/)))]}
  (let [new-site (TemplateBasedWebSite. script->html-template
                                        uri-prefix
                                        default-html-page
                                        site-dispatchers)]


    new-site))

(defn- handle-static-file [^File file]
  (let [last-modified (rfc-1123-date (Date. (.lastModified file)))]
    (if (= (get (headers*) "if-modified-since") last-modified)
      {:status +http-not-modified+}
      {:body file
       :headers {"last-modified" (rfc-1123-date (Date. last-modified))}})))

(defn- create-static-file-dispatcher [prefix file]
  (let [regex (Pattern/compile (str prefix "$"))]
    #(when (re-find regex (uri*))
       (fn [] (handle-static-file file)))))

(defn- create-folder-dispatcher [prefix file]
  (let [regex   (Pattern/compile (str "^" prefix))]
    #(let [uri (uri*)
           matcher (.matcher regex uri)]
       ;;(println `(:uri ~uri :regex ~regex))
       (when (.find matcher)
         ;;(println `(:got2 ,(subs uri (.end matcher))))
         (fn [] (handle-static-file (File. file (subs uri (.end matcher)))))))))

(defn update-tbw-sites! [site-obj]
  (let [new-uri-prefix (:uri-prefix site-obj)
        [existing-pos] (take 1 (positions #(= (:uri-prefix %) new-uri-prefix) @tbw-sites))]
    (if (integer? existing-pos)
      (do
        (warn "Existing definition found for URI" new-uri-prefix)
        (dosync (alter tbw-sites assoc existing-pos site-obj)))
      (dosync
       (alter tbw-sites conj site-obj)))))

(defn make-resource-dispatchers [resource-dispatchers]
  (vec (map (fn [[prefix {type :type  path :path}]]
              (with-existing-file [file path :cwd true]
                (let [file? (= type :file)]
                  (assert (=  (.isFile file) file?))
                  (if file?
                    (create-static-file-dispatcher prefix file)
                    (create-folder-dispatcher prefix file)))))
            resource-dispatchers)))

(defn- canonicalize-resource-dispatchers "Build sorted and prefixed resource dispatcher defs."
  [prefix resource-dispatchers]
  (loop [defs resource-dispatchers file-defs [] folder-defs []]
    (let [[key val] (first defs)]
      (cond (empty? key) (into {} `(~@(sort file-defs) ~@(sort folder-defs)))
            (= (:type val) :file) (recur (rest defs) (conj file-defs [key val]) folder-defs)
            (= (:type val) :folder) (recur (rest defs) file-defs (conj folder-defs [(str prefix key) val]))
            :else (error "Unknown resource type: " (:type val))))))

;; FIXME: is this really necessary? Make it simple
;;              Need to add/pass encoding info at def-tbw level
(defn- make-apply-env-fn [env-fn html-file]
  (let [file-channel (.getChannel (FileInputStream. html-file))]
    (fn []
      ;;(println `(~(form-params*) ~(env-fn)))
      ((create-tmpl-printer (.. (Charset/forName "UTF-8")
                                newDecoder
                                (decode (.map file-channel FileChannel$MapMode/READ_ONLY 0 (.size file-channel)))
                                toString))
       (env-fn)))))

(defn canonicalize-html-dispatchers [uri-prefix html-folder html-page->env-mappers]
  (with-existing-file [html-folder html-folder :cwd true]
    (when-not (.isDirectory html-folder)
      (error html-folder "is not a folder."))

    (into {} (map (fn [[file env-fn]]
                    (let [html-file (File. html-folder file)]
                      (when-not (.exists html-file)
                        (error "HTML file " (.toString html-file) " does not exist."))
                      (when-not (.isFile html-file)
                        (error html-folder "is not a file."))
                      (when-not (.canRead html-file)
                        (error html-folder "is not readable."))
                      [(str uri-prefix "/" file) (make-apply-env-fn env-fn html-file)]))
                  html-page->env-mappers))))

(defmacro def-tbw [site-name [& {:keys [uri-prefix]}]
                   & {:keys [resource-dispatchers html-folder html-page->env-mappers default-html-page]}]
  {:pre [resource-dispatchers html-page->env-mappers
         default-html-page
         html-folder]}
  (let [resource-dispatchers (canonicalize-resource-dispatchers uri-prefix resource-dispatchers)]
    `(do
       (update-tbw-sites! (make-tbw-site :script->html-template (canonicalize-html-dispatchers ~uri-prefix
                                                                                               ~html-folder
                                                                                               ~html-page->env-mappers)
                                         :uri-prefix ~uri-prefix
                                         :default-html-page ~default-html-page
                                         :site-dispatchers (make-resource-dispatchers ~resource-dispatchers))))))

(defn- default-handler []
  ;; FIXME: logging
  {:status +http-bad-request+
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body "<html><head><title>tbw</title></head><body><h2>tbw Default Page</h2><p>This is the tbw default page. You're most likely seeing it because the server administrator hasn't set up a custom default page yet.</p></body></html>"})

(defn- run-html-dispatcher [html-dispatcher site]
  (binding [*response* {:status
  {:status
   :headers
   :body (html-dispatcher)})

(defn- call-request-handlers [site script-name]
  (if-let [html-dispatcher (get (:script->html-template site) script-name)]
    (run-html-dispatcher html-dispatcher site)
    (loop [[dispatcher & rest] (:site-dispatchers site)]
      ;;    (println `(:call-request-handlers ~dispatcher))
      (if dispatcher
        (if-let [handler (dispatcher)]
          (if-let [response (ignore-errors (handler))]
            response
            (recur rest))
          (recur rest))
        (default-handler)))))

(def tbw-handle-request
  (wrap-params (fn [request]
                 (binding [*request* request]
                   ;;(println *request*)
                   (let [uri (uri*)
                         uri-prefix (apply subs uri (take 2 (positions #(= % \/) uri)))
                         [site] (take 1 (filter (fn [site-def]
                                                  (= (:uri-prefix site-def) uri-prefix))
                                                @tbw-sites))]
                     ;;      (println `(:site ~site :empty? ~(empty? site) :uri-prefix ,uri-prefix))
                     (if (empty? site)
                       ;; call default handler
                       (default-handler)
                       (call-request-handlers site uri)))))))

;;; CORE.CLJ ends here
