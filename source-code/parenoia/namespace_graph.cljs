(ns parenoia.namespace-graph
  (:require ["cytoscape" :as cytoscape]
            ["react" :as react]
            ["react-graph-vis" :default Graph]
            ["vis" :as vis]
            [parenoia.refactor :as refactor]
            [re-frame.core :refer [dispatch subscribe]]))

(defn generate-node [file-path id]
  {:id id
   :label id
   :file-path file-path
   :margin {:top 40,
            :right 40,
            :bottom 40,
            :left 40}
   :color {:background "lightgreen",
           :border "lightgreen"
           :hightlight {:color "red"}}})

(defn generate-edge [source-id target-id]
  {:from source-id
   :to target-id
   :arrows "to"
   :color {:color "lightblue"
           :hightlight "#848484"}})

(defn generate-edges [ids target-id]
  (mapv (fn [source-id] (generate-edge source-id target-id))
    ids))

(def options
  {:physics {:enabled true
             :hierarchicalRepulsion {:nodeDistance 300}}
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
  (let [ref (react/useRef)
        graph {:nodes nodes
               :edges edges}]
    [:div {:ref ref
           :style {:height "100vh"
                   :width "100vw"
                   :border-radius "10px"}}
     [:> Graph {:graph graph
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

(defn view []
  (let [ref (react/useRef)
        files @(subscribe [:db/get [:parenoia :project-last-saved]])
        [nodes set-nodes] (react/useState [])
        [edges set-edges] (react/useState [])]
    (react/useEffect
      (fn []
        (set-nodes (get-all-nodes files))
        (set-edges (get-all-edges files))
        (fn []))
      #js [files])

    [view-wrapper
     [vis-js-component nodes edges]]))
