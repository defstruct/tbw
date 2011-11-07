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

(def-tbws test-site ()
  :site-home "~/Works/defstruct/"
  :site-pathnames ("DEFSTRUCT" (("HTML;**;*.*"      ("html" "**"))
				("NAME-BADGE-HTML;**;*.*"      ("name-badge-html" "**"))
				("JS;**;*.*"	    ("js" "**"))
				("YUI;**;*.*"	    ("yui" "**"))
				("CSS;**;*.*"       ("css" "**"))
				("IMG;**;*.*"       ("img" "**"))
				("ETC;**;*.*"       ("etc" "**"))))
  :site-dispatchers (("/css/"		:folder		"DEFSTRUCT:CSS;")
		     ("/img/"		:folder		"DEFSTRUCT:IMG;")
		     ("/js/"		:folder		"DEFSTRUCT:JS;")
		     ("/yui/"		:folder		"DEFSTRUCT:YUI;")
		     ("/favicon.ico"	:file		"DEFSTRUCT:IMG;favicon.ico")
		     ;;("^/process-contact.html$" :regex   process-contact.html)
		     ("/robots.txt"	:file		"DEFSTRUCT:ETC;robots.txt")
		     ;;("^/"		:regex		defstruct-site)
		     )
  :html-page-defs (("/home.html"	defstruct-home-menu-vars)
		   ("/services.html"	defstruct-services-menu-vars)
		   ("/examples.html"	defstruct-examples-menu-vars)
		   ("/about.html"	defstruct-about-menu-vars)
		   ("/contact.html"	defstruct-contact-menu-vars)
		   ("/about-jongwon.html" defstruct-about-menu-vars)
		   ("/thanks.html" defstruct-contact-menu-vars))
    :home-page-url "/home.html"
    :template (:folder "DEFSTRUCT:HTML;"
	       :top-template "defstruct-template.html"
	       :content-marker "<!-- TMPL_VAR content -->"
	       :template-var-fn defstruct-template-vars))