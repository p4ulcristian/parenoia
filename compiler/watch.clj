(ns watch
  (:require
   [editor.api :as editor]
   [shadow.cljs.devtools.server :as server]
   [shadow.cljs.devtools.api    :as shadow]
   [babashka.process :refer [process]]))


(defn webpack-compile []
 (let [stream (-> (process "ls") :out)]
    @(process {:in stream
               :out :inherit} "npx webpack --config-name frontend --mode development")))

(defn watch
  [config]
  (server/stop!)
  (server/start!)
  (webpack-compile)
  (shadow/watch :frontend)
  (editor/start)
  (println "Parenoia backend and frontend started"))

