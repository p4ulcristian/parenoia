(ns parenoia.namespace-graph
   (:require ["cytoscape" :as cytoscape]
             ["vis" :as vis]
             ["react" :as react]
             ["react-graph-vis" :default Graph]
             [parenoia.refactor :as refactor]
             [re-frame.core :refer [dispatch subscribe]]))

(defn generate-node [file-path id]
  {:id id :label id :file-path file-path
   :margin {:top 20, :right 20, :bottom 20, :left 20 }})

(defn generate-edge [source-id target-id]
  {:from source-id 
   :to target-id
   :arrows "to"})

(defn generate-edges [ids target-id]
  (mapv (fn [source-id] (generate-edge source-id target-id)) 
        ids))

(def options 
 {:physics {:enabled true 
            :hierarchicalRepulsion {:nodeDistance 500}}
  :layout {:hierarchical {:enabled true 
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
              (dispatch 
               [:db/set [:parenoia :selected :file-path] 
                (get-node-with-id nodes node-clicked)])))})

(defn vis-js-component [nodes edges]
 (let [ref (react/useRef)
       graph {:nodes nodes
              :edges edges}]
  [:div {:ref ref 
          :style {:height "100vh" 
                  :width "100vw"
                  :z-index 1000
                  :position :fixed
                  
                  :right 0 
                  :top 0
                  :background "rgba(0,0,0,0.6)"
                  :border-radius "10px"}}
    [:> Graph {:graph graph :options options :events (events nodes)}]]))   

(defn view []
  (let [ref (react/useRef)
        project-map? @(subscribe [:db/get [:parenoia :project-map?]])
       
        files @(subscribe [:db/get [:parenoia :project]])
        all-nodes (mapv (fn [[file-path file]] (generate-node  
                                                  file-path
                                                  (refactor/get-ns file)))  
                        files)
        all-edges (vec (reduce concat 
                        (mapv (fn [file]  
                               (let [file-ns (refactor/get-ns file)
                                     required-by-this (refactor/get-requires-by-namespace file)
                                     cyto-edges  (generate-edges required-by-this (str file-ns))]
                                   (vec cyto-edges)))
                             (map second files))))
        all-elements (vec (concat all-nodes all-edges))]          
   [:div {:style {:display (if project-map? :block :none)}}
    [vis-js-component all-nodes all-edges]]))
