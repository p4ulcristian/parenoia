(ns parenoia.view
  (:require
   ["react" :as react]
   ["react-dom" :as react-dom]
   [cljs.reader :as reader]
   [clojure.string :as clojure.string]
   [parenoia.events]
   [parenoia.form-conditionals :as form-conditionals]
   [parenoia.form-interpreters  :as form-interpreters]
   [parenoia.keyboard :as keyboard]
   [parenoia.namespace-graph :as namespace-graph]
   [parenoia.refactor :as refactor]
   [parenoia.refactor-ui :as refactor-ui]
   [parenoia.rewrite :as rewrite]
   [parenoia.style :as style]
   [parenoia.textarea :as textarea]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :refer [atom] :as reagent]
   [rewrite-clj.node :as znode]
   [rewrite-clj.parser :as zparser]
   [rewrite-clj.zip :as z]))

(defn load-effect []
  (react/useEffect
    (fn []
      (dispatch [:parenoia/get-files])
      (fn []))
    #js []))

(defn has-position? [zloc]
  (try (z/position zloc)
    (catch js/Error e false)))

(defn split-namespace [namespace]
  (clojure.string/join
    " || "
    (clojure.string/split  namespace #"\.")))

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

(defn overlay-wrapper-beta [ref set-open? content additional-style]
  (let [[x set-x] (react/useState 0)
        [y set-y] (react/useState 0)
        [width set-width] (react/useState 0)
        [height set-height] (react/useState 0)]
    (react/useEffect
      (fn []
        (let [scroll-top (.-scrollTop (.getElementById js/document "parenoia-body"))
              scroll-left (.-scrollLeft (.getElementById js/document "parenoia-body"))
              this-x (.-x (.getBoundingClientRect (.-current ref)))
              this-y (.-y (.getBoundingClientRect (.-current ref)))
              this-height (.-height (.getBoundingClientRect (.-current ref)))
              this-width  (.-width (.getBoundingClientRect (.-current ref)))]
          (set-x (+ scroll-left this-x))
          (set-y (+ scroll-top this-y))
          (set-height this-height)
          (set-width this-width))
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
                                :left x
                                :height height
                                :width width
                                :min-width "400px"
                                :min-height "50px"}
                          additional-style)}
           content])
        (.getElementById js/document "parenoia-body")))))

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

