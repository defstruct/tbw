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

  ;;(:import [java.io :only []]))
  )

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

;; Global mapping used in top level dispatcher

(def tbw-sites (ref []))

(defrecord TemplateBasedWebSite [script->html-template uri-prefix home-page-uri resource-dispatchers common-template-var-fn])

(defn- make-tbw-site [& {:keys [site-dispatcher uri-prefix site-home home-page-uri resource-dispatchers common-template-var-fn]
                         :or {uri-prefix "/"}}]
  {:pre [(identity home-page-uri) (identity resource-dispatchers) (and (= (get uri-prefix 0) \/)
                                                                       (not (= (get uri-prefix (dec (count uri-prefix)))
                                                                               \/)))]}
  (let [new-site (TemplateBasedWebSite. {} ;; script->html-template
                                        uri-prefix
                                        home-page-uri
                                        resource-dispatchers
                                        common-template-var-fn)]


    new-site))

;; FIXME: move unless macro to proper place
(defmacro unless [pred & body]
  `(when ~pred
     ~@body))

(defn- update-tbw-sites! [site-obj]
  (let [new-uri-prefix (:uri-prefix site-obj)
        existing-pos (take 1 (positions #(= (:uri-prefix %) new-uri-prefix) @tbw-sites))]
    (if (integer? existing-pos)
      (do
        ;; FIXME: warn!
        (dosync (alter tbw-sites assoc existing-pos site-obj)))
      (dosync
       (alter tbw-sites conj site-obj)))))

(defmacro def-tbw [site-name [& {:keys [uri-prefix]}]
                   & {:keys [site-home resource-dispatchers html-page-defs
                             home-page-uri template common-template-var-fn]}]
  {:pre [site-home resource-dispatchers html-page-defs
         home-page-uri template ;;common-template-var-fn
         ]}
  `(do
     (update-tbw-sites! (make-tbw-site :site-dispatcher (fn []
                                                          ;; return html page based on "url-prefix + script-name"
                                                          "FIXME")
                                       :uri-prefix ~uri-prefix
                                       :site-home ~site-home
                                       :home-page-uri ~home-page-uri
                                       :resource-dispatchers ~resource-dispatchers
                                       :common-template-var-fn ~common-template-var-fn))))




(defn- top-level-dispatcher [request]
  (binding [*request* request]
;;    (print *request*)
    (let [uri (uri*)
          uri-prefix (apply subs uri (take 2 (positions #(= % \/) uri)))
          site (take 1 (filter (fn [site-def]
                                 (= (:uri-prefix site-def) uri-prefix))
                               @tbw-sites))]
      (println `(~uri-prefix ~site))
      (if (empty? site)
        {:Status  400
         :headers {"Content-Type" "text/plain"}
         :body    "Oops"}
        {:Status  200
         :headers {"Content-Type" "text/plain"}
         :body    "FIXME"}))))

;; FIXME - temporary setup
;; (defn- top-level-dispatcher [request]
;;   (binding [*request* request]
;;     (println `(server-name ~(server-name*) uri ~(uri*)))
;;     {:Status  200
;;      :headers {"Content-Type" "text/plain"}
;;      :body    "FIXME"}))

(def server (run-jetty top-level-dispatcher {:port 4347, :join? false}))

    ;; (try
    ;;   (Thread/sleep 2000)
    ;;   (let [response (http/get "http://localhost:4347")]
    ;;     (is (= (:status response) 200))
    ;;     (is (.startsWith (get-in response [:headers "content-type"])
    ;;                      "text/plain"))
    ;;     (is (= (:body response) "Hello World")))
    ;;   (finally (.stop server)))))
;; FIXME end

(def-tbw ex-website [:uri-prefix "/abc"]
  :site-home "~/Works/defstruct/"
  ;; :site-home "~/Works/defstruct/" ;; if not given, use current working dir
  :resource-dispatchers {"/css/"        {:folder "css/"}
                         "/img/"	{:folder "img/"}
                         "/js/"		{:folder "js/"}
                         "/yui/"	{:folder "yui/"}
                         "/favicon.ico"	{:file   "img/favicon.ico"}
                         "/robots.txt"	{:file	 "etc/robots.txt"}
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
