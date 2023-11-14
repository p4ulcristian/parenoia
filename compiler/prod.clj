(ns prod
  (:require [shadow.cljs.devtools.server :as server]
            [shadow.cljs.devtools.api    :as shadow]
            [babashka.process :refer [process sh shell]])
  (:gen-class))

(defn prod
  [{:keys  [java-config js-builds]}]
  (sh "rm -r prod")
  (sh "mkdir -p prod/resources")
  (sh "cp -r resources/frontend prod/resources")
  (sh "cp -r package.json       prod/package.json")
  (sh "rm -r prod/resources/frontend/js")
  (shadow/release :backend-ready)
  (shadow/release :frontend-ready)
  (let [stream (-> (process "ls") :out)]
    @(process {:in stream
               :out :inherit} "npx webpack --config-name frontend_ready --mode production"))
  ;(sh "docker buildx build --platform linux/amd64,linux/arm64 -t paul931224/wizard:latest --push .")
  )

