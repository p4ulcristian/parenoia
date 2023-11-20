(ns test-a
  (:require [cljs.reader :refer [read-string]]))


(defn b [c]
 (read-string b))

(defn a []
 (read-string (b "sda")))
  
