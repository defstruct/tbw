;;;;   -*- Mode: clojure; encoding: utf-8; -*-
;;
;; Copyright (C) 2011 Jong-won Choi
;; All rights reserved.
;; Distributed under the BSD-style license:
;; http://www.opensource.org/licenses/bsd-license.php
;;
;;;; Commentary:
;;
;;
;;
;;;; Code:

(ns tbw.core
  (:use [ring.adapter.jetty :only [run-jetty]]
;;        [clojure.string :only [subs]]
        [clojure.contrib.seq :only [positions]])
  (:import [java.io File]
           [java.util.regex Pattern]
           [java.util Date]
           [java.util TimeZone]
           [java.text SimpleDateFormat]))

;; FIXME: start a utility file?
(defmacro ignore-errors [& forms]
  `(try (do ~@forms)
        (catch java.lang.Exception _# nil)))

(def gmt-date-format (doto (SimpleDateFormat. "E, dd MMM yyyy HH:mm:ss z")
                       (.setTimeZone (TimeZone/getTimeZone "GMT+0:0"))))

(defn rfc-1123-date
  ([] (rfc-1123-date (Date.)))
  ([^Date date]
     (.format gmt-date-format date)))

;; Special variable and functions for the request
(def ^{:dynamic true} *request*)

;; Sigh, this is from ring-devel/src/ring/handler/dump.clj, which (I believe) should be same as
;; keywords in build-request-map function (ring-servlet/src/ring/util/servlet.clj)
(def request-keys [:server-port :server-name :remote-addr :uri :query-string :scheme :request-method
                   :content-type :content-length :character-encoding :headers :body])

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

(defrecord TemplateBasedWebSite [script->html-template uri-prefix home-page-uri site-dispatchers common-template-var-fn])

(defn- make-tbw-site [& {:keys [uri-prefix home-page-uri site-dispatchers common-template-var-fn]
                         :or {uri-prefix "/"}}]
  {:pre [(identity home-page-uri) (identity site-dispatchers) (and (= (get uri-prefix 0) \/)
                                                                       (not (= (get uri-prefix (dec (count uri-prefix)))
                                                                               \/)))]}
  (let [new-site (TemplateBasedWebSite. {} ;; script->html-template
                                        uri-prefix
                                        home-page-uri
                                        site-dispatchers
                                        common-template-var-fn)]


    new-site))

;; FIXME: New file for dispatchers?
;; Dispatchers - stealing from Hunchentoot
;;
(def http-not-modified 304)
(def http-bad-request 400)

(defn- handle-static-file [^File file]
  (let [last-modified (rfc-1123-date (Date. (.lastModified file)))]
    (if (= (get (headers*) "if-modified-since") last-modified)
      {:status http-not-modified}
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

(defn- update-tbw-sites! [site-obj]
  (let [new-uri-prefix (:uri-prefix site-obj)
        [existing-pos] (take 1 (positions #(= (:uri-prefix %) new-uri-prefix) @tbw-sites))]
    (if (integer? existing-pos)
      (do
        ;; FIXME: warn!
        (dosync (alter tbw-sites assoc existing-pos site-obj)))
      (dosync
       (alter tbw-sites conj site-obj)))))

(defn- make-resource-dispatchers [resource-dispatchers]
  (let [cwd (System/getProperty "user.dir")]
    (vec (map (fn [[prefix {type :type  path :path}]]
                (let [file1 (File. path)
                      file2 (File. (str cwd
                                        (when-not (= (get path 0) \/) "/")
                                        path))
                      file? (= type :file)
                      file (cond (.exists file1) file1
                                 (.exists file2) file2
                                 :else (throw (Exception. (str "Expected " (name type) " not found: " file1 " or " file2))))]
                  (assert (=  (.isFile file) file?))
                  (if file?
                    (create-static-file-dispatcher prefix file)
                    (create-folder-dispatcher prefix file))))
              resource-dispatchers))))

(defn- make-html-dispatcher [html-page-defs]
  )

(defn- make-site-dispatchers [resource-dispatchers html-page-defs home-page-uri]
  (conj (make-resource-dispatchers resource-dispatchers) (make-html-dispatcher html-page-defs)))

(defn- canonicalize-resource-dispatchers "Build sorted and prefixed resource dispatcher defs."
  [prefix resource-dispatchers]
  (loop [defs resource-dispatchers file-defs [] folder-defs []]
    (let [[key val] (first defs)]
      (cond (empty? key) (into {} `(~@(sort file-defs) ~@(sort folder-defs)))
            (= (:type val) :file) (recur (next defs) (conj file-defs [key val]) folder-defs)
            (= (:type val) :folder) (recur (next defs) file-defs (conj folder-defs [(str prefix key) val]))
            :else (throw (Exception. (str "Unknown resource type: " (:type val))))))))

