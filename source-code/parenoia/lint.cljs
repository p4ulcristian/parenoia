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
                 :padding "5px"
                 :color :black
                 :opacity 0.3
                 :pointer-events :none}}
   (str (:type lint))])

(defn info-circle [ref this-lints zloc]
  (let [[open? set-open?] (react/useState false)]
    [overlays/overlay-wrapper
     ref
     [:div {:style {:border "1px solid black"
                    :z-index (if open? 10000 5000)
                    :transform "translate(0px, 0px)"
                    :height (if open? "auto" "10px")
                    :width (if open? "auto" "10px")
                    :border-radius (if open? "10px" "50%")
                    :background (lint-background (:level (first this-lints)))}}
      (when open?
        (map
          (fn [this-lint] [one-lint this-lint])
          this-lints))]]))
     

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
          [:div
           {:style {:position :absolute
                    :top 0
                    :right 0
                    :transform "translate(100%, -100%)"
                    :background :white}}
           [info-circle ref this-lints zloc]])))))