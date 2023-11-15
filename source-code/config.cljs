(ns config)

;; Frontend 

(def verbose? false)

;; Backend

(def port2 3000)

(defn hello []
  (str port2))

  
(def version "First")
