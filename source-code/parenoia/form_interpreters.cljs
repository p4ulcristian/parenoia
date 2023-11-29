(ns parenoia.form-interpreters
  (:require [parenoia.rewrite :as rewrite]
            [parenoia.style :as style]
            [rewrite-clj.zip :as z]
            [re-frame.core :refer [subscribe]]
            [clojure.string :as string]))

(defn number-to-letter [index]
  (string/lower-case (str (char (+ 65 index)))))

(defn letter-to-number [letter]
  (- (.charCodeAt (string/upper-case letter) 0)
     65))

(defn selected-zloc? [zloc]
 (let [this-pos (try (z/position zloc) (catch js/Error e "a"))
       selected-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])
       selected-pos (try (z/position selected-zloc) (catch js/Error e "b"))]
  (= this-pos selected-pos)))

(defn get-all-function-parameters-tag-recursion [types zloc]
  (let [new-tags (vec (conj types (z/tag zloc)))]
    (if (z/rightmost? zloc)
      new-tags
      (get-all-function-parameters-tag-recursion new-tags (z/right zloc)))))

(defn get-all-function-parameters-tag [zloc]
  (get-all-function-parameters-tag-recursion [] zloc))

(defn form-interpreter-iterator-recursion [zloc form-interpreter]
  (let [end-of-form?  (z/rightmost? zloc)]
    [:<>
     [form-interpreter zloc]
     (when-not end-of-form?
       [form-interpreter-iterator-recursion
        (z/right zloc)
        form-interpreter])]))

(defn form-interpreter-iterator [zloc form-interpreter mode]
  [:div {:style {:display :flex
                 :justify-content :flex-start
                 :flex-direction (case mode
                                   :horizontal :row
                                   :column)
                 :gap "5px"}}
   [form-interpreter-iterator-recursion zloc form-interpreter]])

(defn map-row-interpreter [zloc form-interpreter]
  (let [key          zloc
        value        (z/right zloc)
        end-of-map?  (z/rightmost? value)]
    [:<>
     [form-interpreter key]
     [form-interpreter value]
     (when-not end-of-map?
       [map-row-interpreter
        (z/right value) form-interpreter])]))

(defn map-interpreter  [zloc form-interpreter]
  (let [selected? (selected-zloc? zloc)]
   [:div {:class "map"
          :style {;:border-left    "4px double black"
                  ;:border-right   "4px double black"
                  :border-radius  "10px"
                  :background  (if selected? 
                                 (style/color [:selection :background-color]) 
                                 (style/color [:map :background-color]))
                  :color       (style/color [:map :text-color])

                  :box-shadow style/box-shadow
                  :padding "5px"
                  :display :grid
                  :width "fit-content"
                  :grid-template-columns "auto auto"
                  :grid-column-gap "15px"
                  :grid-row-gap    "5px"}}
    [map-row-interpreter (z/down zloc) form-interpreter]]))

(defn cond-interpreter  [zloc form-interpreter]
  (let [selected? (selected-zloc? zloc)
        cond-symbol (z/down zloc)
        next-symbol (z/right cond-symbol)]
   [:div {:class "cond"
          :style {;:border-left    "4px double black"
                  ;:border-right   "4px double black"
                  :border-radius  "10px"
                  :background  (if selected? 
                                 (style/color [:selection :background-color]) 
                                 (style/color [:function :background-color]))
                  :color       (style/color [:function :text-color])

                  :box-shadow style/box-shadow
                  :width "fit-content"
                  :padding "5px"}}
                  
    [:div {:style {:font-size "22px"}}
     [form-interpreter cond-symbol]]
    [:div {:style {:display :grid
                    :grid-template-columns "auto auto"
                    :grid-column-gap "15px"
                    :grid-row-gap    "5px"}}
     [map-row-interpreter next-symbol form-interpreter]]]))


(defn case-interpreter  [zloc form-interpreter]
  (let [selected? (selected-zloc? zloc)
        case-symbol (z/down zloc)
        case-condition (z/right case-symbol)
        next-symbol (z/right case-condition)]
   [:div {:class "case"
          :style {;:border-left    "4px double black"
                  ;:border-right   "4px double black"
                  :border-radius  "10px"
                  :background  (if selected? 
                                 (style/color [:selection :background-color]) 
                                 (style/color [:function :background-color]))
                  :color       (style/color [:function :text-color])

                  :box-shadow style/box-shadow
                  :width "fit-content"
                  :padding "5px"}}
                  
    [:div {:style {:font-size "22px" 
                   :display :flex
                   :padding-bottom "20px"}}
     [form-interpreter case-symbol]
     [form-interpreter case-condition]]
    [:div {:style {:display :grid
                    :grid-template-columns "auto auto"
                    :grid-column-gap "15px"
                    :grid-row-gap    "5px"}}
     [map-row-interpreter next-symbol form-interpreter]]]))


