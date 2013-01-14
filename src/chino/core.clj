(ns chino.core
  (:refer-clojure :exclude [eval])
  (:import [org.mozilla.javascript Context Scriptable ScriptableObject NativeObject NativeArray Function BaseFunction]))

(set! *warn-on-reflection* true)

(declare from-js)

(defn from-js-function [^Context context ^Scriptable scope ^Function function ^Scriptable this]
  (fn [& args]
    (from-js context scope (.call function context scope this (to-array args)) this)))

(defn from-js-array [^Context context ^Scriptable scope ^NativeArray array]
  (vec (map #(from-js context scope (.get array (int %1) array) array) (.getIds array))))

(defn from-js-object [^Context context ^Scriptable scope ^ScriptableObject obj]
  (reduce #(assoc %1 (keyword %2) (from-js context scope (.get obj (str %2) obj) obj)) {} (.getIds obj)))

(defn from-js [^Context context ^Scriptable scope ^NativeObject obj ^Scriptable this]
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

(defn to-js-function [func]
  (proxy [BaseFunction] []
    (call [context scope this args]
      (apply func (map to-js args)))))

(defn to-js-object
  ([map] (to-js-object (NativeObject.) map))
  ([^ScriptableObject scope map]
     (doseq [[key value] map]
       (.put scope (name key) scope (to-js value)))
     scope))

(defn to-js-array [value]
  (NativeArray. (to-array (map to-js value))))

(defn to-js [value]
  (cond
   (nil? value) nil
   (number? value) (double value)
   (string? value) value
   (keyword? value) (name value)
   (instance? Boolean value) value
   (map? value) (to-js-object value)
   (coll? value) (to-js-array value)
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
                   (from-js context scope (.evaluateString context scope src, "<eval>", 0, nil) nil)))))
