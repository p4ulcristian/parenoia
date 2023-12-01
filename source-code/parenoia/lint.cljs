(ns parenoia.lint
  (:require ["react" :as react]
            ["react-dom" :as react-dom]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent :refer [atom]]
            [rewrite-clj.zip :as z]
            [parenoia.overlays :as overlays]))

(defn lint-background [level]
  (case level
    :warning :orange
    :error :red
    :green))

(defn one-lint [lint]
  [:div {:style {:background (lint-background (:level lint))
                 :padding "5px 10px"
                 :color :black
                 :border "1px solid black"
                 :border-radius "10px"
                 :pointer-events :none
                 :white-space :nowrap}}
   (str (:type lint))])

(defn bug-button [color open? set-open?]
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
  [:i {:style {:font-size "16px"}
       :class "fa-solid fa-radiation"}]])

(defn lint-overlay [ref this-lints]
  (let [[open? set-open?] (react/useState false)
        first-lint-color (lint-background (:level (first this-lints)))]
    [:div
           {:style {:position :absolute
                    :top 0
                    :right 0
                    :transform "translate(100%, -100%)"
                    :background :white}}
     [overlays/overlay-wrapper
      ref
      [:div {:style {:pointer-events :auto
                     :height (if open? "fit-content" "0")
                     :width (if open? "fit-content" "0")
                     :transform "translate(-15px, -15px)"
                     :border-radius (if open? "10px" "50%")
                     :color (if open? "black" first-lint-color)}}
       (if-not open?
         [bug-button first-lint-color open?  set-open?]
         [:div {:style {:display :grid 
                        :grid-template-columns "auto auto"
                         :gap "3px"}}
          [bug-button first-lint-color open? set-open?]
          [:div (map
                  (fn [this-lint] [one-lint this-lint])
                  this-lints)]])]
      open?]]))
     

(defn view [position zloc ref]
  (when-let [lints @(subscribe [:db/get [:parenoia :kondo-lints]])]
    (when-let [[this-row this-col] position]
      (let [this-lints (filter (fn [{:keys [col row]}]
                                 (and
                                   (= col this-col)
                                   (= row this-row)))
                         lints)
            empty-lints? (empty? this-lints)]
        (when-not empty-lints?
          
           [lint-overlay ref this-lints])))))