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

(def-tbw ex-website [:uri-prefix "/abc"]
  :resource-dispatchers {"/css/"        {:type :folder :path "css"}
                         "/img/"        {:type :folder :path "img"}
                         "/js/"         {:type :folder :path "js"}
                         "/yui/"        {:type :folder :path "yui"}
                         "/favicon.ico" {:type :file   :path "img/favicon.ico"}
                         "/robots.txt"  {:type :file   :path "etc/robots.txt"}
                         }
  :html-page-dispatchers {"/home.html"          ex-home-menu-vars
                          "/services.html"	ex-services-menu-vars
                          "/examples.html"	ex-examples-menu-vars
                          "/about.html"         ex-about-menu-vars
                          "/contact.html"	ex-contact-menu-vars
                          "/thanks.html"        ex-contact-menu-vars}
  :default-html-page    "/home.html"
  :template { ;;:folder "DEFSTRUCT:HTML;" ;; if not given, use CWD
	     :top-template "defstruct-template.html"
             :content-marker "<!-- TMPL_VAR content -->"
             :template-var-fn ex-template-vars})








;; FIXME: make this experiment a real.
(use 'ring.adapter.jetty)
(use 'ring.middleware.cookies)

(def *counter* (atom 0))

(defn test-app [req]
  (println req)

  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (str "Hello World from Ring" (swap! *counter* inc))})

(defonce server (run-jetty #'test-app {:port 8080 :join? false}))

;;(.stop server)
;;(.start server)



;;; EX-WEBSITE.CLJ ends here
