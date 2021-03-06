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

(ns tbw.test.template
  (:require [tbw.template] :reload)
  (:use [clojure.test]
        [tbw.template]))

(deftest tmpl-var-printer
  (let [p (create-tmpl-evaluator "<p class='fancy'><!-- TMPL_VAR text --></p>")]
    (testing "TMPL_VAR printer with empty env"
      (is (= (p {}) "<p class='fancy'></p>")))
    (testing "TMPL_VAR printer with matching env"
      (assert (= (p {:text "Foo"}) "<p class='fancy'>Foo</p>")))))

(deftest tmpl-if-printer
  (let [p (create-tmpl-evaluator "The <!-- TMPL_IF fast -->quick <!-- /TMPL_IF -->brown fox")]
    (testing "TMPL_IF printer with empty env"
      (is (= (p {}) "The brown fox")))
    (testing "TMPL_IF printer with true then branch"
      (is (= (p {:fast true}) "The quick brown fox")))
    (testing "TMPL_IF printer with false then branch"
      (is (= (p {:fast false}) "The brown fox")))))

(deftest tmpl-if-printer+tmpl-var
  (let [p (create-tmpl-evaluator "The <!-- TMPL_IF fast --><!--TMPL_VAR speed--><!-- /TMPL_IF -->brown fox")]
    (testing "TMPL_IF+TMPL_VAR printer with empty env"
      (is (= (p {}) "The brown fox")))
    (testing "TMPL_IF+TMPL_VAR printer with true then branch without TMPL_VAR"
      (is (= (p {:fast true}) "The brown fox")))
    (testing "TMPL_IF+TMPL_VAR printer with false then branch without TMPL_VAR"
      (is (= (p {:fast false}) "The brown fox")))
    (testing "TMPL_IF+TMPL_VAR printer with true then branch with TMPL_VAR"
      (is (= (p {:fast true :speed "mach 10 "}) "The mach 10 brown fox")))
    (testing "TMPL_IF+TMPL_VAR printer with false then branch with TMPL_VAR"
      (is (= (p {:fast false :speed "mach 10 "}) "The brown fox")))))

(deftest tmpl-if-printer+else
  (let [p (create-tmpl-evaluator "The <!-- TMPL_IF fast -->quick<!-- TMPL_ELSE -->slow<!-- /TMPL_IF --> brown fox")]
    (testing "TMPL_IF+TMPL_ELSE printer with empty env"
        (is (= (p {}) "The slow brown fox")))
    (testing "TMPL_IF+TMPL_ELSE printer with else branch"
        (is (= (p {:fast false}) "The slow brown fox")))
    (testing "TMPL_IF+TMPL_ELSE printer with then branch"
        (is (= (p {:fast true}) "The quick brown fox")))))

(deftest tmpl-unless+else
  (let [p (create-tmpl-evaluator "The <!-- TMPL_UNLESS slow -->quick<!-- TMPL_ELSE -->slow<!-- /TMPL_UNLESS --> brown fox")]
    (testing "TMPL_UNLESS+TMPL_ELSE printer with empty env"
      (is (= (p {}) "The quick brown fox")))
    (testing "TMPL_UNLESS+TMPL_ELSE printer with then branch"
      (is (= (p {:slow false}) "The quick brown fox")))
    (testing "TMPL_UNLESS+TMPL_ELSE printer with else branch"
      (is (= (p {:slow true}) "The slow brown fox")))))

(deftest tmpl-unless
  (let [p (create-tmpl-evaluator "The <!-- TMPL_UNLESS slow -->quick<!-- /TMPL_UNLESS --> brown fox")]
    (testing "TMPL_UNLESS printer with empty env"
      (is (= (p {}) "The quick brown fox")))
    (testing "TMPL_UNLESS printer with false var"
      (is (= (p {:slow false}) "The quick brown fox")))
    (testing "TMPL_UNLESS printer with true var"
      (is (= (p {:slow true}) "The  brown fox")))))

