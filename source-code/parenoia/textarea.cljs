(ns parenoia.textarea
  (:require ["parinfer" :as parinfer]
            ["react" :as react]
            [reagent.core :refer [atom] :as reagent]
            [parenoia.keyboard :as keyboard]
            [re-frame.core :refer [dispatch subscribe]]
            [rewrite-clj.parser :as zparser]
            [rewrite-clj.zip :as z]
            [parenoia.rewrite :as rewrite]
            ["@webscopeio/react-textarea-autocomplete" :default ReactTextareaAutocomplete]))

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
      (let [current-ref ref]
       (when ref 
       
          (do
            (reset! autofocus-input-value (z/string zloc))
            (keyboard/add-listener current-ref keyboard/block-some-keyboard-events)
            (.setTimeout js/window #(.select current-ref) 50)))
       (if ref 
        (autofocus-input--unmount current-ref zloc)
        (fn []))))
       

    #js [ref]))

(defn autofocus-input-wrapper [content]
  [:div
   {:style {:position :absolute
            :top 0
            :left "50%"
            :min-width "100%"
            :height "100%"
            :min-height "100px"
            :transform "translateX(-50%)"
            :color "#333"}}
   content])


;;  <ReactTextareaAutocomplete
;;           className="my-textarea"
;;           loadingComponent={() => <span>Loading</span>}
;;           trigger={{ ... }}
;;           ref={(rta) => { this.rta = rta; } }
;;           onCaretPositionChange={this.onCaretPositionChange}
;;         />


(defn autocomplete-item-inner [^js data]
  (let [ref (react/useRef)
        selected? (.-selected data)
        entity (.-entity  data)
        function (.-function entity)
        namespace (.-namespace entity)]
   [:div.autocomplete-item
     {:ref ref
      :style {:background (if selected? "turquoise" "#333")
              :padding "10px"
              :position :relative
              :border-radius "5px"
              :white-space :nowrap
              :width "fit-content"
              :color      (if selected? "#333" "#FFF")}}     
     [:div (str function)]
     [:div {:style {:position :absolute 
                    :top "50%"
                    :font-size "12px"
                    :font-weight "bold"
                    :background "rgb(255, 191, 0)"
                    :border-radius "5px"
                     :right 0
                     :color :black
                     :padding "5px"
                     :width :fit-content
                     :transform "translate(110%, -50%)"}}
        (str namespace)]]))   
      
      

(defn autocomplete-item [data]
 (reagent/as-element [autocomplete-item-inner data]))

(defn autocomplete-loading [data]

 [:div {:style {:color :white}}
   (str (js->clj  data))])

(defn get-fn-name [zloc]
 (when (z/down zloc)
  (z/string (z/right (z/down zloc)))))

(defn filter-results [results token]
 (take 20 
  (vec 
   (doall 
    (filter 
     (fn [result] 
      (or (clojure.string/includes? (:function result) (str token))
          (clojure.string/includes? (:function result) (str ":" token))))
       ;(clojure.string/includes? (:function result) (str ":" token))))
     results)))))


(defn get-all-autocomplete-results [token]
 (let [zloc  @(subscribe [:db/get [:parenoia :selected-zloc]])
       zlocs (map second @(subscribe [:db/get [:parenoia :project]]))]
  (sort-by :function (filter-results
                      (reduce concat 
                       (map (fn [file-zloc] 
                               (let [forms   (rewrite/get-forms-from-file file-zloc)
                                     ns-name (rewrite/get-namespace-from-file file-zloc)]
                                (remove nil? 
                                 (map (fn [form-zloc]
                                       (let [fn-name (get-fn-name form-zloc)]
                                        (when (and fn-name ns-name)
                                          {:function  fn-name
                                           :namespace ns-name})))
                                      forms)))) 
                           zlocs))
                      token))))  
                       

(defn view [zloc]
  (let [[ref set-ref] (react/useState nil)
        og-string  (z/string zloc)
        on-change (fn [^js event] (let [value (-> event .-target .-value)]
                                    (reset! autofocus-input-value value)))]

    (autofocus-input--effect ref zloc)
    [autofocus-input-wrapper
     [:<>
      [:> ReactTextareaAutocomplete 
       {:className "my-textarea"
        :loadingComponent  (fn [data] (reagent/as-element [autocomplete-loading data]))
        :innerRef (fn [a] (set-ref a))
        :movePopupAsYouType true
        ;:renderToBody true
        :listStyle {:z-index 1000
                    :color :white
                    :backdrop-filter "3px"
                     :list-style-type :none
                     :position :fixed}
                     
                     
      
                    
        :containerStyle {:min-width 100
                         :background "transparent"
                          :box-sizing "border-box"
                          :height "100%"
                          :width "100%"}
        :dropdownStyle {:position :fixed}                  
        :style {:height "100%"
                :width "100%"
                :box-sizing "border-box"
                
                :background "#333"
                :color :white
                :padding "10px"
                :border-radius "10px"
                :white-space "pre-wrap"}
        :trigger {";" {:dataProvider (fn [token] 
                                        (clj->js (get-all-autocomplete-results token)))
                       :component autocomplete-item
                       :output (fn [item trigger] (str (.-function ^js item)))}}}]]]))            
      ;; [:textarea {:style {:position :absolute
                          
      ;;                     :border-radius 10
      ;;                     :left 0
      ;;                     :white-space "pre-wrap"
      ;;                     :background "#333"
      ;;                     :color :white
      ;;                     :top 0
      ;;                      ;:transform "translateY(-100%)"
      ;;                     :min-width 100
      ;;                     :box-sizing "border-box"
      ;;                     :height "100%"
      ;;                     :width "100%"

      ;;                     :padding "5px"
      ;;                     :z-index 1000}
      ;;              :ref ref
      ;;             :value @autofocus-input-value
      ;;               ;:autofocus true
      ;;              :on-click #(.stopPropagation %)

      ;;             :on-change  on-change}]]]))