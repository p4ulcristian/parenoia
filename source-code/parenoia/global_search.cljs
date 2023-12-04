(ns parenoia.global-search
  (:require ["react" :as react]
            [parenoia.keyboard :as keyboard]
            [re-frame.core :refer [dispatch subscribe]]))
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
(defn view []
  (let [ref (react/useRef)
        global-search? (subscribe [:db/get [:parenoia :global-search?]])
        toggle-global-search-fn (fn [] (dispatch [:db/set [:parenoia :global-search?]
                                                  (not @global-search?)]))
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
                   :display (if @global-search? :flex :none)
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