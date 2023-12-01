(ns parenoia.overlays
 (:require ["react" :as react]
           ["react-dom" :as react-dom]
           [reagent.core :as reagent]))


(defn get-element []
 (.getElementById js/document "parenoia-body"))

(defn portal [content]
 (let [element (get-element)]
  [:<>
   (when element
       (react-dom/createPortal
         (reagent/as-element content)
         element))]))


(defn overlay-effect [ref set-x set-y set-width set-height]
 (react/useEffect
      (fn []
        (let [element    (get-element)
              scroll-top (.-scrollTop   element)
              scroll-left (.-scrollLeft element)
              bounding-client-rect (.getBoundingClientRect (.-current ref))
              this-x      (.-x bounding-client-rect)
              this-y      (.-y bounding-client-rect)
              this-height (.-height bounding-client-rect)
              this-width  (.-width bounding-client-rect)]
          (set-x (+ scroll-left this-x))
          (set-y (+ scroll-top this-y))
          (set-height this-height)
          (set-width this-width))
        (fn []))
      #js [(.-current ref)]))



(defn overlay-wrapper [ref content]
  (let [[x set-x] (react/useState 0)
        [y set-y] (react/useState 0)
        [width set-width] (react/useState 0)
        [height set-height] (react/useState 0)]
    (overlay-effect ref set-x set-y set-width set-height)
    [portal
          [:div {:class "overlay-wrapper"
                 :style {:cursor :pointer
                          :position :absolute
                          :top y
                          :left x
                          :background :none
                          :height height
                          :width width}}          
              content]]))
        