(defn lint [position zloc ref]
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

(defn find-top-form-recursion [last-zloc zloc]
  (let [up-loc (z/up zloc)]
    (cond
      (nil? up-loc) last-zloc
      :else (recur zloc up-loc))))

(defn find-top-form [zloc]
  (find-top-form-recursion nil zloc))

(defn first-in-list? [zloc]
  (let [is-first? (z/leftmost? zloc)
        is-in-list? (z/list? (z/up zloc))]
    (and is-first? is-in-list?)))

(defn token-inner [zloc selected? unused-binding?]
  [:div {:style {:box-shadow style/box-shadow
                 :border-radius "10px"
                 :padding-left "8px"
                 :padding-right "10px"
                 :padding-top    "5px"
                 :padding-bottom "5px"
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
    (if (= nil (z/tag zloc))
      [:br]
      (z/string zloc))]])

(defn token [zloc selected?]
  (let [unused-binding?   (subscribe [:parenoia/unused-binding? zloc])]
    [token-inner zloc selected? @unused-binding?]))

(defn new-line-before-last? [zloc]
  (if (= :newline (z/tag zloc))
    true
    (if (z/whitespace-or-comment? zloc)
      (new-line-before-last? (z/left* zloc))
      false)))

(def timeout (atom nil))

(defn get-info-about-zloc [zloc] (do
                                   (dispatch [:parenoia/get-variable-info zloc])
                                   (dispatch [:parenoia/get-form-info zloc])
                                   (dispatch [:parenoia/get-kondo-lints zloc])
                                   (dispatch [:parenoia/get-definition zloc])))

(defn form-interpreter-effect [zloc selected? ref timeout set-timeout]
  (react/useEffect
    (fn []
      (if selected?
        (do
          (get-info-about-zloc zloc)
          (let [el (.getElementById js/document "parenoia-body")
                rect (.getBoundingClientRect (.-current ref))
                scroll-top (.-scrollTop el)
                top (.-top rect)
                new-top (- (+ scroll-top top) (/ (.-innerHeight js/window) 2))]
            (set-timeout (.setTimeout js/window
                           (fn [] (.scrollTo el
                                    #js {:behavior "smooth"
                                         :top   new-top}))
                           100))))
        (if timeout
          (do
            (.clearTimeout js/window timeout)
            (set-timeout nil))))
      (fn []))
    #js [selected?]))

(defn form-interpreter-inner [zloc selected? editable? form-interpreter]
  (let [ref (react/useRef)
        this-pos     (has-position? zloc)
        [timeout set-timeout] (react/useState)]
    (form-interpreter-effect zloc selected? ref timeout set-timeout)
    ^{:key (str this-pos (z/string zloc))}
    [:div {:style {:display :flex
                   :justify-content :flex-start
                   :align-items :flex-start
                   :padding "5px"}}

     [:div.form-interpreter
      {:class (when selected? "selected")
       :ref ref
       :style {:position :relative
               :box-sizing :border-box
               :border-radius "10px"
               :pointer-events (if zloc "auto" "none")}
       :on-click (fn [e]
                   (.stopPropagation e)
                   (dispatch [:db/set [:parenoia :editable?] false])
                   (js/window.setTimeout
                     (fn [] (dispatch [:db/set [:parenoia :selected-zloc] zloc]))
                     50))}
      [lint this-pos zloc ref]
      (cond
        (form-conditionals/is-ns? zloc)
        [form-interpreters/ns-interpreter zloc form-interpreter selected?]
        (form-conditionals/is-defn? zloc)
        [form-interpreters/defn-interpreter zloc form-interpreter selected?]
        (form-conditionals/is-def? zloc)
        [form-interpreters/def-interpreter zloc form-interpreter selected?]
        (or
          (form-conditionals/is-let-vector? zloc)
          (form-conditionals/is-loop-vector? zloc))
        [form-interpreters/let-vector-interpreter  zloc form-interpreter selected?]
        (form-conditionals/is-map? zloc)
        [form-interpreters/map-interpreter  zloc form-interpreter selected?]
        (form-conditionals/is-cond? zloc)
        [form-interpreters/cond-interpreter  zloc form-interpreter selected?]
        (form-conditionals/is-case? zloc)
        [form-interpreters/case-interpreter  zloc form-interpreter selected?]
        (form-conditionals/is-if? zloc)
        [form-interpreters/if-interpreter  zloc form-interpreter selected?]
        (or
          (form-conditionals/is-vector? zloc)
          (form-conditionals/is-function? zloc))
        [form-interpreters/function-interpreter  zloc form-interpreter selected?]
        (form-conditionals/is-reader-macro? zloc)
        [form-interpreters/reader-macro-interpreter  zloc form-interpreter selected?]
        (form-conditionals/is-deref? zloc)
        [form-interpreters/deref-interpreter  zloc form-interpreter selected?]
        (form-conditionals/is-meta? zloc)
        [form-interpreters/meta-interpreter  zloc form-interpreter selected?]
        (form-conditionals/is-anonym-fn? zloc)
        [form-interpreters/anonym-fn-interpreter  zloc form-interpreter selected?]
        ;; (form-conditionals/is-function? zloc)
        ;; [form-interpreters/form-interpreter-iterator (z/down zloc) form-interpreter :horizontal]
        :else [token zloc selected?])
      (if (and selected? editable?)

        [overlay-wrapper-beta
         ref (fn [e])
         [textarea/view zloc]
         {:z-index 10000}])]]))

(defn form-interpreter [zloc]
  (let [selected? (subscribe [:parenoia/selected? zloc])
        editable?     (subscribe [:parenoia/editable? zloc])]
    [form-interpreter-inner zloc @selected? @editable?
     form-interpreter]))

(defn sticky-function-header [zloc index ns-name]
  [:div {:style {:position :sticky
                 :top 0
                 :z-index 1000}}
   [:div
    {:style {:background "#FFBF00"

             :box-shadow style/box-shadow
             :padding "10px"
             :color "#333"
             :border "1px solid white"
             :border-bottom-left-radius 10
             :border-bottom-right-radius 10
             :display :flex
             :align-items :center
             :gap "10px"}}
    [:div
     {:on-click (fn [e]
                  (.stopPropagation e)
                  (dispatch [:parenoia/add-pin! zloc]))
      :style {:border "1px solid black"
              :height 30
              :width 30
              :border-radius "50%"
              :display :flex
              :justify-content :center
              :align-items :center
              :background "lightgreen"}}
     [:div {:style {:color "magenta"}}
      [:i {:class "fa-solid fa-location-dot"}]]]
    [:div
     (str index
       " | "
       (z/string (z/right (z/down zloc))))]
    [:div {:style {:flex-grow 1
                   :text-align :right}}
     ns-name]]])

(defn form-container [zloc index ns-name]
  [:div
   {:style {:border-radius "10px"
            :position :relative
            :overflow-wrap "break-word"}}
   [sticky-function-header zloc index ns-name]
   [:div {:style {:padding "40px"}}
    [:div {:style {:width "100%"
                   :box-sizing :border-box
                   :overflow-x :auto}}
     [:div {:style {:display :flex
                    :gap "10px"

                    :flex-wrap :wrap
                    :margin-top 10}}
      [form-interpreter zloc]]]]])

(defn forms-container [forms ns-name]
  (let [style {:display :flex
               :flex-direction :column
               :gap "20px"}

        render-fn (fn [index form] ^{:key index} [form-container form index ns-name])]
    [:div {:style style
           :on-click (fn [e] (.stopPropagation e))}
     (map-indexed render-fn forms)]))

(defn one-namespace [file-path zloc]
  (let [[clicked? set-clicked?] (react/useState true)
        style {:font-size "16px"
               :font-weight :bold
               :cursor :pointer
               :padding-bottom "100px"}

        ns-name (rewrite/get-namespace-from-file zloc)]
    [:div {:style style}
     [forms-container (rewrite/get-forms-from-file zloc) ns-name]]))

(defn parenoia-icon []
  (let [[open? set-open?] (react/useState false)]
    [:div
     {:on-mouse-enter #(set-open? true)
      :on-mouse-leave #(set-open? false)
      :on-click #(dispatch [:db/set [:parenoia :menu?] (not @(subscribe [:db/get [:parenoia :menu?]]))])}
     [:i {:style {:font-size "22px"
                  :cursor :pointer}
          :class (if open?
                   "fa-solid fa-circle"
                   "fa-regular fa-circle")}]]))

(defn one-result [result]
  [:div
   {:on-click #(dispatch [:parenoia/go-to!
                          (:file-path result)
                          (:position result)])
    :style {:color "white"
            :cursor :pointer
            :margin-bottom "20px"}}

   [:div
    {:style
     {:background "#FFBF00"
      :padding "10px"
      :border-bottom-left-radius "10px"
      :border-bottom-right-radius "10px"
      :font-weight :bold
      :color "#333"}}
    (:namespace result)]
   [:pre
    {:style {:overflow-x :auto
             :padding "10px"}}
    (str (:content result))]])
   ;[:pre (:content result)]])

