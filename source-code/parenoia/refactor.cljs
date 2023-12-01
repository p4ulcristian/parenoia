(ns parenoia.refactor (:require [re-frame.core :refer [subscribe]]
                                [rewrite-clj.zip :as z]))


(defn find-top-form-recursion [last-zloc zloc]
  (let [up-loc (z/up zloc)]
    (cond
      (nil? up-loc) last-zloc
      :else (recur zloc up-loc))))

(defn find-top-form [zloc]
  (find-top-form-recursion nil zloc))

(defn get-ns [zloc]
  (z/sexpr (z/right (z/down zloc))))

(defn go-through-require-vectors [zloc result]
 (if (z/rightmost? zloc)
  (vec (conj result (z/sexpr zloc)))
  (go-through-require-vectors 
    (z/right zloc)
    (vec (conj result (z/sexpr zloc))))))
 

(defn get-require-vectors [zloc]
 (let [require-form (-> zloc z/down z/right z/right)
       first-require-vec (-> require-form z/down z/right)]
  (go-through-require-vectors first-require-vec [])))

(defn namespace-required? [zloc this-ns]
 (let [require-vectors (get-require-vectors zloc)
       required-namespaces (mapv first require-vectors)]
    (boolean (some (fn [a] (= a this-ns))
              required-namespaces))))
    ;(str required-namespaces " - " this-ns)))

(defn get-requires-by-namespace [zloc] 
 (let [this-ns (get-ns zloc)
       project (deref (subscribe [:db/get [:parenoia :project]]))]      
   (remove (fn [one-ns] (= one-ns this-ns))
    (map (fn [[file-name file-zloc]]
           (get-ns file-zloc)) 
         (filter (fn [[file-name file-zloc]] (namespace-required? file-zloc this-ns))
                project))))) 
                                      
(defn get-all-namespace-parents [zloc]
         (let [project (deref (subscribe [:db/get [:parenoia :project]]))]
             (str project)))


