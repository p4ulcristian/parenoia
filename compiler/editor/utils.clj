(ns editor.utils 
  (:require [clojure.edn :as edn]
            [cljfmt.core :as fmt]))


(defn write-file [file-name content]
  (spit file-name (fmt/reformat-string content 
                    {:split-keypairs-over-multiple-lines? true
                     :sort-ns-references? true
                     :indents {#".*" [[:inner 0]]}})))

(defn read-file [file-name]
  (slurp file-name))

(def column-offset 2)