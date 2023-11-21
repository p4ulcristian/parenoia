(ns parenoia.form-conditionals
  (:require [rewrite-clj.zip :as z]))

(defn is-ns? [zloc]
  (let [is-list?    (z/list? zloc)
        first-token (z/string (z/down zloc))
        defn?       (= "ns" first-token)]
    (and is-list? defn?)))

(defn is-defn? [zloc]
  (let [is-list?    (z/list? zloc)
        first-token (z/string (z/down zloc))
        defn?       (= "defn" first-token)]
    (and is-list? defn?)))

(defn is-def? [zloc]
  (let [is-list?    (z/list? zloc)
        first-token (z/string (z/down zloc))
        def?        (= "def" first-token)]
    (and is-list? def?)))

(defn is-let-vector? [zloc]
  (let [is-vector?    (z/vector? zloc)
        is-let?       (= "let" (z/string (z/left zloc)))]

    (and is-vector? is-let?)))

(defn is-loop-vector? [zloc]
  (let [is-vector?    (z/vector? zloc)
        is-let?       (= "loop" (z/string (z/left zloc)))]
    (and is-vector? is-let?)))

(defn is-map? [zloc]
  (let [is-map?     (z/map? zloc)]
    is-map?))

(defn is-vector? [zloc]
  (let [is-vector?     (z/vector? zloc)]
    is-vector?))

(defn is-function? [zloc]
  (let [is-list?     (z/list? zloc)]
    is-list?))