(defmacro def-tbw [site-name [& {:keys [uri-prefix]}]
                   & {:keys [resource-dispatchers html-page-defs
                             home-page-uri template common-template-var-fn]}]
  {:pre [resource-dispatchers html-page-defs
         home-page-uri template ;;common-template-var-fn
         ]}
  (let [resource-dispatchers (canonicalize-resource-dispatchers uri-prefix resource-dispatchers)]
    `(do
       (update-tbw-sites! (make-tbw-site :uri-prefix ~uri-prefix
                                         :home-page-uri ~home-page-uri
                                         :site-dispatchers (make-site-dispatchers ~resource-dispatchers ~html-page-defs ~home-page-uri)
                                         :common-template-var-fn ~common-template-var-fn)))))

(defn- default-handler []
  ;; FIXME: logging
  {:status http-bad-request
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body "<html><head><title>tbw</title></head><body><h2>tbw Default Page</h2><p>This is the tbw default page. You're most likely seeing it because the server administrator hasn't set up a custom default page yet.</p></body></html>"})

(defn- call-request-handlers [site]
  (loop [[dispatcher & rest] (:site-dispatchers site)]
;;    (println `(:call-request-handlers ~dispatcher))
    (if dispatcher
      (if-let [handler (dispatcher)]
        (if-let [response (ignore-errors (handler))]
          response
          (recur rest))
        (recur rest))
      (default-handler))))

(defn- handle-request [request]
  (binding [*request* request]
    (let [uri (uri*)
          uri-prefix (apply subs uri (take 2 (positions #(= % \/) uri)))
          [site] (take 1 (filter (fn [site-def]
                                 (= (:uri-prefix site-def) uri-prefix))
                               @tbw-sites))]
;;      (println `(:site ~site :empty? ~(empty? site) :uri-prefix ,uri-prefix))
      (if (empty? site)
        ;; call default handler
        (default-handler)
        (call-request-handlers site)))))

;; FIXME - temporary setup
;; (defn- handle-request [request]
;;   (binding [*request* request]
;;     (println `(server-name ~(server-name*) uri ~(uri*)))
;;     {:Status  200
;;      :headers {"Content-Type" "text/plain"}
;;      :body    "FIXME"}))

(def server (run-jetty handle-request {:port 4347, :join? false}))

    ;; (try
    ;;   (Thread/sleep 2000)
    ;;   (let [response (http/get "http://localhost:4347")]
    ;;     (is (= (:status response) 200))
    ;;     (is (.startsWith (get-in response [:headers "content-type"])
    ;;                      "text/plain"))
    ;;     (is (= (:body response) "Hello World")))
    ;;   (finally (.stop server)))))
;; FIXME end

(defn- ex-home-menu-vars []
  )
(defn- ex-services-menu-vars []
  )
(defn- ex-examples-menu-vars []
  )
(defn- ex-about-menu-vars []
  )
(defn- ex-contact-menu-vars []
  )
(defn- ex-contact-menu-vars []
  )

(def-tbw ex-website [:uri-prefix "/abc"]
  :resource-dispatchers {"/css/"        {:type :folder :path "css"}
                         "/img/"        {:type :folder :path "img"}
                         "/js/"         {:type :folder :path "js"}
                         "/yui/"        {:type :folder :path "yui"}
                         "/favicon.ico" {:type :file   :path "img/favicon.ico"}
                         "/robots.txt"  {:type :file   :path "etc/robots.txt"}
                         }
  :html-page-defs {"/home.html"         ex-home-menu-vars
		   "/services.html"	ex-services-menu-vars
		   "/examples.html"	ex-examples-menu-vars
		   "/about.html"	ex-about-menu-vars
		   "/contact.html"	ex-contact-menu-vars
		   "/thanks.html"       ex-contact-menu-vars}
  :home-page-uri "/home.html"
  :template { ;;:folder "DEFSTRUCT:HTML;" ;; if not given, use CWD
	     :top-template "defstruct-template.html"
             :content-marker "<!-- TMPL_VAR content -->"
             :template-var-fn ex-template-vars})


;;; CORE.CLJ ends here
