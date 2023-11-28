(ns parenoia.events
  (:require 
    [re-frame.core :refer [reg-event-db dispatch reg-sub]]
    [parenoia.undo :refer [undoable]]
    [cljs.reader :refer [read-string]]
    [parenoia.rewrite :as rewrite] 
    [ajax.core     :refer [GET POST]]
    [parenoia.refactor :as refactor]
    [rewrite-clj.zip :as z]))


(defn find-top-form-recursion [last-zloc zloc]
 (let [up-loc (z/up zloc)]
  (cond 
    (nil? up-loc) last-zloc
    :else (recur zloc up-loc))))
 

(defn find-top-form [zloc]
 (find-top-form-recursion nil zloc))

(reg-event-db
 :db/set
 (fn [db [_ path value]]
   (assoc-in db path value)))

(reg-event-db
 :db/unset
 (fn [db [_ path]]
   (let [parent-path (vec (drop-last 1 path))
         parent (get-in db parent-path)
         child-key (last path)]
     (assoc-in db parent-path (dissoc parent child-key)))))

(reg-sub
 :db/get
 (fn [db [_ path]]
   (get-in db path)))

(reg-event-db
 :db/merge
 (fn [db [_ path value-to-merge]]
   (let [value       (get-in db path)
         new-value   (merge value value-to-merge)]
     (assoc-in db path new-value))))

(defn process-file-to-zloc [all-files]
  (reduce merge
          (map
           (fn [[file-name file-string]]
             (assoc {} file-name
                    (rewrite/zloc-from-file-string file-string)))
           all-files)))


(defn get-project-structure []
  (GET "/files"
    {:with-credentials false
      :handler          (fn [e]
                         (let [processed-string (try (read-string e)
                                                     (catch js/Error e false))]
                          (dispatch
                           [:db/set [:parenoia :project]
                            (process-file-to-zloc 
                             processed-string)])
                          (dispatch [:db/set [:parenoia :project-last-saved]
                                            (process-file-to-zloc 
                                             processed-string)])))
     :error-handler    (fn [e] (println "watdsakopdsakdl;as"))}))


(reg-event-db
 :parenoia/get-files
 [(undoable)]
 (fn [db [_ file zloc]]
   (get-project-structure)
   db))


(reg-event-db 
 :parenoia/go-to!
 []
 (fn [db [_ path pos]]
   (let [file-zloc (get-in db [:parenoia :project path])
         selected-zloc (z/find-last-by-pos file-zloc pos)]
      (println path pos)
      (.setTimeout js/window #(dispatch [:db/set [:parenoia :selected-zloc]  selected-zloc])
                  150)
      (-> db
       (assoc-in [:parenoia :selected :file-path] path)))))
   


(defn get-text-context [text index]
 (let [context-radius 200
       a (max 0 (- index context-radius))
       b (min (count text) (+ index context-radius))]
  (subs text a b)))


