(ns parenoia.events
  (:require 
    [re-frame.core :refer [reg-event-db dispatch reg-sub]]
    [parenoia.undo :refer [undoable]]
    [cljs.reader :refer [read-string]]
    [parenoia.rewrite :as rewrite] 
    [ajax.core     :refer [GET POST]]
            
    [rewrite-clj.zip :as z]))


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
                          (dispatch [:db/set [:parenoia :project-map?] true])
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
 :parenoia/set-file!
 [(undoable)]
 (fn [db [_ file zloc]]
   (let [file-name (-> db :parenoia :selected :file-path)]
    (-> db 
      (assoc-in [:parenoia :project file-name] file)
      (assoc-in [:parenoia :selected-zloc] zloc)))))


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
                           (println "Save: " e))
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


(defn find-top-form-recursion [last-zloc zloc]
 (let [up-loc (z/up zloc)]
  (cond 
    (nil? up-loc) last-zloc
    :else (recur zloc up-loc))))
 

(defn find-top-form [zloc]
 (find-top-form-recursion nil zloc))

(defn find-op
  [zloc]
  (loop [op-loc (or (and (= :list (z/tag zloc))
                         (z/down zloc))
                    (z/leftmost zloc))]
    (let [up-loc (z/up op-loc)]
      (cond
        (nil? up-loc) nil
        (= :list (z/tag up-loc)) op-loc
        :else (recur (z/leftmost up-loc))))))

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
   (println "Hello " (z/string (to-top zloc)))
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
 :parenoia/reanalyze-project!
 [] 
 (fn [db [_ zloc]]
   (println "Hello " (z/string (to-top zloc)))
   (let [file-name (-> db :parenoia :selected :file-path)
         file      (z/root-string (get-in db [:parenoia :project file-name]))]           
    (POST "/reanalyze-project"
     {:params {}
       :handler          (fn [e] (println "Successful " e))
                          
      :error-handler    (fn [e] (.log js/console e))}))

   db))
