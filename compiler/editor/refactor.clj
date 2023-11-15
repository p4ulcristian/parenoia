(ns editor.refactor
  (:require [rewrite-clj.zip :as z]
            [editor.utils :as utils]))

(defn get-zloc [{:keys [file-path position]}]
 (let [file-string (utils/read-file file-path)
       zloc        (z/of-string file-string {:track-position? true})]
  (z/find-tag-by-pos zloc position :list)))
  
 