(deftest tmpl-loop
  (let [p (create-tmpl-evaluator "<!-- TMPL_LOOP foo -->[<!-- TMPL_VAR bar -->,<!-- TMPL_VAR baz -->]<!-- /TMPL_LOOP -->")]
    (testing "TMPL_LOOP printer with empty env"
      (is (= (p {}) "")))
    (testing "TMPL_LOOP printer with missing inner var"
      (is (= (p {:foo [{:bar 0 :barz 1} {:bar 2 :barz 3}]}) "[0,][2,]")))
    (testing "TMPL_LOOP printer with local inner var"
      (is (= (p {:foo [{:bar 0 :baz 1} {:bar 2 :baz 3}]}) "[0,1][2,3]")))
    (testing "TMPL_LOOP printer with global inner var"
      (is (= (p {:baz 3 :foo [{:bar 0} {:bar 2}]}) "[0,3][2,3]")))))

(deftest tmpl-loop+tmpl-var
  (let [p (create-tmpl-evaluator "<!-- TMPL_LOOP foo -->[<!-- TMPL_VAR bar -->,<!-- TMPL_VAR baz -->]<!-- /TMPL_LOOP -->")]
    (testing "TMPL_LOOP+TMPL_VAR printer with empty env"
      (is (= (p {}) "")))
    (testing "TMPL_LOOP+TMPL_VAR printer with missing inner var"
      (is (= (p {:foo [{:bar 0 :barz 1} {:bar 2 :barz 3}]}) "[0,][2,]")))
    (testing "TMPL_LOOP+TMPL_VAR printer with local inner var"
      (is (= (p {:foo [{:bar 0 :baz 1} {:bar 2 :baz 3}]}) "[0,1][2,3]")))
    (testing "TMPL_LOOP+TMPL_VAR printer with global inner var"
      (is (= (p {:baz 3 :foo [{:bar 0} {:bar 2}]}) "[0,3][2,3]")))))

(deftest tmpl-repeat
  (let [p (create-tmpl-evaluator "The <!-- TMPL_REPEAT three -->very <!-- /TMPL_REPEAT -->fast brown fox")]
    (testing "TMPL_REPEAT printer with empty env"
      (is (= (p {}) "The fast brown fox")))
    (testing "TMPL_REPEAT printer with proper number var"
      (is (= (p {:three 3}) "The very very very fast brown fox")))
    (testing "TMPL_REPEAT printer with proper invalid var"
      (is (= (p {:three "3"}) "The fast brown fox")))))

(deftest tmpl-include
  (let [p (create-tmpl-evaluator "Fox<!-- TMPL_include examples/html/main.html --> jumps over the lazy dog")]
    (testing "TMPL_INCLUDE with example HTML file"
      (is (= (p {}) "Fox<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>\n<html>\n  <head>\n    <meta http-equiv='Content-Type' content='text/html; charset=utf-8' />\n    <link rel=\"stylesheet\" type=\"text/css\" href=\"/adder/css/adder.css\">\n    <title>adder</title>\n  </head>\n\n  <body>\n    \n    \n  </body>\n</html>\n jumps over the lazy dog"))
      (is (= (p {:input  true :show-invalid-message true :a 111 :b 222 })
             "Fox<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>\n<html>\n  <head>\n    <meta http-equiv='Content-Type' content='text/html; charset=utf-8' />\n    <link rel=\"stylesheet\" type=\"text/css\" href=\"/adder/css/adder.css\">\n    <title>adder</title>\n  </head>\n\n  <body>\n    \n    <h2>add two numbers</h2>\n    <form method='post'>\n      \n      <p>Those are not both numbers!</p>\n      \n      <input type='text' name='a' value='111'></input>\n      <span> + </span>\n      <input type='text' name='b' value='222'></input>\n      <br>\n      <input type='submit' value='add'></input>\n    </form>\n    \n    \n  </body>\n</html>\n jumps over the lazy dog")))))