(defn if-interpreter  [zloc form-interpreter]
  (let [selected? (selected-zloc? zloc)
        if-symbol    (z/down zloc)
        if-condition (z/right if-symbol)
        if-true (z/right if-condition)
        if-false (z/right if-true)
        next-symbol (z/right if-false)]
   [:div {:class "if"
          :style {;:border-left    "4px double black"
                  ;:border-right   "4px double black"
                  :border-radius  "10px"
                  :background  (if selected? 
                                 (style/color [:selection :background-color]) 
                                 (style/color [:function :background-color]))
                  :color       (style/color [:function :text-color])

                  :box-shadow style/box-shadow
                  :width "fit-content"
                  :padding "5px"}}
    [:div {:style {:display :flex}}             
     [:div {:style {:font-size "22px"
                    :margin-bottom "10px"
                    :margin-right "10px"}} 
      [form-interpreter if-symbol]]
     [:div
      [:div 
         {:style {:background "#FFA500"
                   :padding "5px"
                   :margin-bottom "10px"
                   :border-radius "10px"}}
                
         [form-interpreter if-condition]]
      [:div 
        {:style {:background "#66FF66"
                   :border-radius "10px"
                   :padding "5px"
                   :margin-bottom "10px"}}
        [form-interpreter if-true]]
      [:div 
        {:style {:background "crimson"
                 :padding "5px"
                 :border-radius "10px"}}
        [form-interpreter if-false]]]]
    (when next-symbol [form-interpreter next-symbol])]))


(defn vector-interpreter  [zloc form-interpreter]
  [:div.vector {:style {:border-left    "1px solid black"
                        :border-right   "1px solid black"
                        :border-radius  "10px"
                        :background "lightgreen"
                        :padding "0 5"
                        :display :flex
                        :justify-content :center
                        :gap    "5px"
                        :color "#333"
                        :white-space :nowrap}}
   [form-interpreter-iterator (z/down zloc) form-interpreter :horizontal]])

(defn bigger-font-size [content]
 [:div {:style {:font-size "24px"}}
  content])

(defn defn-interpreter [zloc form-interpreter]
  (let [defn-symbol     (z/down zloc)
        function-name   (z/right defn-symbol)
        parameter-list  (z/right function-name)
        selected?       (selected-zloc? zloc)]
    [:div {:style {:border-radius  "10px"
                   :background (if selected?
                                    (style/color [:selection :background-color])
                                    (style/color [:defn  :background-color]))

                   :padding "10px"}}
     [:div  {:style {:display :flex 
                     :align-items :center}}
       [form-interpreter defn-symbol]
       [bigger-font-size [form-interpreter function-name]]
       [form-interpreter parameter-list]]
     [:div
      [:div {:style {:display :flex
                     :margin-bottom "5px"}}]
      [form-interpreter-iterator (z/right parameter-list) form-interpreter]]]))

(defn ns-interpreter [zloc form-interpreter selected?]
  (let [ns-symbol       (z/down zloc)
        ns-name         (z/right ns-symbol)] 
    [:div {:style {:border-radius  "10px"
                   :padding "10px"
                   :background (if selected?
                                    (style/color [:selection :background-color])
                                    (style/color [:defn  :background-color]))}}
     [:div  {:style {:display :flex 
                     :align-items :center}}
       [form-interpreter ns-symbol]
       [bigger-font-size [form-interpreter ns-name]]]
     [:div
      [:div {:style {:display :flex
                     :margin-bottom "5px"}}]
      [form-interpreter-iterator (z/right ns-name) form-interpreter]]]))


(defn def-interpreter [zloc form-interpreter]
  (let [def-symbol       (z/down zloc)
        def-name         (z/right def-symbol)
        selected?       (selected-zloc? zloc)]
    [:div {:style {:border-radius  "10px"
              
                   :padding "10px"
                   :background (if selected?
                                    (style/color [:selection :background-color])
                                    (style/color [:defn  :background-color]))}}
     [:div  {:style {:display :flex 
                     :align-items :center}}
       [form-interpreter def-symbol]
       [bigger-font-size [form-interpreter def-name]]]
     [:div
      [:div {:style {:display :flex
                     :margin-bottom "5px"}}]
      [form-interpreter-iterator (z/right def-name) form-interpreter]]]))

