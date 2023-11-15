(ns editor.load-files
  (:require 
   [editor.utils :as utils]
   [rewrite-clj.zip :as z]
   [rewrite-clj.node :as node]))
  


(def default-path "source-code")
(def experimental-path "/Users/paulcristian/projects/zgen/wizard/source-code")

(defn get-all-files []
 (let [directory (clojure.java.io/file default-path)
       dir? #(.isDirectory %)]
   (mapv #(.getPath %)
         (filter (comp not dir?)
                 (tree-seq dir? #(.listFiles %) directory)))))

(defn get-all-clojure-files []
  (vec (filter
        (fn [file] (= "cljs" (last (clojure.string/split file #"\."))))
        (get-all-files))))

(defn project-structure []
  (let [file-names (get-all-clojure-files)]
    (reduce merge
     (mapv
       (fn [file-name]
          {file-name (utils/read-file file-name)})
       file-names))))

(defn save-file [{:keys [path content]}]
  (let []
    (utils/write-file path content)
    (str path " written successfully.")))
  
