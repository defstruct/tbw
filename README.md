# tbw

Simple Template Based Web for Clojure.

## Comment
This is my first Clojure project. Any suggestion will be welcomed.

## Idea
Define a site using declarative style.
Web pages of the same website usually has the same style and to achieve this just use template html files.
A website is defined by a unique uri prefix.

The template idea from HTML-TEMPLATE written by Edi Weitz.

## Usage

For quick start, please check tbw/examples/adder.clj

### tbw Dictionary

+http-*+
 - HTTP response return codes. See specials.clj

+http-code->message-map+
 - Hash map for HTTP response return code -> message

server-port*, server-name*, remote-addr*, uri*, query-string*, scheme*, request-method*,
content-type*, content-length*, character-encoding*, headers*, body*, :query-params*, form-params*
 - Request readers derived from 'ring' request-map keys. They can accept an optional request-map which is *request* usually.

def-tbw site-name [& {:keys [uri-prefix]}]
                   & {:keys [resource-dispatchers html-folder html-page->env-mappers default-html-page]}
 - Constructor for a tbw site.
   uri-prefix will be used to determine the site handler.
   resource-dispatchers are definitions for static files like CSS files, JS files, Images, etc.
   html-folder is the folder in which all the html files of html-page->env-mappers exist.
   html-page->env-mappers is a pair of HTML file and a hash map returning function.
   default-html-page is the default page to render when there is no script in URI.

set-http-response-headers! header-key header-value
 - Setter for headers of the current HTTP response, *response*

set-http-response-status! http-return-code
 - Setter for HTTP status return code of the current HTTP response, *response*

tbw-handle-request
 - tbw handler. Need to be passed to the underlying webserver.
   For example:
   (defonce server (run-jetty tbw-handle-request {:port 4347, :join? false}))


## Acknowledgements

Thanks to Edi Weitz for many Common Lisp libraries including HTML-TEMPLATE.

## License

Copyright (C) 2011 Jong-won Choi

Distributed under the BSD-style license(http://www.opensource.org/licenses/bsd-license.php)

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

  * Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above
    copyright notice, this list of conditions and the following
    disclaimer in the documentation and/or other materials
    provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR 'AS IS' AND ANY EXPRESSED
OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
