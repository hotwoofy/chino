(ns chino.test.core
  (:require [clojure.test :refer :all]
            [chino.core :as js])
  (:import [org.mozilla.javascript Context ScriptableObject NativeObject NativeArray Function]))

(deftest wraps-javascript-types?
  (testing "converts numbers to doubles"
    (is (= 2.0 (js/eval "1 + 1"))))
  (testing "returns booleans"
    (is (true? (js/eval "2 + 2 == 4")))
    (is (false? (js/eval "2 + 2 == 5"))))
  (testing "returns strings"
    (is (= "Hello, World!" (js/eval "'Hello, World!'"))))
  (testing "converts objects to maps"
    (is (= { :message "Hello, World!" } (js/eval "o = { message: 'Hello, World!'}"))))
  (testing "converts arrays to vectors"
    (is (= [1.0, 2.0, 3.0] (js/eval "[1, 2, 3]"))))
  (testing "converts functions to fns"
    (is (fn? (js/eval "f = function() {}"))))
  (testing "creates fns that can be executed"
    (is (= "Hello, World!" ((js/eval "f = function(greeting) { return greeting + ', World!'; }") "Hello"))))
  (testing "returns null"
    (is (nil? (js/eval "null"))))
  (testing "treats undefined as null"
    (is (nil? (js/eval "undefined"))))
  (testing "converts complex objects"
    (is (= { :array [1.0, 2.0, 3.0] :message "Hello, World" :object { :nothing nil :thing 1.0 }},
           (js/eval "o = { message: 'Hello, World', array: [1, 2, 3], object: { nothing: null, thing: 1 }}")))))

(deftest wraps-clojure-types?
  (testing "can pass in numbers"
    (is (= 4.0 (js/eval { :a 2 :b 2 } "a + b"))))
  (testing "can pass in strings"
    (is (= "Hello" (js/eval { :a "Hello"} "a"))))
  (testing "can pass in bools"
    (is (true? (js/eval { :a true } "a")))
    (is (false? (js/eval { :a false } "a"))))
  (testing "can pass in maps"
    (is (= {:b 1.0 :c 2.0 } (js/eval { :a { :b 1 :c 2 }} "a"))))
  (testing "can pass in collections"
    (is (= [1.0 2.0 3.0] (js/eval { :a [1 2 3] } "a")))
    (is (= [1.0 2.0 3.0] (js/eval { :a #{1 2 3} } "a")))
    (is (= [1.0 2.0 3.0] (js/eval { :a '(1 2 3) } "a"))))
  (testing "can pass in functions"
    (is (= "Hello, World" (js/eval { :greet (fn [name] (str "Hello, " name)) } "greet('World')")))))