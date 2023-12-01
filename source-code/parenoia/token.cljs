(ns parenoia.token
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as znode]
            [parenoia.style :as style]
            [re-frame.core :refer [subscribe dispatch]]))


(defn decide-token-color [zloc]
  (let [token-node (try (z/node zloc) (catch js/Error e nil))]
    (if token-node
      (cond
        (znode/keyword-node? token-node) (style/color [:keyword? :background-color])
        (znode/symbol-node? token-node)         (style/color [:symbol? :background-color])
        :else (style/color [:string? :background-color])))))

(defn decide-token-text-color [zloc]
  (let [token-node (try (z/node zloc) (catch js/Error e nil))]
    (if token-node
      (cond
        (znode/keyword-node? token-node) (style/color [:keyword? :text-color])
        (znode/symbol-node? token-node)         (style/color [:symbol? :text-color])
        :else (style/color [:string? :text-color])))))


(defn first-in-list? [zloc]
  (let [is-first? (z/leftmost? zloc)
        is-in-list? (z/list? (z/up zloc))]
    (and is-first? is-in-list?)))

(defn go-to-definition-button []
  (let [this-def @(subscribe [:db/get [:parenoia :definition]])]
    (when (:uri this-def)

      [:div {:style {:position :absolute
                     :top 0
                     :right 0
                     :background :none
                     :color :red
                     :transform "translate(50%, -50%)"}
             :on-click (fn [e]
                         (.stopPropagation e)
                         (dispatch [:parenoia/set-selected-file-by-uri
                                    (:uri this-def)
                                    (:row this-def)
                                    (:col this-def)]))}
       [:i {:class "fa-regular fa-circle-up"}]])))

(defn token-inner [zloc selected? unused-binding?]
  [:div {:style {:box-shadow style/box-shadow
                 :border-radius "10px"
                 :padding-left "8px"
                 :padding-right "10px"
                 :padding-top    "5px"
                 :padding-bottom "5px"
                 :position :relative
                 :white-space :nowrap
                 :border-left (str "3px solid " (cond
                                                   ;;  same-as-selected?     "magenta"
                                                  (first-in-list? zloc) "magenta"
                                                  :else "transparent"))
                 :color (cond
                          selected? (style/color [:selection :text-color])
                            ;same-as-selected? (style/color [:same-as-selection :text-color])
                          unused-binding?   (style/color [:unused-binding :text-color])
                          :else (decide-token-text-color zloc))
                 :background (cond
                               selected?         (style/color [:selection :background-color])
                                 ;same-as-selected? (style/color [:same-as-selection :background-color])

                               unused-binding?   (style/color [:unused-binding :background-color])
                               :else (decide-token-color zloc))}}
   [:div
    (when selected? [go-to-definition-button])
    (if (= nil (z/tag zloc))
      [:br]
      (z/string zloc))]])

(defn view [zloc selected?]
  (let [unused-binding?   (subscribe [:parenoia/unused-binding? zloc])]
    [token-inner zloc selected? @unused-binding?]))