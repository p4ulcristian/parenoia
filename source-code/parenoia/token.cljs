(ns parenoia.token
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as znode]
            [parenoia.style :as style]
            [parenoia.overlays :as overlays]
            ["react" :as react]
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


(defn references-button [color open? set-open?]
 [:div {:on-mouse-enter #(set-open? true)
        :on-mouse-leave #(set-open? false)
        :style {:display :flex 
                :justify-content :center 
                :align-items :center 
                :height 30 
                :width 30 
                :border "1px solid white"
                :border-radius "50%"
                :background "#333"
                :color color
                :transform (if open? "scale(1)" "scale(0.7)")}}
  [:i {:style {:font-size "14px"}
       :class "fa-solid fa-circle-nodes"}]])

(defn references-and-definition []
 (let [the-references     (subscribe [:db/get [:parenoia :references]])
       the-definition (subscribe [:db/get [:parenoia :definition]])]
  [:div 
    {:style {:background :lightblue 
             :border-radius "10px"
             :height "fit-content"
             :padding "10px"}}
            
    "References"
    (str @the-references)]))
       


(defn references-overlay [ref]
  (let [[open? set-open?] (react/useState false)
        first-lint-color "lightblue"]
    [overlays/overlay-wrapper
     ref
     [:div {:style {:z-index (if open? 10000 5000)
                    :pointer-events :auto
                    :height (if open? "fit-content" "0")
                    :width (if open? "fit-content" "0")
                    :transform "translate(calc(100% - 15px), -15px)"
                    :border-radius (if open? "10px" "50%")
                    :position :absolute 
                    :right 0 
                    :top 0
                    
                    :color (if open? "black" first-lint-color)}}
      (if-not open?
        [references-button first-lint-color open?  set-open?]
        [:div {:style {:display :grid 
                       :grid-template-columns "auto auto"
                        :gap "3px"}}
                        
         [references-button first-lint-color open? set-open?]
         [references-and-definition]])]]))


(defn token-inner [zloc selected? unused-binding?]
  (let [ref (react/useRef)]
   [:div {:ref ref
          :style {:box-shadow style/box-shadow
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
                                                   :else "none"))
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
    (when selected? [references-overlay ref])
    [:div
     ;(when selected? [go-to-definition-button])
     (if (= nil (z/tag zloc))
       [:br]
       (z/string zloc))]]))

(defn view [zloc selected?]
  (let [unused-binding?   (subscribe [:parenoia/unused-binding? zloc])]
    [token-inner zloc selected? @unused-binding?]))