(defn let-vector-row-interpreter [zloc form-interpreter]
  (let [key          zloc
        value        (z/right zloc)
        end-of-map?  (z/rightmost? value)]
    [:<>
     [form-interpreter key]
     [form-interpreter value]
     (when-not end-of-map?
       [let-vector-row-interpreter
        (z/right value) form-interpreter])]))

(defn let-vector-interpreter  [zloc form-interpreter]
  (let [selected? (selected-zloc? zloc)]
   [:div {:style {:border-radius  "10px"
                  :padding "5px"
                  :display :grid
                  :align-items :grid
                  :background  (if selected? 
                                  (style/color [:selection :background-color]) 
                                  (style/color [:vector :background-color]))
                  :color (style/color [:let-vector :color])
                  :box-shadow style/box-shadow
                  :grid-template-columns "auto auto"
                  :grid-column-gap "15px"
                  :grid-row-gap    "5px"}}
    [let-vector-row-interpreter (z/down zloc) form-interpreter]]))

(def vertical-align-style
  {:display :flex
   :align-items :center})

(def function-container-style
  {:border-radius  "10px"
   :padding "0 5"
   :color "#333"
   :box-shadow style/box-shadow})

(def vector-container-style
  {:border-radius  "10px"
   :padding "0 5"
   :background "lightgreen"
   :color "#333"
   :box-shadow style/box-shadow})

(defn gather-tokens-and-separators [zloc coll]
  (let [whitespace? (z/whitespace-or-comment? zloc)
        new-line?-fn (fn [this-zloc] (= :newline (z/tag this-zloc)))
        newline?           (new-line?-fn zloc)
        double-newline?    (and
                             newline? (< 1 (z/length zloc)))
        new-coll (cond
                   newline?           (vec (conj coll :newline))
                   (not whitespace?)  (vec (conj coll :node))
                   :else              coll)
        new-zloc (z/right* zloc)]
    (if-not (z/rightmost? zloc)
      (gather-tokens-and-separators new-zloc new-coll)
      new-coll)))

(defn add-to-last-vec-in-vec [coll char]
  (let [new-last (vec (conj (last coll) char))
        new-coll (vec (concat (butlast coll) [new-last]))]
    new-coll))

(defn calc-offset [coll row-index]
  (if (= row-index 0)
    0
    (- (count (reduce str (take row-index coll)))
      row-index)))

(defn areas-coll->areas-template [coll]
  (apply str
    (map
      (fn [row]
        (str "\""
          (apply str (clojure.string/join " " row))
          "\""))
      coll)))

(defn get-areas-template-second-step [coll]
  (let [row-count   (count coll)
        col-count   (count (first coll))
        blank-areas (mapv
                      (fn [row-index]
                        (mapv
                          (fn [col-index] ".")
                          (range col-count)))
                      (range row-count))]
    (vec (map-indexed
           (fn [row-index row]
             (vec (map-indexed
                    (fn [col-index col]
                      (get-in coll [row-index col-index]
                        "."))

                    row)))
           blank-areas))))

(defn generate-vector-of-points [number]
  (mapv (fn [a] ".") (range number)))

(defn fill-space-with-points [coll length]
  (let [point-length (- length (count coll))
        point-vec    (generate-vector-of-points point-length)]
    (vec (concat coll point-vec))))

(defn keep-one-new-line-in-a-row [coll]
  (reduce
    (fn [result this]
      (let [new-result (vec (conj result this))]
        (if (= this (last result) :newline)
          result
          new-result)))
    []
    coll))

(defn count-grid-width-with-token-coll [coll]
  (let [how-many-lines  (count (filter (fn [a] (= a :newline)) coll))
        how-many-tokens (count (filter (fn [a] (= a :node)) coll))]
    (- how-many-tokens how-many-lines)))

(defn count-points-in-vector [coll]
  (count (filter (fn [a] (= "." a))
           coll)))

(defn modify-last-item-of-vec [coll the-fn]
  (let [last-item (last coll)
        butlast-item (vec (butlast coll))
        modified-last-item (the-fn last-item)]
    (vec (conj butlast-item modified-last-item))))

(defn get-areas-template-first-step [token-newline-coll]
  (let [filtered-token-newline-coll (keep-one-new-line-in-a-row token-newline-coll)
        grid-width  (count-grid-width-with-token-coll filtered-token-newline-coll)]
    (:result
     (reduce
       (fn [{:keys [index result]
             :as x} this]
         (assoc x
           :index (cond
                    (= this :node)     (inc index)
                    (= this :newline)  index)
           :result (cond
                     (= this :node)    (add-to-last-vec-in-vec
                                         result (number-to-letter index))
                     (= this :newline) (vec
                                         (conj
                                           (modify-last-item-of-vec result
                                             (fn [coll] (fill-space-with-points coll grid-width)))
                                           (generate-vector-of-points (dec (count (last result)))))))))
       {:index  0
        :result [[]]}
       filtered-token-newline-coll))))

