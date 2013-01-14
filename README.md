# chino

A Clojure wrapper for the Mozilla Rhino JavaScript engine.

## Usage

```clojure
user> (use 'chino.core)
user> (eval "2+2")
4.0
user> (eval "o = {a: 'Hello', b: 'World!'}")
{:b "World!", :a "Hello"}
user> ((eval "f = function greet(who) { return 'Hello, ' + who }") "World")
"Hello, World"
user> (eval {:a 1 :b 2} "a + b")
3.0
user> (eval {:a 1 :b 2 :add (fn [a b] (+ a b))} "add(a, b)")
3.0
user> (eval {:person {:name "Thom" :greet #(str "Hello, " (:name *this*))}} "person.greet()")
"Hello, Thom"
```

Mostly just [see the tests](https://github.com/hotwoofy/chino/blob/master/test/chino/test/core.clj) though.

## License

Copyright © 2013 Thom Lawrence

Distributed under the Eclipse Public License, the same as Clojure.
