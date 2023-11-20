(ns parenoia.refactor-ui (:require [re-frame.core :refer [subscribe dispatch]]
                                   [rewrite-clj.zip :as z]))





(defn view [] 
 (let [variable-info @(subscribe [:db/get [:parenoia :variable-info]])
       {:keys [from to name]}  variable-info
       selected-zloc (deref (subscribe [:db/get [:parenoia :selected-zloc]]))]
   [:div {:style {:position :fixed
                  :top 0
                  :right 0
                  :background :red
                  :width 400
                  :padding 10
                  :z-index 20
                  :overflow-y :scroll
                  :height "100px"}} 
    [:div "From: " (str from)]
    [:div "To: " (str to)]
    [:div "Name: " (str name)]]))