(defn generate-form-areas [zloc]

  (let [nodes-and-separators (gather-tokens-and-separators zloc [])]
    (->
      nodes-and-separators
      get-areas-template-first-step
      get-areas-template-second-step)))

(defn function-grid-style [areas-vec]
  (let [areas-str  (areas-coll->areas-template areas-vec)
        grid-template-columns (clojure.string/join " "
                                (map (fn [a] "auto")
                                  (first areas-vec)))]
    {:display :grid
     :width :fit-content
     :grid-template-columns grid-template-columns
     :grid-template-areas areas-str}))

(defn function-child-interpreter [zloc index form-interpreter]
  [:<>
   [:div {:style {:grid-area (number-to-letter index)
                  :display :flex
                  :justify-content :flex-start
                  :border-radius "10px"
                  :align-items :center}}
    [form-interpreter zloc]]
   (if-not (z/rightmost? zloc)
     [function-child-interpreter (z/right zloc) (inc index) form-interpreter])])

(defn function-interpreter  [zloc form-interpreter selected?]
  (let [function-name (z/down zloc)
        no-parameters? (z/rightmost? function-name)
        function-first-parameter  (z/right function-name)
        function-parameters (z/right function-first-parameter)
        areas-vec  (generate-form-areas function-name)]
    [:<>
   ;(str areas-vec)
   ;(str (gather-tokens-and-separators function-name []))
   ;(str (keep-one-new-line-in-a-row (gather-tokens-and-separators function-name [])))
   ;(str (gather-tokens-and-separators function-name []))
     [:div {:style (merge
                     function-container-style
                     {:background  (if selected? 
                                     (style/color [:selection :background-color]) 
                                     (if (z/vector? zloc)
                                       (style/color [:vector :background-color])
                                       (style/color [:function :background-color])))}
                     
                     
                     (function-grid-style areas-vec))}

      [function-child-interpreter
       function-name 0 form-interpreter]]]))


(defn reader-macro-interpreter  [zloc form-interpreter]
  (let [reader-macro (z/down zloc)
        selected? (selected-zloc? zloc)]
    [:<>
     [:div {:style (merge
                     function-container-style
                     {:display :flex
                      :align-items :center
                      :background  (if selected? 
                                     (style/color [:selection :background-color]) 
                                     (style/color [:reader-macro :background-color]))})}
      [:div {:style {:padding "10px"
                     :color (style/color [:reader-macro :text-color])}}
           "#"]
      [function-child-interpreter
       reader-macro 
       0 form-interpreter]]]))

(defn deref-interpreter  [zloc form-interpreter]
  (let [reader-macro (z/down zloc)
        selected? (selected-zloc? zloc)]
    [:<>
     [:div {:style (merge
                     function-container-style
                     {:display :flex
                      :align-items :center
                      :background  (if selected? 
                                     (style/color [:selection :background-color]) 
                                     (style/color [:function :background-color]))})}
      [:div {:style {:padding "10px"
                     :color (style/color [:function :text-color])}}
           "@"]
      [function-child-interpreter
       reader-macro 
       0 form-interpreter]]]))


(defn meta-interpreter  [zloc form-interpreter]
  (let [reader-macro (z/down zloc)
        selected? (selected-zloc? zloc)]
    [:<>
     [:div {:style (merge
                     function-container-style
                     {:display :flex
                      :align-items :center
                      :background  (if selected? 
                                     (style/color [:selection :background-color]) 
                                     (style/color [:meta :background-color]))})}
      [:div {:style {:padding "10px"
                     :color (style/color [:meta :text-color])}}
           "^"]
      [:div {:style {:display :flex}}
       [function-child-interpreter
        reader-macro 
        0 form-interpreter]]]]))

(defn anonym-fn-interpreter  [zloc form-interpreter]
  (let [reader-macro (z/down zloc)
        selected? (selected-zloc? zloc)]
    [:<>
     [:div {:style (merge
                     function-container-style
                     {:display :flex
                      :align-items :center
                      :background  (if selected? 
                                     (style/color [:selection :background-color]) 
                                     (style/color [:function :background-color]))})}
      [:div {:style {:padding "10px"
                     :color (style/color [:function :text-color])}}
           "#"]
      [:div {:style {:display :flex}}
       [function-child-interpreter
        reader-macro 
        0 form-interpreter]]]]))


