(ns parenoia.namespace-graph
   (:require ["cytoscape" :as cytoscape]
             ["vis" :as vis]
             ["react" :as react]
             ["react-graph-vis" :default Graph]
             [parenoia.refactor :as refactor]
             [re-frame.core :refer [dispatch subscribe]]))

(defn generate-node [file-path id]
  {:id id :label id :file-path file-path})

(defn generate-edge [source-id target-id]
  {:from source-id 
   :to target-id
   :arrows "to"})

(defn generate-edges [ids target-id]
  (mapv (fn [source-id] (generate-edge source-id target-id)) 
        ids))

(def options 
 {:physics {:enabled true 
            :hierarchicalRepulsion {:nodeDistance 100}}
  :layout {:hierarchical {:enabled true 
                          :direction "LR"
                          :levelSeparation 250
                          :sortMethod "directed"}}
                          
  :edges {:color "lightgreen"}})
  
 

(defn vis-js-component [nodes edges]
 (let [ref (react/useRef)
       graph {:nodes nodes
              :edges edges}]
  [:div {:ref ref 
          :style {:height "500px" 
                  :width "500px"
                  :z-index 1000
                  :position :fixed 
                  :right 0 
                  :top 0
                  :background "rgba(0,0,0,0.6)"
                  :border-radius "10px"}}
    [:> Graph {:graph graph :options options}]]))   

(defn view []
  (let [ref (react/useRef)
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
   
   [vis-js-component all-nodes all-edges]))
