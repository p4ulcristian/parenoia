(ns parenoia.refactor-ui (:require [re-frame.core :refer [subscribe dispatch]]
                                   [rewrite-clj.zip :as z]))
(defn view [] (let [selected-zloc (deref (subscribe [:db/get [:parenoia :selected-zloc]]))]
                [:div {:style {:position :fixed
                               :top 0
                               :right 0
                               :background :red
                               :width 400
                               :z-index 20
                               :overflow-y :scroll
                               :height "400px"}
                       :on-click (fn [e] (dispatch [:parenoia/refactor! selected-zloc]))}
                 (str (z/string selected-zloc) " - " (z/tag selected-zloc))]))