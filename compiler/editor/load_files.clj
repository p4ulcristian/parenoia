(ns editor.load-files
  (:require
   [cljfmt.core :as fmt]
   [clojure.string :as clojure.string]
   [editor.config :as config]
   [editor.utils :as utils]
   [rewrite-clj.zip :as z]))



(defn get-all-files []
  (let [directory (clojure.java.io/file @config/project-path)
        dir? #(.isDirectory %)]
    (mapv #(.getPath %)
      (filter (comp not dir?)
        (tree-seq dir? #(.listFiles %) directory)))))



(defn get-all-clojure-files []
  (vec (filter
         (fn [file] (or
                      (= "clj" (last (clojure.string/split file #"\.")))
                      (= "cljs" (last (clojure.string/split file #"\.")))
                      (= "cljc" (last (clojure.string/split file #"\.")))))
         (get-all-files))))



(defn project-structure []
  (let [file-names (get-all-clojure-files)]
    (reduce merge
      (mapv
        (fn [file-name]
          {file-name (utils/read-file file-name)})
        file-names))))



(defn get-forms-divided-by-newlines-recursion [zloc]
  (if (z/rightmost? zloc)
    zloc
    (get-forms-divided-by-newlines-recursion
      (z/right (z/edit-node zloc (fn [this-zloc]  (z/insert-newline-right zloc 2)))))))



(defn get-forms-divided-by-newlines [text]
  (z/root-string (get-forms-divided-by-newlines-recursion
                   (z/of-string text))))



(defn clj-format [text]
  (fmt/reformat-string text
    {:split-keypairs-over-multiple-lines? true
     :sort-ns-references? true
     :indents {#".*" [[:inner 0]]}}))



(defn save-file [{:keys [path content]}]
  (let []
    (utils/write-file path (get-forms-divided-by-newlines (clj-format content)))
    (str path " written successfully.")))

