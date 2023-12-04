(ns editor.utils 
  (:require [clojure.edn :as edn]
            [cljfmt.core :as fmt]))


(defn write-file [file-name content]
  (spit file-name content))

(defn read-file [file-name]
  (slurp file-name))

(def column-offset 2)