(defn get-text-position [text index]
   (let [text-till-index (subs text 0 index)
         splitted-lines  (clojure.string/split text-till-index #"\n")
         last-line       (last splitted-lines)]
    [(count splitted-lines)
     (inc (count last-line))]))
     
(reg-event-db
 :parenoia/global-search
 (fn [db [_ term]]
   (let [project (-> db :parenoia :project)
         filtered-indexes  (remove false? (map (fn [[path file]]
                                                 (clojure.string/index-of (z/root-string file) term))
                                               project))
         filtered-namespaces (filter
                               (fn [[path file]]
                                 (clojure.string/includes? (z/root-string file) term))
                               project)
         filtered-projects (map (fn [[path zloc]]
                                   (let [root-string (z/root-string zloc)
                                         search-index (clojure.string/index-of root-string term)
                                         pos          (get-text-position root-string search-index)
                                         pos-zloc     (z/find-last-by-pos zloc pos)
                                         top-form     (z/string (find-top-form pos-zloc))] 
                                    {:namespace (refactor/get-ns zloc)
                                     :file-path path
                                     :position  pos
                                     :content   top-form})) 
                                filtered-namespaces)]
                                                  
                             
    (println "Searching for " term)
    (if (= "" term) 
      (assoc-in db  [:parenoia :search-results] [])
      (assoc-in db  [:parenoia :search-results] filtered-projects)))))
      

(reg-event-db
 :parenoia/set-file!
 [(undoable)]
 (fn [db [_ file zloc]]
   (let [file-name (-> db :parenoia :selected :file-path)]
    (-> db 
      (assoc-in [:parenoia :project file-name] file)
      (assoc-in [:parenoia :selected-zloc] zloc)))))


(defn uri->path [uri]
 (apply str (drop 7 uri)))

(reg-event-db 
  :parenoia/set-selected-file-by-uri
  [(undoable)]
  (fn [db [_ uri row col]]
   (let [path (uri->path uri)
         zloc (get-in db [:parenoia :project path])
         new-zloc (z/find-last-by-pos zloc [row col])]
    (-> db 
       (assoc-in [:parenoia :selected :file-path] path)
       (assoc-in [:parenoia :selected-zloc]       new-zloc)))))

(reg-event-db
 :parenoia/save!
 []
 (fn [db [_]]
   (let [file-name (-> db :parenoia :selected :file-path)
         file      (z/root-string (get-in db [:parenoia :project file-name]))]           
    (POST "/file"
     {:params {:path file-name 
               :content file}
      ;; :response-format    :text
      ;; :format    :text
       :handler          (fn [e]
                           (println "Save: " e)
                           (dispatch [:parenoia/get-kondo-lints (get-in db [:parenoia :project file-name])]))
      :error-handler    (fn [e] (.log js/console e))}))

   db))


(reg-event-db
 :parenoia/get-variable-info
 []
 (fn [db [_ zloc]]
   (let [file-name (-> db :parenoia :selected :file-path)
         file      (z/root-string (get-in db [:parenoia :project file-name]))]           
    (POST "/variable-info"
     {:params {:file-path file-name 
               :position (z/position zloc)}
      ;; :response-format    :text
      ;; :format    :text
       :handler          (fn [e]
                           (dispatch [:db/set [:parenoia :variable-info] (read-string e)]))
      :error-handler    (fn [e] (.log js/console e))}))

   db))




(defn root? [loc]
  (identical? :forms (z/tag loc)))

(defn top? [loc]
  (root? (z/up loc)))

(defn to-top
  "Returns the loc for the top-level form above the loc, or the loc itself if it
  is top-level, or nil if the loc is at the `:forms` node."
  [loc]
  (z/find loc z/up top?))


(reg-event-db
 :parenoia/get-form-info
 [] 
 (fn [db [_ zloc]]
   
   (let [file-name (-> db :parenoia :selected :file-path)
         file      (z/root-string (get-in db [:parenoia :project file-name]))]           
    (POST "/form-info"
     {:params {:file-path file-name 
               :position (z/position zloc)}
       :handler          (fn [e]
                           (dispatch [:db/set [:parenoia :form-info] (read-string e)]))
      :error-handler    (fn [e] (.log js/console e))}))

   db))

(reg-event-db
 :parenoia/get-completion
 [] 
 (fn [db [_ zloc]]
   (let [file-name (-> db :parenoia :selected :file-path)
         file      (z/root-string (get-in db [:parenoia :project file-name]))]           
    (POST "/completion"
     {:params {:file-path file-name 
               :position (let [[r c] (z/position zloc)] [r (inc c)])}
       :handler          (fn [e]
                           (dispatch [:db/set [:parenoia :completion] (read-string e)]))
      :error-handler    (fn [e] (.log js/console e))}))
   db))


(reg-event-db
 :parenoia/get-kondo-lints
 [] 
 (fn [db [_ zloc]]
   (let [file-name (-> db :parenoia :selected :file-path)
         file      (z/root-string (get-in db [:parenoia :project file-name]))]           
    (POST "/kondo-lints"
     {:params {:file-path file-name 
               :position (let [[r c] (z/position zloc)] [r (inc c)])}
       :handler          (fn [e]
                           (dispatch [:db/set [:parenoia :kondo-lints] (read-string e)]))
      :error-handler    (fn [e] (.log js/console e))}))

   db))

(reg-event-db
 :parenoia/get-definition
 [] 
 (fn [db [_ zloc]]
   (let [file-name (-> db :parenoia :selected :file-path)
         file      (z/root-string (get-in db [:parenoia :project file-name]))]           
    (POST "/get-definition"
     {:params {:file-path file-name 
               :position (let [[r c] (z/position zloc)] [r (inc c)])}
       :handler          (fn [e]
                           (dispatch [:db/set [:parenoia :definition] (read-string e)]))
      :error-handler    (fn [e] (.log js/console e))}))

   db))




(reg-event-db
 :parenoia/rename!
 [] 
 (fn [db [_ zloc from to]]
   (let [file-name  (-> db :parenoia :selected :file-path)
         file-zloc  (get-in db [:parenoia :project file-name])]
    (POST "/rename"
      {:params {:from (str (refactor/get-ns file-zloc) "/" from)
                :to   (str (refactor/get-ns file-zloc) "/" to)}
        :handler          (fn [e] (println "Successful " e))
                          
       :error-handler    (fn [e] (.log js/console e))}))

   db))

(reg-event-db
 :parenoia/remove-pin!
 [] 
 (fn [db [_ pin]]
   (println "Remove pin " pin)
   (let [pins (get-in db [:parenoia :pins] [])
         new-pins (vec (remove (fn [a] (= a pin)) 
                               pins))]
     (assoc-in db [:parenoia :pins] new-pins)))) 

(reg-event-db
 :parenoia/select-pin!
 [] 
 (fn [db [_ {:keys [position file-path]}]]
   (println "Selecting pin " position file-path)
   (let [file-zloc (get-in db [:parenoia :project file-path])
         zloc (z/find-last-by-pos file-zloc position)]
    (-> db
     (assoc-in [:parenoia :selected :file-path] file-path)
     (assoc-in [:parenoia :selected-zloc] zloc))))) 

(reg-event-db
 :parenoia/add-pin!
 [] 
 (fn [db [_ zloc]]
   (let [file-name  (-> db :parenoia :selected :file-path)
         file-zloc  (get-in db [:parenoia :project file-name])
         pins (get-in db [:parenoia :pins] [])
         top-form (find-top-form zloc)
         function-name (z/string (z/right (z/down top-form)))
         namespaced-function-name (str (refactor/get-ns file-zloc) "/" function-name)]
    (assoc-in db [:parenoia :pins] (vec (set (conj pins
                                              {:position (z/position zloc)
                                               :file-path file-name
                                               :function-name namespaced-function-name}))))))) 
                                    

   
