(ns test-a
  (:require [cljs.reader :refer [read-string]]))
  

(defn b []
 (str "b"))

(defn a []
 (let [x 1]
  (str read-string)))
