(ns editor.refactor
  (:require [rewrite-clj.zip :as z]
            [editor.utils :as utils]
            [clojure-lsp.api :as clojure-lsp]))

(defn get-zloc [{:keys [file-path position]}]
 (let [file-string (utils/read-file file-path)
       zloc        (z/of-string file-string {:track-position? true})]
  (z/find-tag-by-pos zloc position :list)))


(defn symbol->position [symbol]
 (let [first-reference (first (:references (clojure-lsp/references {:from symbol})))
       row (:row first-reference)
       col (:col first-reference)]
    [row col]))

(defn to-lodash-string [ns-name]
 (clojure.string/replace ns-name #"-" "_"))

(defn to-file-path [ns-name]
 (clojure.string/replace ns-name #"\." "/"))

(defn keep-namespace-from-symbol [text]
 (first
   (clojure.string/split text #"\/")))


(defn namespace-to-file [symbol]
  (str "source-code/"
   (to-file-path
    (keep-namespace-from-symbol  
     (to-lodash-string (str symbol))))
   ".cljs")) 
      


(defn move-form [from to]
  (let [from-file-path (namespace-to-file from)
        to-file-path   (namespace-to-file to)
        from-position  (symbol->position from)
        from-zloc      (get-zloc {:file-path from-file-path 
                                  :position from-position})
        new-from-zloc  (z/remove from-zloc)
        to-position     [1 1]
        to-zloc        (get-zloc {:file-path to-file-path 
                                  :position to-position})
        new-to-zloc   (z/insert-newline-right
                         (z/insert-right 
                           (z/rightmost to-zloc)
                           (z/sexpr from-zloc))
                         2)]                                
    (utils/write-file from-file-path (z/root-string new-from-zloc))
    (utils/write-file to-file-path   (z/root-string new-to-zloc))
    
    (str "success")))       
 