(defn global-search []
  (let [ref (react/useRef)
        [term set-term] (react/useState "")
        [timeout? set-timeout?] (react/useState nil)
        results (or @(subscribe [:db/get [:parenoia :search-results]]) [])]
    (react/useEffect
      (fn []
        (let [current-ref (.-current ref)]
          (keyboard/add-listener current-ref keyboard/block-some-keyboard-events)
          (fn []
            (keyboard/remove-listener current-ref keyboard/block-some-keyboard-events))))
      #js [])
    [:div {:ref ref
           :style {:padding "5px 10px"
                   :display :flex
                   :position :fixed
                   :right 0
                   :top 50
                   :z-index 1000
                   :flex-direction :column
                   :justify-content :center
                   :align-items :flex-end}}

     [:input.global-search
      {:style {:border-bottom-left-radius "50px"
               :border-bottom-right-radius "10px"
               :border-top-left-radius "10px"
               :border-top-right-radius "10px"
               :padding "5px"
               :text-align :center
               :font-weight :bold}

       :placeholder "search."
       :value term
       :on-change (fn [a]
                    (set-term (-> a .-target .-value))
                    (if timeout?
                      (.clearTimeout js/window timeout?))
                    (set-timeout?
                      (.setTimeout js/window
                        (fn [e]
                          (set-timeout? nil)
                          (dispatch [:parenoia/global-search (-> a .-target .-value)]))
                        500)))}]
     (when-not (empty? results)
       [:div
        {:style {:width "400px"
                 :border-radius "10px"
                 :overflow-y :auto
                 :max-height "80vh"

                 :background "#111"}}

        (map (fn [a] ^{:key (str a)} [one-result a])
          results)])]))

(defn title []
  (let [style {:font-weight :bold
               :font-size "28px"

               :border-bottom-left-radius "20px"
               :border-bottom-right-radius "20px"

               :padding "10px 20px"
               :color "#333"
               :display :flex
               :justify-content :center
               :align-items :center
               :box-shadow style/box-shadow
               :gap "5px"
               :font-family "'Syne Mono', monospace"
               :background "radial-gradient(circle, rgba(245,201,49,1) 0%, rgba(191,165,76,1) 100%)"}]

    [:div   {:style {:top 0
                     :left "50%"
                     :z-index 1000
                     :transform "translateX(-50%)"
                     :position :fixed}}
     [:div
      {:style style}
      [:div "Paren"]
      [parenoia-icon]
      [:div [:span "ia"]]]]))

(defn keyboard-shortcut [shortcut desc]
  [:<>
   [:div shortcut]
   [:div desc]])

(defn keyboard-shortcuts []
  [:div {:style {:position :fixed
                 :right 0
                 :top 0
                 :background "rgba(255,255,255,0.3)"
                 :display :grid
                 :grid-template-areas "a b"
                 :grid-template-columns "auto auto"
                 :gap "10px"
                 :padding "10px"}}
   [keyboard-shortcut "<-" "paredit slurp-backward"]
   [keyboard-shortcut "->" "paredit barf-backward"]
   [keyboard-shortcut "shift + <-" "paredit barf-backward"]
   [keyboard-shortcut "shift + ->" "paredit slurp-backward"]
   [keyboard-shortcut "tab" "wrap-around"]
   [keyboard-shortcut "shift + tab" "remove-wrap-around"]
   [keyboard-shortcut "up" "select prev node"]
   [keyboard-shortcut "down" "select next node"]])

(defn pins []
  (let [pins-data @(subscribe [:db/get [:parenoia :pins]])]
    [:div
     {:style {:position :fixed
              :bottom 0

              :right 0
              :background "#333"
              :border-bottom-left-radius "10px"
              :border-top-left-radius "10px"
              :z-index 1000
              :padding "10px"
              :display :flex
              :flex-direction :column
              :gap 5}}
     (map (fn [one-pin]
            [:div {:on-click #(dispatch [:parenoia/select-pin! one-pin])
                   :style {:color "magenta"
                           :gap "5px"
                           :display :flex
                           :cursor :pointer}}
             [:i {:on-click (fn [e] (.stopPropagation e)
                              (dispatch [:parenoia/remove-pin! one-pin]))
                  :class "fa-solid fa-x"}]
             [:i {:class "fa-solid fa-location-dot"}]
             [:div (str (:function-name one-pin))]])

       pins-data)]))

(defn namespace-container []
  (let [ref (react/useRef)
        current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])
        selected-file-path @(subscribe [:db/get [:parenoia :selected :file-path]])
        selected-file @(subscribe [:db/get [:parenoia :project selected-file-path]])]
    (load-effect)
    (keyboard/effect ref)
    [:div {:ref ref
           :id "parenoia-body"
           :style {:height "100vh"
                   :width "100vw"
                   :overflow-y "scroll"
                   :position :fixed
                   :right 0
                   :top 0
                   :z-index 10}}
     ^{:key (str selected-file)}
     [one-namespace selected-file-path selected-file]
     [:div {:style {:height "80vh"}}]]))

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
        [path set-path] (react/useState "/Users/paulcristian/projects/zgen/wizard")]
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

(defn menu []
  (react/useEffect
    (fn []
      (dispatch [:db/set [:parenoia :menu?] true])
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

(defn view []

  [:div {:class "parenoia-background"
         :style {:color "#EEE"
                 :height "100vh"
                 :width "100vw"}}
   [title]
   [menu]
   [namespace-graph/view]
   [namespace-container]
   [refactor-ui/view]
   [pins]
   [global-search]])
   ;[refactor-ui/view]])
