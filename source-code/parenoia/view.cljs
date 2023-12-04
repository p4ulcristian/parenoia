(ns parenoia.view
  (:require
   ["react" :as react]
   [clojure.string :as clojure.string]
   [parenoia.events]
   [parenoia.form-conditionals :as form-conditionals]
   [parenoia.form-interpreters  :as form-interpreters]
   [parenoia.keyboard :as keyboard]
   [parenoia.lint :as lint]
   [parenoia.menu :as menu]
   [parenoia.namespace-graph :as namespace-graph]
   [parenoia.overlays :as overlays]
   [parenoia.refactor-ui :as refactor-ui]
   [parenoia.rewrite :as rewrite]
   [parenoia.style :as style]
   [parenoia.textarea :as textarea]
   [parenoia.token :as token]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :refer [atom] :as reagent]
   [rewrite-clj.zip :as z]
   [reagent.dom.server :as reagent.dom.server]))

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

(defn new-line-before-last? [zloc]
  (if (= :newline (z/tag zloc))
    true
    (if (z/whitespace-or-comment? zloc)
      (new-line-before-last? (z/left* zloc))
      false)))

(def timeout (atom nil))

(defn get-info-about-zloc [zloc]
  (do
    (dispatch [:parenoia/get-variable-info zloc])
    (dispatch [:parenoia/get-form-info zloc])
    (dispatch [:parenoia/get-kondo-lints zloc])
    (dispatch [:parenoia/get-definition zloc])
    (dispatch [:parenoia/get-references zloc])))

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
    ^{:key (z/string zloc)}
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
      (when-not selected? [lint/view this-pos zloc ref])
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
        :else [token/view zloc selected?])
      (if (and selected? editable?)

        [overlays/overlay-wrapper
         ref [textarea/view zloc]])]]))

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



(defn block-re-render [component block?]
  (if block?
   [:div.block-re-render
    (if block? 
     {:dangerouslySetInnerHTML
      {:__html (reagent.dom.server/render-to-static-markup
                      component)}})]
   component))
  
  

(defn in-position-span? [position-span position]
 (let [[[start-x start-y] [end-x end-y]] position-span
       [x y] position]
    (and (<= start-x x end-x) (<= start-y y end-y))))  

(defn form-container [zloc index ns-name]
   (let [[hovered? set-hovered?] (react/useState false)]
    (if zloc
     (let [form-position-span (z/position-span zloc)
           selection-position (has-position? @(subscribe [:db/get [:parenoia :selected-zloc]]))]
      [:div
       {:on-mouse-enter #(set-hovered? true)
        :on-mouse-leave #(set-hovered? false)
        :style {:border-radius "10px"
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
          [block-re-render 
           [form-interpreter zloc] 
           (not hovered?)]]]]]))))
           ;(not (in-position-span? form-position-span selection-position))]]]])))

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

(defn placeholder-div []
  [:div {:style {:height "80vh"
                 :background "linear-gradient(180deg, rgba(51,51,51,1) 21%, rgba(42,42,42,1) 44%, rgba(68,68,68,1) 66%, rgba(0,0,0,1) 91%)"}}])

(defn namespace-container []
  (let [ref (react/useRef)
        current-zloc (subscribe [:db/get [:parenoia :selected-zloc]])
        selected-file-path @(subscribe [:db/get [:parenoia :selected :file-path]])
        selected-file @(subscribe [:db/get [:parenoia :project selected-file-path]])]
    (load-effect)
    (keyboard/effect @current-zloc)
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
     [placeholder-div]]))

(defn view []

  [:div {:class "parenoia-background"
         :style {:color "#EEE"
                 :height "100vh"
                 :width "100vw"}}
   [title]
   [menu/view]
   [namespace-graph/view]
   [namespace-container]
   ;[refactor-ui/view]
   [pins]
   [global-search]])