(deftest tmpl-call
  (let [p (create-tmpl-evaluator "Fox<!-- TMPL_CALL parts --> jumps over the lazy dog")]
    (testing "TMPL-CALL without any parts var"
      (is (= (p {}) "Fox jumps over the lazy dog")))
    (testing "TMPL-CALL with example HTML file 1"
      (is (= (p {:parts [["examples/html/main.html" {}]]})
             "Fox<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>\n<html>\n  <head>\n    <meta http-equiv='Content-Type' content='text/html; charset=utf-8' />\n    <link rel=\"stylesheet\" type=\"text/css\" href=\"/adder/css/adder.css\">\n    <title>adder</title>\n  </head>\n\n  <body>\n    \n    \n  </body>\n</html>\n jumps over the lazy dog")))
    (testing "TMPL-CALL with example HTML file 2"
      (is (= (p {:parts [["examples/html/main.html" {:input  true :show-invalid-message true :a 111 :b 222 }]]})
             "Fox<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>\n<html>\n  <head>\n    <meta http-equiv='Content-Type' content='text/html; charset=utf-8' />\n    <link rel=\"stylesheet\" type=\"text/css\" href=\"/adder/css/adder.css\">\n    <title>adder</title>\n  </head>\n\n  <body>\n    \n    <h2>add two numbers</h2>\n    <form method='post'>\n      \n      <p>Those are not both numbers!</p>\n      \n      <input type='text' name='a' value='111'></input>\n      <span> + </span>\n      <input type='text' name='b' value='222'></input>\n      <br>\n      <input type='submit' value='add'></input>\n    </form>\n    \n    \n  </body>\n</html>\n jumps over the lazy dog")))))

(deftest nested-tmpl
  (let [p (create-tmpl-evaluator "<!-- TMPL_IF input --> TMPL_IF input start <!-- TMPL_UNLESS valid-numbers --> TMPL_UNLESS valid-numbers start <!-- /TMPL_UNLESS --> TMPL_UNLESS valid-numbers end <!-- TMPL_IF a -->TMPL_IF a start '<!-- TMPL_VAR a -->' 'TMPL_VAR a' <!-- /TMPL_IF --> TMPL_IF a end <!-- TMPL_IF b --> TMPL_IF b start '<!-- TMPL_VAR b -->' 'TMPL_VAR b' <!-- /TMPL_IF --> TMPL_IF b end <!-- /TMPL_IF --> TMPL_IF input end <!-- TMPL_IF output --> TMPL_IF output start<!-- TMPL_VAR a --> + <!-- TMPL_VAR b --> = <!-- TMPL_VAR sum --> a + b = sum <!-- /TMPL_IF --> TMPL_IF output end")]
    (testing "TMPL-IF with nested TMPL_VAR"
      (is (= (p {}) " TMPL_IF input end  TMPL_IF output end"))
      (is (= (p {:input true}) " TMPL_IF input start  TMPL_UNLESS valid-numbers start  TMPL_UNLESS valid-numbers end  TMPL_IF a end  TMPL_IF b end  TMPL_IF input end  TMPL_IF output end"))
      (is (= (p {:input true :valid-numbers true}) " TMPL_IF input start  TMPL_UNLESS valid-numbers end  TMPL_IF a end  TMPL_IF b end  TMPL_IF input end  TMPL_IF output end"))
      (is (= (p {:input true :valid-numbers false :a 1 :b 2})
             " TMPL_IF input start  TMPL_UNLESS valid-numbers start  TMPL_UNLESS valid-numbers end TMPL_IF a start '1' 'TMPL_VAR a'  TMPL_IF a end  TMPL_IF b start '2' 'TMPL_VAR b'  TMPL_IF b end  TMPL_IF input end  TMPL_IF output end"))
      (is (= (p {:input true :valid-numbers true :a 1 :b 2})
             " TMPL_IF input start  TMPL_UNLESS valid-numbers end TMPL_IF a start '1' 'TMPL_VAR a'  TMPL_IF a end  TMPL_IF b start '2' 'TMPL_VAR b'  TMPL_IF b end  TMPL_IF input end  TMPL_IF output end"))

      (is (= (p {:output true}) " TMPL_IF input end  TMPL_IF output start +  =  a + b = sum  TMPL_IF output end"))
      (is (= (p {:output true :a 1 :b 2 :sum 3}) " TMPL_IF input end  TMPL_IF output start1 + 2 = 3 a + b = sum  TMPL_IF output end")))))

;;; TEMPLATE.CLJ ends here
