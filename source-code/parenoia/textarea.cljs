(ns parenoia.textarea
  (:require ["@webscopeio/react-textarea-autocomplete" :default ReactTextareaAutocomplete]
            ["parinfer" :as parinfer]
            ["react" :as react]
            [parenoia.keyboard :as keyboard]
            [parenoia.rewrite :as rewrite]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :refer [atom] :as reagent]
            [rewrite-clj.parser :as zparser]
            [rewrite-clj.zip :as z]))

(defn autofocus-input--unmount [current-ref zloc]
  (fn []
    (let [og-string (z/string zloc)
          edited-string (try (zparser/parse-string
                               (.-text (parinfer/smartMode (.-value ^js current-ref)
                                         #js {:forceBalance true})))
                          (catch js/Error e "veryspecific-&error"))
          error? (= edited-string "veryspecific-&error")
          same?  (= og-string (.-value ^js current-ref))
          edited-zloc (z/edit zloc (fn [e]  edited-string))]

      (when-not (or same? error?)
        (do
          (keyboard/modify-file edited-zloc)
          (keyboard/set-zloc edited-zloc)
          (keyboard/remove-listener current-ref keyboard/block-some-keyboard-events))))))

(defn autofocus-input--effect [ref zloc]
  (react/useEffect
    (fn []
      (let [current-ref ref]
        (when ref
          (set! (.-value ref) (z/string zloc))
          (set! (.-onclick ref) (fn [a] (.stopPropagation a)))
          (keyboard/add-listener current-ref keyboard/block-some-keyboard-events)
          (.setTimeout js/window #(.select current-ref) 50))
        (if ref
          (autofocus-input--unmount current-ref zloc)
          (fn []))))
    #js [ref]))


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

(defn view [zloc]
  (let [[ref set-ref] (react/useState nil)]
    (autofocus-input--effect ref zloc)
    [autofocus-input-wrapper
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
                          :width "100%"
                          :pointer-events :auto
                          :z-index "10000"}
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
                        :output (fn [item trigger] (str (.-function ^js item)))}}}]]))
