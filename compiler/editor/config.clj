(ns editor.config)


(def project-path (atom "/Users/paulcristian/Projects/zgen/wizard"))
(defn project-root [] (str "file://" @project-path))