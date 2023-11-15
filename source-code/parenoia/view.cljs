(ns parenoia.view
  (:require ["parinfer" :as parinfer]
            ["react" :as react]
            [cljs.reader :as reader]
            [parenoia.events]
            [parenoia.form-conditionals :as form-conditionals]
            [parenoia.form-interpreters  :as form-interpreters]
            [parenoia.keyboard :as keyboard]
            [parenoia.namespace-graph :as namespace-graph]
            [parenoia.refactor :as refactor]
            [parenoia.refactor-ui :as refactor-ui]
            [parenoia.rewrite :as rewrite]
            [parenoia.style :as style]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :refer [atom]]
            [rewrite-clj.node :as znode]
            [rewrite-clj.parser :as zparser]
            [rewrite-clj.zip :as z]))

(defn load-effect []
  (react/useEffect
    (fn []
      (dispatch [:parenoia/get-files])
      (dispatch [:db/set [:parenoia :selected :file-path] "/Users/paulcristian/projects/zgen/wizard/source-code/wizard/editor_overlays/layer/areas/area_items.cljs"])

      (fn []))
    #js []))

(defn has-position? [zloc]
  (try (z/position zloc)
    (catch js/Error e false)))

(defn split-namespace [namespace]
  (clojure.string/join
    " || "
    (clojure.string/split  namespace #"\.")))

(def autofocus-input-value (atom nil))
(defn block-some-keyboard-events [^js e]
  (if
    (or (keyboard/check-key e "ArrowLeft")
      (keyboard/check-key e "ArrowRight")
      (keyboard/check-key e "ArrowDown")
      (keyboard/check-key e "ArrowUp")
      (and (.-shiftKey e) (keyboard/check-key e "Enter"))
      (keyboard/check-key e " ")
      (keyboard/check-key e "Backspace")
      (keyboard/check-key e "m"))
    (.stopPropagation e)))

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
          (keyboard/remove-listener current-ref block-some-keyboard-events))))))

