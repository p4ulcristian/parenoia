(ns parenoia.namespace-graph
   (:require ["cytoscape" :as cytoscape]
             ["vis" :as vis]
             ["react" :as react]
             [parenoia.refactor :as refactor]
             [re-frame.core :refer [subscribe]]))

(defn generate-node [id]
  {:id id :label id})

(defn generate-nodes [ids]
  (mapv generate-node ids))

(defn generate-edge [source-id target-id]
  {:from source-id 
   :to target-id})

(defn generate-edges [ids target-id]
  (mapv (fn [source-id] (generate-edge source-id target-id)) 
        ids))

(defn view []
  (let [ref (react/useRef)
        files (map second @(subscribe [:db/get [:parenoia :project]]))
        all-nodes (mapv (fn [a] (generate-node a)) 
                        (sort (map refactor/get-ns files)))
        all-edges (vec (reduce concat 
                        (mapv (fn [file]  
                               (let [file-ns (refactor/get-ns file)
                                     required-by-this (refactor/get-requires-by-namespace file)
                                     cyto-edges  (generate-edges required-by-this (str file-ns))]
                                   (vec cyto-edges)))
                             files)))
        all-elements (vec (concat all-nodes all-edges))]          
   (react/useEffect 
     (fn [] 
      (vis/Network. (.-current ref) #js {:nodes (vis/DataSet. (clj->js all-nodes)) 
                                         :edges (vis/DataSet. (clj->js all-edges))} 
                                    #js {})           
      (fn [])
     #js []))
   [:div {:ref ref
          :style {:height "600px"
                  :width "100%"}}]))
    ;(map (fn [a] [:div (str a)]) all-elements)])) 


;; (def edges (vis/DataSet. (clj->js [{:from 1 :to 3}
;;                                    {:from 1 :to 2}
;;                                    {:from 2 :to 4}
;;                                    {:from 2 :to 5}
;;                                    {:from 3 :to 3}])))


;; (def nodes (vis/DataSet. (clj->js [{:id 1 :label "node 1"}
;;                                    {:id 2 :label "node 2"}
;;                                    {:id 3 :label "node 3"}
;;                                    {:id 4 :label "node 4"}
;;                                    {:id 5 :label "node 5"}])))

;; (def data (clj->js {:nodes nodes 
;;                     :edges edges}))

;; (def options (clj->js {}))

;; (defn view [zloc]
;;    (let [ref (react/useRef)]
;;      (react/useEffect (fn []
;;                         (vis/Network. (.-current ref) data options)
;;                        (fn []))
;;                       #js []) 
;;      [:div 
;;       {:style {:height 600
;;                :width "100%"}
;;         :ref ref}
;;       "hello there"]))
    