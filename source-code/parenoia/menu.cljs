(ns parenoia.menu
  (:require
   ["react" :as react]
   [parenoia.keyboard :as keyboard]
   [parenoia.refactor :as refactor]
   [re-frame.core :refer [dispatch subscribe]]
   [rewrite-clj.zip :as z]))

(defn ns-part [index part color]
  (let [css-name (str "a" (random-uuid))
        css-id (str "." css-name)]
    [:div {:style {:display :flex
                   :align-items :center}}

     [:div
      {:style {:padding "10px 15px"
              ;:border-radius "10px"
               :padding-left (if (< 0 index)
                               "40px" "10px")
               :margin-left (if (< 0 index)
                              "-80px")
               :background color
               :color "white"}}
      [:div.menu-namespace
       {:style {:background "#333"
                :color "#DDD"
                :padding "10px"
                :border-radius "10px"
                :border "1px solid white"}}
       part]]
     [:style (str css-id ", " css-id ":before, " css-id ":after { width: 90px; height: 90px;}
" css-id " {
	overflow: hidden;
	position: relative;
	border-radius: 20%;
	transform: translateY(25%) translateX(-20%) rotate(0deg) skewY(30deg) scaleX(.866);
	cursor: pointer;
	pointer-events: none;
} 
" css-id ":before, " css-id ":after {
	position: absolute;
	background: " color ";
	pointer-events: auto;
	content: '';
}
" css-id ":before {
	border-radius: 20% 20% 20% 53%;
	transform: scaleX(1.155) skewY(-30deg) rotate(-30deg) translateY(-42.3%) 
			skewX(30deg) scaleY(.866) translateX(-24%);
}
" css-id ":after {
	border-radius: 20% 20% 53% 20%;
	transform: scaleX(1.155) skewY(-30deg) rotate(-30deg) translateY(-42.3%) 
			skewX(-30deg) scaleY(.866) translateX(24%);
}")]
     [:div {:class css-name}]]))

(defn get-all-namespace-parts [namespaces]
  (set (reduce concat
         (map
           (fn [namespace] (map-indexed (fn [i a]
                                          [i a])
                             (clojure.string/split namespace #"\.")))
           namespaces))))

(defn generate-color []
  (str "rgb(" (rand-int 256) ", " (rand-int 256) ", " (rand-int 256)  ")"))

(defn generate-colors [namespaces]
  (reduce merge
    (map
      (fn [part] {part (generate-color)})
      (get-all-namespace-parts namespaces))))

(defn menu-namespace [namespace project-item generated-colors]
  (let [selected? (= (first project-item)
                    @(subscribe [:db/get [:parenoia :selected :file-path]]))]
    [:div
     {:on-click (fn [e]
                  (dispatch [:parenoia/go-to! (first project-item) (z/position (second project-item))]))

      :style {:font-weight "bold"
              :padding "5px"
              :gap "10px"
              :display :flex
              :border-bottom "1px solid black"
              :cursor :pointer
              :z-index 1000
            ;;  :flex-direction :row-reverse
            ;;  :justify-content :flex-end
              :background (if selected? :turquoise :none)}}

     (map-indexed
       (fn [i a] ^{:key a} [ns-part i a (get generated-colors [i a])])
       (clojure.string/split namespace #"\."))]))

(defn namespace-search [search-term set-search-term]
  (let [ref (react/useRef)]
    (react/useEffect
      (fn []
        (let [current-ref (.-current ref)]
          (keyboard/add-listener current-ref keyboard/block-some-keyboard-events)
          (fn []
            (keyboard/remove-listener current-ref keyboard/block-some-keyboard-events))))
      #js [])
    [:div
     {:style {:display :flex
              :justify-content :center}}
     [:input {:ref ref
              :value search-term
              :placeholder "namespace"
              :style {:padding "10px"
                      :width "200px"
                      :border-radius "5px"
                      :text-align :center}
              :on-change (fn [e] (set-search-term (-> e .-target .-value)))}]]))

(defn menu-namespaces []
  (let [[search-term set-search-term] (react/useState "")
        project (filter
                  (fn [[path file-zloc]]
                    (clojure.string/includes?
                      (str (refactor/get-ns file-zloc))
                      search-term))
                  @(subscribe [:db/get [:parenoia :project]]))
        namespaces (map refactor/get-ns (map second project))
        generated-colors (generate-colors namespaces)]

    [:div {:style {:display :flex
                   :flex-direction :column
                   :gap "10px"
                   :justify-content :flex-start}}
     [namespace-search search-term set-search-term]
     (map
       (fn [a project-item] ^{:key a} [:div [menu-namespace a project-item generated-colors]])
       (sort namespaces)
       (sort (fn [a b] (compare (refactor/get-ns (second a))
                         (refactor/get-ns (second b))))
         project))]))

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
      (fn []))
    #js [])
  (let [menu? @(subscribe [:db/get [:parenoia :menu?]])]

    [:div (str menu?)
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