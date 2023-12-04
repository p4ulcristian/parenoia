(ns parenoia.utils
  (:require [clojure.string :as clojure.string]))

(defn uri->path [uri]
  (apply str (drop 7 uri)))

(defn file-uri? [uri]
  (clojure.string/starts-with? uri "file"))