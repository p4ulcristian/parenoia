(ns parenoia.token
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as znode]
            [parenoia.style :as style]
            [parenoia.overlays :as overlays]
            ["react" :as react]
            [re-frame.core :refer [subscribe dispatch]]))


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


(defn first-in-list? [zloc]
  (let [is-first? (z/leftmost? zloc)
        is-in-list? (z/list? (z/up zloc))]
    (and is-first? is-in-list?)))

(defn uri->path [uri]
  (apply str (drop 7 uri)))



(defn label-for-bucket [bucket]
 [:div 
   {:style {:background "#ffbf00" 
             :color "#333"
             :border-radius "5px"
             :padding "5px 10px"}}  
   (str bucket)])


(defn label-for-function [ns-name fn-name]
 [:div 
        (if ns-name 
         (str ns-name "/" fn-name)
         (str fn-name))])



(defn one-reference [the-ref]
 (let [selected-zloc (subscribe [:db/get [:parenoia :selected-zloc]])
       selected-position (subscribe [:parenoia/selected-position])
       {:keys [uri row col from alias name bucket]} the-ref
       
       reference-zloc  (z/find-last-by-pos @(subscribe [:db/get [:parenoia :project (uri->path uri)]])
                                           [row col])
       reference-is-first-parameter? (boolean (z/down reference-zloc))
       reference-pos (if reference-is-first-parameter? 
                      (z/position (z/down reference-zloc))
                      [row col])
       same-as-selected? false] 
  (when-not same-as-selected?
   [:div {:style {:padding "5px"
                  :display :grid 
                  :gap "8px"
                  :white-space :nowrap
                  :grid-template-columns "auto auto"}
          :on-click (fn [e]
                     (.stopPropagation e)
                     (dispatch [:parenoia/go-to! (uri->path uri) reference-pos]))}
      [label-for-function from name]
      [label-for-bucket bucket]])))     

(defn go-to-references-button [the-refs]
   [:div 
       (map  (fn [a] 
              ^{:key (str (random-uuid))}[one-reference a])
             the-refs)])

(defn go-to-definition-button [the-def]
    (let [{:keys [uri row col name namespace bucket]} the-def]
     
       [:div {:style {:padding "5px"
                      :display :grid 
                      :gap "8px"
                      :white-space :nowrap
                      :grid-template-columns "auto auto"}
              :on-click 
              (fn [e]
                (.stopPropagation e)
                (dispatch [:parenoia/go-to! 
                                (uri->path uri) 
                                [row col]]))}
        [label-for-function namespace name]
        [label-for-bucket bucket]]))


(defn references-button [open? set-open?]
 [:div {:style {:display :flex 
                :justify-content :center 
                :align-items :center 
                :height 30 
                :width 30 
                :border "1px solid white"
                :border-radius "50%"
                :background "#333"
                :color "lightblue"
                :transform (if open? "scale(1)" "scale(0.7)")}}
  [:i {:style {:font-size "16px"}
       :class "fa-solid fa-book-journal-whills"}]])



(defn refs-and-def-title [title]
 [:div 
   {:style {:border-bottom "1px solid black"
            :font-weight :bold
            :padding "10px"}}
   [:div title]])




(defn references-and-definition [the-definition the-references]

  [:div 
    {:style {:background :lightblue 
             :border-radius "10px"
             
             :color :black
             :height "fit-content"}}
             
    [refs-and-def-title "Definition"]
    [go-to-definition-button the-definition]  
    [refs-and-def-title "References"]
    [go-to-references-button the-references]])
       


(defn references-overlay [ref]
  (let [[open? set-open?] (react/useState false)
        the-definition     (subscribe [:db/get [:parenoia :definition]])
        the-references     (subscribe [:db/get [:parenoia :references]])
        no-findings? (and (not (:uri the-definition)) (empty? @the-references))]
        
    [overlays/overlay-wrapper
     ref
     [:div {:on-mouse-enter #(set-open? true)
            :on-mouse-leave #(set-open? false)
            :style {:pointer-events :auto
                    :height (if open? "fit-content" "0")
                    :width (if open? "fit-content" "0")
                    :transform "translate( -15px, -15px)"
                    :border-radius (if open? "10px" "50%")
                    :position :absolute 
                    :left 0 
                    :top 0}}
      (if-not open?
        (when-not no-findings? 
          [references-button open?  set-open?])
        [:div {:style {:display :grid 
                       :grid-template-columns "auto auto"
                        :gap "3px"}}
                        
         [references-button open? set-open?]
         [references-and-definition @the-definition @the-references]])]
     open?]))


(defn token-inner [selected? unused-binding? token-color token-text-color first-in-list? token-string]
  (let [ref (react/useRef)]
   [:div {:ref ref
          :style {:box-shadow style/box-shadow
                  :border-radius "10px"
                  :padding-left "8px"
                  :padding-right "10px"
                  :padding-top    "5px"
                  :padding-bottom "5px"
                  :position :relative
                  :white-space :nowrap
                  :border-left (str "3px solid " (cond
                                                    ;;  same-as-selected?     "magenta"
                                                   first-in-list? "magenta"
                                                   :else "none"))
                  :color (cond
                           selected? (style/color [:selection :text-color])
                             ;same-as-selected? (style/color [:same-as-selection :text-color])
                           unused-binding?   (style/color [:unused-binding :text-color])
                           :else token-text-color)
                  :background (cond
                                selected?         (style/color [:selection :background-color])
                                  ;same-as-selected? (style/color [:same-as-selection :background-color])

                                unused-binding?   (style/color [:unused-binding :background-color])
                                :else token-color)}}
    (when selected? [references-overlay ref])
    [:div token-string]]))

(defn view [zloc selected?]
  (let [token-color (decide-token-color zloc)
        token-text-color (decide-token-text-color zloc)
        token-string  (if (= nil (z/tag zloc))
                          [:br]
                          (z/string zloc))
        first-in-list? (first-in-list? zloc)
        unused-binding?   (subscribe [:parenoia/unused-binding? zloc])]
    [token-inner  selected? @unused-binding? token-color token-text-color first-in-list? token-string]))