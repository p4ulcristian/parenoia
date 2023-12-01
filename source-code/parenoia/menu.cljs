(ns parenoia.menu
  (:require
   ["randomcolor" :refer [randomColor]]
   ["react" :as react]
   [parenoia.keyboard :as keyboard]
   [re-frame.core :refer [dispatch subscribe]]))

(defn generate-random-bright-color [] (randomColor #js {:format        "rgb"
                                                        :luminosity "light"}))
(defn triangle-right [color]
  [:div.triangle
   {:style {:width 0
            :height 0
            :border-top "25px solid transparent"
            :border-left (str "30px solid " color)
            :border-bottom "25px solid transparent"
            :position "absolute"
            :top 0
            :right 0
            :transform "translate(100%, 0)"}}])
(defn rectangle [color]
  [:div {:style {:background color
                 :height "100%"
                 :width "100%"}}
   [triangle-right color]])
(defn ns-background [color] [:div
                             {:style {;:border-radius "10px"
                                      :box-sizing :border-box
                                      :color "white"
                                      :height "100%"
                                      :width "100%"
                                      :position :absolute
                                      :top 0
                                      :left 0}}
                             [rectangle color]])
(defn ns-part--name [index part]
  [:div.menu-namespace
   {:style {:color "#333"
            :padding "10px"
            :padding-right "0px"
            :margin-left (if (= index 0) 0 25)
            :z-index 1000
            :position :relative}}
   [:div {:style {:color
                  "#333"
                  :border-radius
                  "10px"
                  :padding
                  "5px 10px"}}
    part]])
(defn ns-part [index part color]
  [:div {:style {:position :relative}}
   [ns-background color]
   [ns-part--name index part]])

(defn get-all-namespace-parts [namespaces]
  (set (reduce concat
         (map
           (fn [namespace] (map-indexed (fn [i a]
                                          [i a])
                             (clojure.string/split namespace #"\.")))
           namespaces))))

(defn generate-colors [namespaces]
  (reduce merge
    (map
      (fn [part] {part (generate-random-bright-color)})
      (get-all-namespace-parts namespaces))))

(defn menu-namespace [this-path this-ns generated-colors]
  (let
    [selected? (subscribe [:parenoia/selected-path? this-path])]
    [:div
     {:on-click (fn [e]
                  (dispatch [:parenoia/go-to! this-path [1 1]]))

      :style {:background (if @selected? :turquoise :none)
              :font-weight "bold"
              :display :flex
              :flex-direction :row-reverse
              :justify-content :flex-end
              :cursor :pointer
              :z-index 1000
              :height "50px"}}
     (reverse
       (map-indexed
         (fn [i a] ^{:key a} [ns-part i a (get generated-colors [i a])])
         (clojure.string/split this-ns #"\.")))]))

(defn namespace-search []
  (let [ref (react/useRef)
        search-term (subscribe [:db/get [:parenoia :menu :search-term]])
        menu? (subscribe [:db/get [:parenoia :menu?]])]
    (react/useEffect
      (fn []
        (let [current-ref (.-current ref)]
          (keyboard/add-listener current-ref keyboard/block-some-keyboard-events)
          (fn []
            (keyboard/remove-listener current-ref keyboard/block-some-keyboard-events))))
      #js [])
    (react/useEffect
      (fn [] (when @menu?
               (.select (.-current ref)))
        (fn []))

      #js [@menu?])
    [:div
     {:style {:display :flex
              :justify-content :center}}
     [:input {:ref ref
              :value @search-term
              :placeholder "namespace"
              :style {:padding "10px"
                      :width "200px"
                      :border-radius "5px"
                      :text-align :center}
              :on-change (fn [e] (dispatch [:db/set [:parenoia :menu :search-term] (-> e .-target .-value)]))}]]))

(defn menu-namespaces-inner [paths-and-namespaces]
  (let [generated-colors (generate-colors (map second paths-and-namespaces))]
    [:div {:style {:display :flex
                   :flex-direction :column
                   :justify-content :flex-start
                   :gap "10px"}}
     [namespace-search]
     (map
       (fn [[this-path this-ns]] ^{:key this-path} [:div [menu-namespace this-path this-ns generated-colors]])
       paths-and-namespaces)]))

(defn menu-namespaces []
  (let [search-term
        (subscribe [:db/get [:parenoia :menu :search-term]])
        paths-and-namespaces (subscribe [:parenoia/filter-project-by-namespaces @search-term])]
    [menu-namespaces-inner @paths-and-namespaces]))

(defn open-project []
  (let [ref (react/useRef)
        [path set-path] (react/useState "/Users/paulcristian/Projects/zgen/wizard")]
    (react/useEffect
      (fn []
        (let [current-ref (.-current ref)]
          (keyboard/add-listener current-ref keyboard/block-some-keyboard-events)
          (fn []
            (keyboard/remove-listener current-ref keyboard/block-some-keyboard-events))))
      #js [])
    [:div
     {:style {:text-align :center
              :padding-bottom "10px"
              :display :flex
              :flex-direction :column
              :justify-content :center
              :align-items :center}}
     [:input {:ref ref
              :style {:text-align :center
                      :padding "5px"
                      :width "100%"
                      :border-radius "5px"}
              :value path
              :on-change (fn [a] (set-path (-> a .-target .-value)))
              :placeholder "Project path"}]
     [:div {:on-click (fn [a] (dispatch [:parenoia/set-project-path! path]))
            :style {:margin "20px"
                    :width "200px"
                    :padding "10px"
                    :border-radius "10px"
                    :background "orange"
                    :cursor "pointer"}}
      "set project path"]]))

(defn menu-inner []
  [:div
   [open-project]
   [menu-namespaces]])

(defn view []
  (react/useEffect
    (fn []
      ;(dispatch [:db/set [:parenoia :menu?] true])
      (dispatch [:db/set [:parenoia :editable?] false])
      (dispatch [:db/set [:parenoia :menu :search-term] ""])
      (fn []))
    #js [])
  (let [menu? @(subscribe [:db/get [:parenoia :menu?]])]
    [:div
     [:div.fade-animation
      {:style {:display (if menu? "block" "none")
               :position :fixed
               :z-index 10000
               :transform "translateX(-50%)"
               :padding "20px"
               :border-radius "10px"
               :border "10px solid black"
               :color "#333"
               :height "80vh"
               :width "80vw"
               :overflow-y "auto"
               :top 100
               :left "50%"
               :background "radial-gradient(circle, rgba(245,201,49,1) 0%, rgba(191,165,76,1) 100%)"}}
      [menu-inner]]]))