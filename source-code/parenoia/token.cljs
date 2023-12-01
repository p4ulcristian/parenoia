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


(defn mindfuck-zloc-equality? [pos-one pos-two])


(defn one-reference [the-ref]
 (let [selected-zloc (subscribe [:db/get [:parenoia :selected-zloc]])
       selected-position (subscribe [:parenoia/selected-position])
       {:keys [uri row col from alias name]} the-ref
       
       reference-zloc  (z/find-last-by-pos @(subscribe [:db/get [:parenoia :project (uri->path uri)]])
                                           [row col])
       reference-is-first-parameter? (boolean (z/down reference-zloc))
       reference-pos (if reference-is-first-parameter? 
                      (z/position (z/down reference-zloc))
                      [row col])
       same-as-selected? false] 
  (println "Hello peti: " reference-is-first-parameter?)
  (when-not same-as-selected?
   [:div {:style {:padding "5px"}
          :on-click (fn [e]
                     (.stopPropagation e)
                     (dispatch [:parenoia/go-to! (uri->path uri) reference-pos]))}
      (if from 
           (str from "/" name)
           (str name))
      [:div (str [row col] " - " @selected-position)]])))     

(defn go-to-references-button [the-refs]
   [:div 
       (map  (fn [a] 
              ^{:key (str (random-uuid))}[one-reference a])
             the-refs)])

(defn go-to-definition-button [the-def]
    (let [{:keys [uri row col name namespace bucket]} the-def]
     
       [:div {:style {:padding "5px"}
              :on-click 
              (fn [e]
                (.stopPropagation e)
                (dispatch [:parenoia/go-to! 
                                (uri->path uri) 
                                [row col]]))}
        (if namespace 
          (str namespace "/" name)
          (str name))
        [:div(str bucket)]]))


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
  [:i {:style {:font-size "14px"}
       :class "fa-solid fa-circle-nodes"}]])



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
             :width "300px"
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
            :style {:z-index (if open? 10000 5000)
                    :pointer-events :auto
                    :height (if open? "fit-content" "0")
                    :width (if open? "fit-content" "0")
                    :transform "translate(calc(100% - 15px), -15px)"
                    :border-radius (if open? "10px" "50%")
                    :position :absolute 
                    :right 0 
                    :top 0}}
      (if-not open?
        (when-not no-findings? 
          [references-button open?  set-open?])
        [:div {:style {:display :grid 
                       :grid-template-columns "auto auto"
                        :gap "3px"}}
                        
         [references-button open? set-open?]
         [references-and-definition @the-definition @the-references]])]]))


(defn token-inner [zloc selected? unused-binding?]
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
                                                   (first-in-list? zloc) "magenta"
                                                   :else "none"))
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
    (when selected? [references-overlay ref])
    [:div
     (if (= nil (z/tag zloc))
       [:br]
       (z/string zloc))]]))

(defn view [zloc selected?]
  (let [unused-binding?   (subscribe [:parenoia/unused-binding? zloc])]
    [token-inner zloc selected? @unused-binding?]))