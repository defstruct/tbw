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
  (:use ring.adapter.jetty)
  ;;(:import [java.io :only []]))
  )

;; Global mapping used in top level dispatcher
(def tbw-sites (ref {}))

(defn- top-level-dispatcher [request]
  )

(defrecord TemplateBasedWebSite [script->html-template site-prefix home-page-url site-dispatchers common-template-var-fn])

(defn- make-tbw-site [site-name & {:keys [site-prefix home-page-url site-dispatchers common-template-var-fn]
                                  :or {site-prefix "FIXME"}}]
  {:pre [(identity home-page-url) (identity site-dispatchers)]}
  (let [new-site (TemplateBasedWebSite. {} ;; script->html-template
                                        site-prefix
                                         home-page-url
                                         site-dispatchers
                                         common-template-var-fn)]


    new-site))

(defn- update-tbw-sites! [site-name site-obj]
  (let [existing-def (get @tbw-sites site-name)]
    (when existing-def
      ;; FIXME: warn!
      )
    (dosync
     (alter tbw-sites assoc site-name site-obj))))


(defmacro def-tbw [site-name [prefix]
                   & {:keys [site-home site-dispatchers html-page-defs
                             default-page-url template]}]
  `(do
     ;; define dispatcher
     (defn ~site-name []
       ;; return html page based on "site-name + script-name"
       )
     (update-tbw-sites! site-name (make-tbw-site :site-prefix prefix
                                                 :site-home site-home
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
