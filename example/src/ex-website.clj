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
