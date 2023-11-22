(ns parenoia.namespace-graph
  (:require ["cytoscape" :as cytoscape]
            ["react" :as react]
            ["react-graph-vis" :default Graph]
            ["vis" :as vis]
            [parenoia.refactor :as refactor]
            [re-frame.core :refer [dispatch subscribe]]))



(def depth-one-color   "lightgreen")
(def depth-two-color   "pink")
(def depth-three-color "orange")
(def depth-four-color  "lightblue")
(def depth-five-color  "blue")

(defn split-ns [ns-name]
  (clojure.string/split  ns-name #"\."))

(defn ns-name->ns-depth [ns-name]
  (count (split-ns ns-name)))

(def wizard-color "pink")
(def backend-color "orange")
(def extensions-color "lightgreen")

(defn ns-name->first-name [ns-name]
   (first (split-ns ns-name)))


(defn ns-name->ns-color [ns-name]
  (case (ns-name->first-name ns-name)
    "wizard" wizard-color 
    "backend" backend-color 
    "extensions" extensions-color
    "lightblue"))

(defn generate-node [file-path id]
  {:id id
   :label (str (clojure.string/replace (str id) #"\." " |"))
   :widthConstraint 200
   :shape "box"
   :group (ns-name->first-name ns-name)
   :file-path file-path
   :font {:size 18}
   :margin 10
   :color {:background (ns-name->ns-color id)
           :border "lightgreen"
           :highlight  {:background "yellow"}
           :hover      {:background "yellow"}}})

(defn generate-edge [source-id target-id]
  {:from source-id
   :to target-id
   :arrows "to"
   ;:smooth true
   :color {:color (ns-name->ns-color source-id)
           :highlight "yellow"}})
           ;:highlight  {:background "red"}}})
  

(defn generate-edges [ids target-id]
  (mapv (fn [source-id] (generate-edge source-id target-id))
    ids))

(def options
  {:physics {:enabled true
             :hierarchicalRepulsion {:nodeDistance 500}}
   :layout {:hierarchical {:enabled true
                           :shakeTowards "roots"
                           :direction "UD"
                           :levelSeparation 250
                           :sortMethod "directed"}}

   :edges {:color "lightgreen"}
   :interaction {:navigationButtons true}})

(defn get-node-with-id [nodes id]
  (:file-path (first (filter
                       (fn [node] (= id (str (:id node))))
                       nodes))))

(defn events  [nodes]
  {:select (fn [event]
             (let [node-clicked (first (get (js->clj event) "nodes"))]
               (if node-clicked
                 (dispatch
                   [:db/set [:parenoia :selected :file-path]
                    (get-node-with-id nodes node-clicked)]))))})

(defn vis-js-component [nodes edges]
  (let [[vis set-vis]   (react/useState nil)
        graph {:nodes nodes
               :edges edges}]
    [:div {:style {:height "100vh"
                   :width "100vw"
                   :border-radius "10px"}}
     [:> Graph {:vis (fn [e] (set-vis e))
                :graph graph
                :options options
                :events (events nodes)}]]))

(defn view-wrapper [content]
  (let [project-map? @(subscribe [:db/get [:parenoia :project-map?]])]
    [:div {:style {:position :fixed
                   :right 0
                   :top 0
                   :background "rgba(0,0,0,0.6)"
                   :backdrop-filter (if project-map? "blur(10px)" "none")
                   :filter (if project-map? "none" "blur(10px)")
                   :z-index (if project-map? 15 7)}}
     
     content]))

(defn get-all-edges [files]
  (vec (reduce concat
         (mapv (fn [file]
                 (let [file-ns (refactor/get-ns file)
                       required-by-this (refactor/get-requires-by-namespace file)
                       cyto-edges  (generate-edges required-by-this (str file-ns))]
                   (vec cyto-edges)))
           (map second files)))))

(defn get-all-nodes [files]
  (mapv (fn [[file-path file]] (generate-node file-path (refactor/get-ns file)))
    files))


(defn filter-nodes-by-string [nodes filter-string] 
     (filter 
       (fn [{:keys [id]}] (clojure.string/includes? (str id) filter-string))
       nodes))                      

(defn filter-edges-by-string [edges filter-string]
 (filter 
       (fn [{:keys [from to]}] 
         (and
          (clojure.string/includes? (str from) filter-string)
          (clojure.string/includes? (str to)   filter-string)))
       edges))
  

(defn view []
  (let [ref (react/useRef)
        files @(subscribe [:db/get [:parenoia :project-last-saved]])
        [nodes set-nodes] (react/useState [])
        [edges set-edges] (react/useState [])
        [local-string set-local-string]   (react/useState "")
        [filter-string set-filter-string] (react/useState "")]
    (react/useEffect
      (fn []
        (set-nodes (filter-nodes-by-string (get-all-nodes files) filter-string))
        (set-edges (filter-edges-by-string (get-all-edges files) filter-string))
        (fn []))
      #js [files filter-string])

    [view-wrapper
     [:<>
      [:input {:value local-string
               :on-change #(set-local-string (-> % .-target .-value))
               :on-blur   #(set-filter-string (-> % .-target .-value))}] 
      [vis-js-component nodes edges]]]))