(defn autofocus-input--effect [ref zloc]
  (react/useEffect
    (fn []
      (let [current-ref (.-current ref)]
        (do
          (reset! autofocus-input-value (z/string zloc))
          (keyboard/add-listener current-ref block-some-keyboard-events)
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
            :transform "translateX(-50%)"
            :color "#333"}}
   content])

(defn autofocus-input [zloc]
  (let [ref (react/useRef)
        og-string  (z/string zloc)
        on-change (fn [^js event] (let [value (-> event .-target .-value)]
                                    (reset! autofocus-input-value value)))]

    (autofocus-input--effect ref zloc)
    [autofocus-input-wrapper
     [:textarea {:ref ref
                 :value @autofocus-input-value
                ;:autofocus true
                 :on-click #(.stopPropagation %)
                 :style {:position :absolute
                         :left 0
                         :top 0
                         :width "100%"
                         :height "100%"
                         :min-width 100
                         :padding "5px"}
                 :on-change  on-change}]]))

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

(defn token [zloc selected?]
  (let [selected-pos (has-position? @(subscribe [:db/get [:parenoia :selected-zloc]]))
        this-pos     (has-position? zloc)]

    [:div {:style {:flex-grow 1
                   ;w:color "#333"
                   :box-shadow style/box-shadow
                   :border-radius "10px"
                   :padding "5px 10px"
                   :white-space :nowrap
                   :color (if selected?
                            (style/color [:selection :text-color])
                            (decide-token-text-color zloc))
                   :background (if selected?
                                 (style/color [:selection :background-color])
                                 (decide-token-color zloc))}}
     [:div (if (= nil (z/tag zloc))
             [:br]
             (z/string zloc))]]))

(defn new-line-before-last? [zloc]
  (if (= :newline (z/tag zloc))
    true
    (if (z/whitespace-or-comment? zloc)
      (new-line-before-last? (z/left* zloc))
      false)))

(defn form-interpreter [zloc]
  (let [ref (react/useRef)
        selected? (form-interpreters/selected-zloc? zloc)
        editable?     @(subscribe [:db/get [:parenoia :editable?]])]

    (react/useEffect
      (fn []
        (if selected? (.scrollIntoView
                        (.-current ref)
                        #js {:behavior "smooth"
                             :block "center"
                             :inline "center"}))
        (fn []))
      #js [selected?])
    [:div
     {:ref ref
      :style {:position :relative
              :box-sizing :border-box
              :border-radius "10px"
              :padding "5px"}
      :on-click (fn [e]
                  (.stopPropagation e)
                  (dispatch [:db/set [:parenoia :editable?] false])
                  (js/window.setTimeout
                    (fn [] (dispatch [:db/set [:parenoia :selected-zloc] zloc]))
                    50))}

      ;(str (new-line-before-last? (z/left* zloc)))
      ;(z/tag (z/right* (z/skip-whitespace zloc)))
     (cond
       (form-conditionals/is-ns? zloc)
       [form-interpreters/ns-interpreter zloc form-interpreter]
       (form-conditionals/is-defn? zloc)
       [form-interpreters/defn-interpreter zloc form-interpreter]
       (form-conditionals/is-def? zloc)
       [form-interpreters/def-interpreter zloc form-interpreter]
       (or
         (form-conditionals/is-let-vector? zloc)
         (form-conditionals/is-loop-vector? zloc))
       [form-interpreters/let-vector-interpreter  zloc form-interpreter]
       (form-conditionals/is-map? zloc)
       [form-interpreters/map-interpreter  zloc form-interpreter]
        ;(form-conditionals/is-vector? zloc)  
        ;[form-interpreters/vector-interpreter  zloc form-interpreter]
       (or
         (form-conditionals/is-vector? zloc)
         (form-conditionals/is-function? zloc))
       [form-interpreters/function-interpreter  zloc form-interpreter]
        ;; (form-conditionals/is-function? zloc)
        ;; [form-interpreters/form-interpreter-iterator (z/down zloc) form-interpreter :horizontal]
       :else [token zloc selected?])
     (if (and selected? editable?)
       [autofocus-input zloc])]))

(defn sticky-function-header [zloc index ns-name]
  [:div {:style {:position :sticky
                 :top 0
                 :z-index 1000}}
   [:div
    {:style {:background "#FFBF00"
             :padding "10px"
             :color "#333"
             :border-bottom-left-radius 10
             :border-bottom-right-radius 10
             :display :flex}}
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
   [:div {:style {:display :flex
                  :gap "10px"
                  :flex-wrap :wrap
                  :margin-top 10}}
    [form-interpreter zloc]]])

(defn forms-container [forms ns-name]
  (let [style {:display :flex
               :flex-direction :column
               :gap "20px"}

        render-fn (fn [index form] ^{:key index} [form-container form index ns-name])]
    [:div {:style style
           :on-click (fn [e] (.stopPropagation e))}
     (map-indexed render-fn forms)]))

(defn namespace-title [ns-name set-clicked? clicked? file-name]
  [:div {:style {:padding "0px 10px"
                 :user-select "none"}
         :on-click (fn [e]
                     (dispatch [:db/set [:parenoia :selected-file] file-name])
                     (dispatch [:db/set [:parenoia :editable?] false])
                     (set-clicked? (not clicked?)))}
   (str ns-name)])

(defn one-namespace [file-path zloc]
  (let [[clicked? set-clicked?] (react/useState true)
        style {:font-size "16px"
               :font-weight :bold
               :cursor :pointer
               :padding-bottom "100px"}

        ns-name (rewrite/get-namespace-from-file zloc)]
    [:div
     [:div {:style style}
      (str file-path)
      [namespace-title ns-name set-clicked? clicked? file-path]
      [forms-container (rewrite/get-forms-from-file zloc) ns-name]]]))

(defn title []
  (let [style {:font-weight :bold
               :display :flex
               :justify-content :center
               :font-size "24px"
               :padding "10px"
               :margin "10px"}]
    [:div
     {:style style}
     [:div "Parenoia"]]))

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

(defn namespace-container []
  (let [ref (react/useRef)
        selected-file-path @(subscribe [:db/get [:parenoia :selected :file-path]])
        selected-file @(subscribe [:db/get [:parenoia :project selected-file-path]])]
    (load-effect)
    (keyboard/effect ref)
    [:div {:ref ref
           :style {:height "100vh"
                   :width "100vw"
                   :overflow-y "scroll"
                   :position :fixed
                   :right 0
                   :top 0
                   :z-index 10}}
     [title]
     ^{:key (str selected-file)} [one-namespace selected-file-path selected-file]]))
     ;[namespace-graph/view]]))

     ;[namespaces  @(subscribe [:db/get [:parenoia :project]])]]))

(defn view []
  [:div {:style {:background "#333"
                 :color "#EEE"
                 :height "100vh"
                 :width "100vw"}}
   [namespace-graph/view]
   [namespace-container]
   [refactor-ui/view]])
