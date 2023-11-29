(ns parenoia.textarea
  (:require ["parinfer" :as parinfer]
            ["react" :as react]
            [reagent.core :refer [atom]]
            [parenoia.keyboard :as keyboard]
            [re-frame.core :refer [dispatch subscribe]]
            [rewrite-clj.parser :as zparser]
            [rewrite-clj.zip :as z]))

(def autofocus-input-value (atom nil))

(defn autofocus-input--unmount [current-ref zloc]
  (fn []
    (let [og-string (z/string zloc)
          edited-string (try (zparser/parse-string
                               (.-text (parinfer/smartMode @autofocus-input-value)))
                          (catch js/Error e "veryspecific-&error"))
          error? (= edited-string "veryspecific-&error")
          same?  (= og-string @autofocus-input-value)
          edited-zloc (z/edit zloc (fn [e]  edited-string))]

      (when-not (or same? error?)
        (do
          (keyboard/modify-file
            (z/of-node (z/root edited-zloc)
              {:track-position? true}))
          (keyboard/set-zloc edited-zloc)
          (keyboard/remove-listener current-ref keyboard/block-some-keyboard-events))))))

(defn autofocus-input--effect [ref zloc]
  (react/useEffect
    (fn []
      (let [current-ref (.-current ref)]
        (do
          (reset! autofocus-input-value (z/string zloc))
          (keyboard/add-listener current-ref keyboard/block-some-keyboard-events)
          (.setTimeout js/window #(.select current-ref) 50))
        (autofocus-input--unmount current-ref zloc)))

    #js []))

(defn autofocus-input-wrapper [content]
  [:div
   {:style {:position :absolute
            :top 0
            :left "50%"
            :min-width "100%"
            :height "100%"
            :min-height "200px"
            :transform "translateX(-50%)"
            :color "#333"}}
   content])

(defn view [zloc]
  (let [ref (react/useRef)
        og-string  (z/string zloc)
        on-change (fn [^js event] (let [value (-> event .-target .-value)]
                                    (reset! autofocus-input-value value)))]

    (autofocus-input--effect ref zloc)
    [autofocus-input-wrapper
     [:<>
      [:textarea {:style {:position :absolute
                          :background "white"
                          :border-radius 10
                          :left 0
                          :top 0
                           ;:transform "translateY(-100%)"
                          :min-width 100
                          :box-sizing "border-box"
                          :height "100%"
                          :width "100%"

                          :padding "5px"
                          :z-index 1000}
                   :ref ref
                  :value @autofocus-input-value
                    ;:autofocus true
                   :on-click #(.stopPropagation %)

                  :on-change  on-change}]]]))