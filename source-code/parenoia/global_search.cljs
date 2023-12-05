(ns parenoia.global-search
  (:require ["react" :as react]
            [parenoia.keyboard :as keyboard]
            [re-frame.core :refer [dispatch subscribe]]))



(defn result-namespace [result]
  [:div
   {:style
    {:background "#FFBF00"
     :padding "10px"
     :border-bottom-left-radius "10px"
     :border-bottom-right-radius "10px"
     :font-weight :bold
     :color "#333"}}
   (:namespace result)])



(defn result-context [result]
  [:pre
   {:style {:overflow-x :auto
            :padding "10px"}}
   (str (:content result))])



(defn one-result [result]
  [:div
   {:on-click #(dispatch [:parenoia/go-to!
                          (:file-path result)
                          (:position result)])
    :style {:color "white"
            :cursor :pointer
            :margin-bottom "20px"}}

   [result-namespace result]
   [:pre
    {:style {:overflow-x :auto
             :padding "10px"}}
    (str (:content result))]])



(defn search-input [ref set-term term set-timeout? timeout?]
  [:input.global-search
   {:ref ref
    :style {:border-radius "20px"
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
                     500)))}])



(defn result-iterator [results]
  (when-not (empty? results)
    [:div
     {:style {:width "400px"
              :border-radius "10px"
              :overflow-y :auto
              :max-height "80vh"

              :background "#111"}}

     (map (fn [a] ^{:key (str a)} [one-result a])
       results)]))



(defn autofocus-effect [global-search? ref]
  (react/useEffect
    (fn [] (when global-search?
             (.select (.-current ref)))
      (fn []))

    #js [global-search?]))



(defn search-view-style [global-search?]
  {:padding "5px 10px"
   :display (if global-search? :flex :none)
   :position :fixed
   :left "50%"
   :top 50
   :transform "translateX(-50%)"
   :z-index 1000
   :flex-direction :column
   :justify-content :center
   :align-items :flex-end})



(defn view []
  (let [ref (react/useRef)
        global-search? (subscribe [:db/get [:parenoia :global-search?]])
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
    (autofocus-effect @global-search? ref)

    [:div {:style (search-view-style @global-search?)}
     [search-input ref set-term term set-timeout? timeout?]
     [result-iterator results]]))