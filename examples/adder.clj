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
;; Example implementation of http://mmcgrana.github.com/2010/07/develop-deploy-clojure-web-applications.html
;;
;;;; Code:

(ns tbw.examples.adder
  (:use [tbw.core :only [def-tbw form-params* tbw-handle-request set-http-response-headers! set-http-response-status!]]
        [tbw.specials]
        [ring.adapter.jetty :only [run-jetty]]))

(defn- get-adder-env []
  (set-http-response-headers! "Cache-Control" "public, max-age=2592000")
  (set-http-response-headers! "Content-Type" "text/html; charset=UTF-8")
  (set-http-response-status! +http-accepted+)

  (let [params (form-params*)
        a (get params "a")
        b (get params "b")]
    (if (and a b)
      (let [num-a (binding [*read-eval* false]
                    (read-string a))
            num-b (binding [*read-eval* false]
                    (read-string b))]
        (if (and (number? num-a) (number? num-b))
          {:output true :a num-a :b num-b :sum (+ num-a num-b)}
          {:input  true :show-invalid-message true :a a :b b }))
      {:input true})))


(def-tbw adder [:uri-prefix "/adder"]
  ;; FIXME: resource definitions may need extra response headers
  :resource-dispatchers {"/css/"        {:type :folder :path "examples/css"}
                         ;;"/img/"        {:type :folder :path "img"}
                         ;;"/js/"         {:type :folder :path "js"}
                         ;;"/yui/"        {:type :folder :path "yui"}
                         ;;"/favicon.ico" {:type :file   :path "img/favicon.ico"}
                         ;; robots.txt is only for a file example
                         "/robots.txt"  {:type :file   :path "examples/etc/robots.txt"}
                         }
  :html-folder "examples/html"
  :html-page->env-mappers {"main.html" get-adder-env}
  ;; FIXME: if no mapping found, render default
  ;; (e.g. /adder/)
  :default-html-page    "main.html")

(defonce server (run-jetty tbw-handle-request {:port 4347, :join? false}))

;;(.stop server)
;;(.start server)

;;; ADDER.CLJ ends here
