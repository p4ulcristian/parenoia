(ns editor.config)


(def project-path (atom "/Users/paulcristian/Projects/zgen/parenoia"))
(defn project-root [] (str "file://" @project-path))