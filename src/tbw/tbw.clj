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

(ns tbw.tbw
  (:import [java.io :only []]))

(defrecord TemplateBasedWebSite [script->html-template site-prefix home-page-url site-dispatchers common-template-var-fn])

(def tbw-sites {})

(defn make-tbw-site [& {:keys [site-prefix home-page-url site-dispatchers common-template-var-fn]
                        :or {site-prefix "FIXME"}}]
  {:pre [(identity home-page-url) (identity site-dispatchers)]}
  (let [
  (TemplateBasedWebSite. {} ;; script->html-template
                         site-prefix
                         ))

(defmacro def-tbw [site-name [prefix]
                   & {:keys [site-home site-dispatchers html-page-defs
                             default-page-url template]}]
  `(do
     ;; define dispatcher
     (defn ~site-name []
       ;; return html page based on "site-name + script-name"
       )
     ;; update global record table
     ;; E.g. remove existing one, put new one
  ))

(def-tbw ex-website [] ;; or ("prefix"). For example ("subdomain")
  :site-home "~/Works/defstruct/"
  ;; :site-home "~/Works/defstruct/" ;; if not given, use current working dir
  :site-dispatchers {"/css/"            {:folder "css/"}
		     "/img/"		{:folder "img/"}
		     "/js/"		{:folder "js/"}
		     "/yui/"		{:folder "yui/"}
		     "/favicon.ico"	{:file   "img/favicon.ico"}
                     "/robots.txt"	{:file	 "etc/robots.txt"}
		     }
  :html-page-defs {"/home.html"         ex-home-menu-vars
		   "/services.html"	ex-services-menu-vars
		   "/examples.html"	ex-examples-menu-vars
		   "/about.html"	ex-about-menu-vars
		   "/contact.html"	ex-contact-menu-vars
		   "/thanks.html"       ex-contact-menu-vars}
  :default-page-url "/home.html"
  :template {;;:folder "DEFSTRUCT:HTML;" ;; if not given, use CWD
	     :top-template "defstruct-template.html"
             :content-marker "<!-- TMPL_VAR content -->"
             :template-var-fn ex-template-vars})


;;; TBW.CLJ ends here
