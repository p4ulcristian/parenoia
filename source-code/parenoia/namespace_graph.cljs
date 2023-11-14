(ns parenoia.namespace-graph
   (:require ["cytoscape" :as cytoscape]
             ["react" :as react]
             [parenoia.refactor :as refactor]
             [re-frame.core :refer [subscribe]]))

(def cyto-style 
 (clj->js 
  [{:selector "node[id]"
    :style {:background-color "#666"
            :label            "data(id)"
            :font-size 3}}
   {:selector "edge"
    :style {:width 1,
            :line-color "orange"
            :target-arrow-color "blue"
            :target-arrow-shape "triangle"
            :curve-style "bezier"}}]))


(defn generate-cyto-node [id]
  {:data {:id id}})

(defn generate-cyto-nodes [ids]
  (mapv generate-cyto-node ids))

(defn generate-cyto-edge [source-id target-id]
  {:data {:id (str source-id " -> " target-id)
          :source source-id 
          :target target-id}})

(defn generate-cyto-edges [ids target-id]
  (mapv (fn [source-id] (generate-cyto-edge source-id target-id)) 
        ids))

(defn view [zloc]
  (let [cyto-ref (react/useRef)
        this-ns (refactor/get-ns zloc)
        files (map second @(subscribe [:db/get [:parenoia :project]]))
        all-nodes (mapv (fn [a] (generate-cyto-node a)) 
                        (sort (map refactor/get-ns files)))
        all-edges (vec (reduce concat 
                        (mapv (fn [file]  
                               (let [file-ns (refactor/get-ns file)
                                     required-by-this (refactor/get-requires-by-namespace file)
                                     cyto-edges (generate-cyto-edges required-by-this (str file-ns))]
                                   (vec cyto-edges)))
                             files)))
        all-elements (vec (concat all-nodes all-edges))]          
   (react/useEffect 
     (fn []
      (cytoscape (clj->js {:container (.-current cyto-ref)
                           :style    cyto-style
                           :elements all-elements}))
                                     
    
                            
      (fn [])
      #js []))
   [:div {:ref cyto-ref
          :style {:height "100vh"
                  :width "100%"}}]))
    ;(map (fn [a] [:div (str a)]) all-elements)]))      
    