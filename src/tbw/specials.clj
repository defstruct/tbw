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

(ns tbw.specials)

;; Return-codes - stealing from Hunchentoot
;;

(defmacro def-http-return-codes [map-var & defs]
  `(do
     (def ~map-var ~(apply hash-map (mapcat (fn [[_ code message]]
                                              [code message])
                                            defs)))
     ~@(map (fn [[var code _]]
              `(def ~var ~code))
            defs)))

(def-http-return-codes +http-code->message-map+
  (+http-continue+ 100 "Continue")
  (+http-switching-protocols+ 101 "Switching Protocols")
  (+http-ok+ 200 "OK")
  (+http-created+ 201 "Created")
  (+http-accepted+ 202 "Accepted")
  (+http-non-authoritative-information+ 203 "Non-Authoritative Information")
  (+http-no-content+ 204 "No Content")
  (+http-reset-content+ 205 "Reset Content")
  (+http-partial-content+ 206 "Partial Content")
  (+http-multi-status+ 207 "Multi-Status")
  (+http-multiple-choices+ 300 "Multiple Choices")
  (+http-moved-permanently+ 301 "Moved Permanently")
  (+http-moved-temporarily+ 302 "Moved Temporarily")
  (+http-see-other+ 303 "See Other")
  (+http-not-modified+ 304 "Not Modified")
  (+http-use-proxy+ 305 "Use Proxy")
  (+http-temporary-redirect+ 307 "Temporary Redirect")
  (+http-bad-request+ 400 "Bad Request")
  (+http-authorization-required+ 401 "Authorization Required")
  (+http-payment-required+ 402  "Payment Required")
  (+http-forbidden+ 403 "Forbidden")
  (+http-not-found+ 404 "Not Found")
  (+http-method-not-allowed+ 405 "Method Not Allowed")
  (+http-not-acceptable+ 406 "Not Acceptable")
  (+http-proxy-authentication-required+ 407 "Proxy Authentication Required")
  (+http-request-time-out+ 408 "Request Time-out")
  (+http-conflict+ 409 "Conflict")
  (+http-gone+ 410 "Gone")
  (+http-length-required+ 411 "Length Required")
  (+http-precondition-failed+ 412 "Precondition Failed")
  (+http-request-entity-too-large+ 413 "Request Entity Too Large")
  (+http-request-uri-too-large+ 414 "Request-URI Too Large")
  (+http-unsupported-media-type+ 415 "Unsupported Media Type")
  (+http-requested-range-not-satisfiable+ 416 "Requested range not satisfiable")
  (+http-expectation-failed+ 417 "Expectation Failed")
  (+http-failed-dependency+ 424 "Failed Dependency")
  (+http-internal-server-error+ 500 "Internal Server Error")
  (+http-not-implemented+ 501 "Not Implemented")
  (+http-bad-gateway+ 502 "Bad Gateway")
  (+http-service-unavailable+ 503 "Service Unavailable")
  (+http-gateway-time-out+ 504 "Gateway Time-out")
  (+http-version-not-supported+ 505 "Version not supported"))

;;; SPECIALS.CLJ ends here
