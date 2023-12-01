(ns parenoia.lint
  (:require ["react" :as react]
            ["react-dom" :as react-dom]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent :refer [atom]]
            [rewrite-clj.zip :as z]))

(defn overlay-wrapper [ref set-open? content additional-style]
  (let [[x set-x] (react/useState 0)
        [y set-y] (react/useState 0)]

    (react/useEffect
      (fn []
        (let [scroll-top (.-scrollTop (.getElementById js/document "parenoia-body"))
              scroll-left (.-scrollLeft (.getElementById js/document "parenoia-body"))
              this-x (.-x (.getBoundingClientRect (.-current ref)))
              this-y (.-y (.getBoundingClientRect (.-current ref)))]

          (set-x (+ scroll-left this-x))
          (set-y (+ scroll-top this-y)))

        (fn []))
      #js [(.-current ref)])
    (when (.getElementById js/document "parenoia-body")
      (react-dom/createPortal
        (reagent/as-element
          [:div {:class "overlay-wrapper"
                 :on-mouse-enter #(set-open? true)
                 :on-mouse-leave #(set-open? false)
                 :style (merge {:cursor :pointer
                                :position :absolute
                                :top y
                                :left x}

                          additional-style)}
           content])
        (.getElementById js/document "parenoia-body")))))

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
    [overlay-wrapper
     ref
     set-open?
     [:div
      (when open?
        (map
          (fn [this-lint] [one-lint this-lint])
          this-lints))]
     {:border "1px solid black"
      :z-index (if open? 10000 5000)
      :transform "translate(0px, 0px)"
      :height (if open? "auto" "10px")
      :width (if open? "auto" "10px")
      :border-radius (if open? "10px" "50%")
      :background (lint-background (:level (first this-lints)))}]))

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