(ns chino.core
  (:refer-clojure :exclude [eval])
  (:import [org.mozilla.javascript Scriptable Context NativeObject NativeArray Function BaseFunction]))

(declare convert)

(defn- convert-function [context scope function this]
  (fn [& args]
    (convert context scope (.call function context scope this (to-array args)) this)))

(defn- convert-array [context scope array]
  (reduce #(assoc %1 (int %2) (convert context scope (.get array (int %2) array))) [] (.getIds array)))

(defn- convert-object [context scope obj]
  (reduce #(assoc %1 (keyword %2) (convert context scope (.get obj %2 obj) obj)) {} (.getIds obj)))

(defn- convert [context scope obj & this]
  (cond
   (nil? obj) obj
   (= (Context/getUndefinedValue) obj) nil
   (= Scriptable/NOT_FOUND obj) nil
   (instance? Number obj) (double obj)
   (instance? String obj) obj
   (instance? Boolean obj) (boolean obj)
   (instance? Function obj) (convert-function context scope obj this)
   (instance? NativeArray obj) (convert-array context scope obj)
   (instance? NativeObject obj) (convert-object context scope obj)
   :else (throw (ex-info "Unknown JavaScript object" obj))))

(declare wrap)

(defn- wrap-function [func]
  (proxy [BaseFunction] []
    (call [context scope this args]
      (apply func (map wrap args)))))

(defn- wrap-map [scope map]
  (doseq [[key value] map]
    (.put scope (wrap key) scope (wrap value)))
  scope)

(defn- wrap [value]
  (cond
   (nil? value) nil
   (number? value) (double value)
   (string? value) value
   (keyword? value) (name value)
   (instance? Boolean value) value
   (map? value) (wrap-map (NativeObject.) value)
   (coll? value) (NativeArray. (to-array (map wrap value)))
   (fn? value) (wrap-function value)))

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
                 (let [scope (wrap-map (.initStandardObjects context nil true) scope)]
                   (convert context scope (.evaluateString context scope src, "<eval>", 0, nil))))))
