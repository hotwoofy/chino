(ns chino.core
  (:refer-clojure :exclude [eval])
  (:import [org.mozilla.javascript Scriptable Context NativeObject NativeArray Function BaseFunction]))

(declare from-js)

(defn- from-js-function [context scope function this]
  (fn [& args]
    (from-js context scope (.call function context scope this (to-array args)) this)))

(defn- from-js-array [context scope array]
  (reduce #(assoc %1 (int %2) (from-js context scope (.get array (int %2) array))) [] (.getIds array)))

(defn- from-js-object [context scope obj]
  (reduce #(assoc %1 (keyword %2) (from-js context scope (.get obj %2 obj) obj)) {} (.getIds obj)))

(defn- from-js [context scope obj & this]
  (cond
   (nil? obj) obj
   (= (Context/getUndefinedValue) obj) nil
   (= Scriptable/NOT_FOUND obj) nil
   (instance? Number obj) (double obj)
   (instance? String obj) obj
   (instance? Boolean obj) (boolean obj)
   (instance? Function obj) (from-js-function context scope obj this)
   (instance? NativeArray obj) (from-js-array context scope obj)
   (instance? NativeObject obj) (from-js-object context scope obj)
   :else (throw (ex-info "Unknown JavaScript object" obj))))

(declare to-js)

(defn- to-js-function [func]
  (proxy [BaseFunction] []
    (call [context scope this args]
      (apply func (map to-js args)))))

(defn- to-js-object [scope map]
  (doseq [[key value] map]
    (.put scope (to-js key) scope (to-js value)))
  scope)

(defn- to-js [value]
  (cond
   (nil? value) nil
   (number? value) (double value)
   (string? value) value
   (keyword? value) (name value)
   (instance? Boolean value) value
   (map? value) (to-js-object (NativeObject.) value)
   (coll? value) (NativeArray. (to-array (map to-js value)))
   (fn? value) (to-js-function value)))

(defmacro with-context
  [context-binding & body]
  `(let [~(first context-binding) (Context/enter)]
     (try
       (do ~@body)
       (finally
        (Context/exit)))))

(defn eval
  ([src] (eval {} src))
  ([scope src] (with-context [context]
                 (let [scope (to-js-object (.initStandardObjects context nil true) scope)]
                   (from-js context scope (.evaluateString context scope src, "<eval>", 0, nil))))))
