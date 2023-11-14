(ns parenoia.rewrite 
 (:require [rewrite-clj.zip :as z]
           [rewrite-clj.node :as node]))
 

(defn get-forms-from-file-recursion [zloc forms]
 (if (z/rightmost? zloc)
  (conj forms 
          zloc)
  (get-forms-from-file-recursion 
   (z/right zloc)
   (vec (conj forms 
          zloc))))) 

(defn get-forms-from-file [zloc]
 (get-forms-from-file-recursion zloc []))

(defn zloc-from-file-string [file-string]
  (z/of-string file-string {:track-position? true}))

(defn zloc->string [zloc]
 (z/string zloc))

(defn zloc-coll? [zloc]
 (or 
  (z/map? zloc)
  (z/vector? zloc)
  (z/set? zloc)
  (z/list? zloc)))

(defn get-namespace-from-file [zloc]
  (let [namespace-node  (z/leftmost zloc)
        namespace-name  (z/string (z/right (z/leftmost (z/down namespace-node))))
        namespace-requires (z/right namespace-name)]
    namespace-name)) 

