(ns parenoia.undo
  (:require-macros [reagent.ratom  :refer [reaction]])
  (:require
   [akiroz.re-frame.storage :refer [persist-db]]
   [reagent.core        :as     reagent]
   [rewrite-clj.zip :as z]
   [re-frame.core       :as     re-frame]
   [re-frame.db         :refer  [app-db]]))


;; -- Configuration ----------------------------------------------------------

(def config (atom {:max-undos       50 ;; Maximum number of undo states maintained
                   :harvest-fn      deref
                   :reinstate-fn    reset!}))

(defn undo-config!
  "Set configuration parameters for library.
  Should be called on app startup."
  [new-config]
  (if-let [unknown-keys (seq (clojure.set/difference
                              (-> new-config keys set)
                              (-> @config keys set)))]
    (re-frame/console :error "re-frame-undo: undo-config! called within unknown keys: " unknown-keys)
    (swap! config merge new-config)))


(defn max-undos
  []
  (:max-undos @config))



;; -- State history ----------------------------------------------------------

(def undo-list "A list of history states"
  (reagent/atom {}))
(def redo-list "A list of future states, caused by undoing"
  (reagent/atom {}))

(defn clear-undos!
  [page-id]
  (swap! undo-list assoc page-id []))

(defn clear-redos!
  [page-id]
  (swap! redo-list assoc page-id []))


(defn store-now!
  "Stores the value currently in app-db, so the user can later undo"
  [db]
  (let [page-id             (-> db :parenoia :selected-file)
        this-undo-list      (get @undo-list page-id [])
        this-editor-state   {:date      (.now js/Date)
                             :selection (get-in db [:parenoia :selected-zloc]) 
                             :page      (get-in db [:parenoia :project page-id])}
        new-undo-list       (vec (concat this-undo-list [this-editor-state]))]
    (clear-redos! page-id)
    (swap! undo-list assoc page-id new-undo-list)))


(defn undos?
  "Returns true if undos exist, false otherwise"
  [page-id]
  (not (empty? (get @undo-list page-id))))

(defn redos?
  "Returns true if redos exist, false otherwise"
  [page-id]
  (not (empty? (get @redo-list page-id))))

;; -- subscriptions  -----------------------------------------------------------------------------

(re-frame/reg-sub
 :parenoia/undos?                   ;;  usage:  (subscribe [:undos?])
 (fn [db _]
   (let [page-id   (-> db :parenoia :selected-file)]
     (undos? page-id))))

(re-frame/reg-sub
 :parenoia/redos?
 (fn [db _]
   (let [page-id   (-> db :parenoia :selected-file)]
     (redos? page-id))))

;; -- event handlers  ----------------------------------------------------------------------------

(defn undo
  [db page-id]
  (let [this-undos   (get @undo-list page-id [])
        this-redos   (get @redo-list page-id [])
        this-state   (last this-undos)

        redo-state   {:page      (get-in  db [:parenoia :project page-id])
                      :selection (get-in db [:parenoia :selected-zloc])}

        new-undos    (vec (butlast this-undos))
        new-redos    (vec (concat [redo-state] this-redos))
        new-db       (-> db
                      (assoc-in [:parenoia :project page-id] (:page this-state))
                      (assoc-in [:parenoia :selected-zloc]   (:selection this-state)))]
    (println "redos:: "  (z/string (:page this-state)))
    (println "undos:: "  (count new-undos))
    (swap! undo-list assoc page-id new-undos)
    (swap! redo-list assoc page-id new-redos)
    new-db))

(defn redo
  [db page-id]
  (let [this-undos   (get @undo-list page-id [])
        this-redos   (get @redo-list page-id [])
        this-state   (first this-redos)

        undo-state   {:page      (get-in  db [:parenoia :project page-id])
                      :selection (get-in db [:parenoia :selected-zloc])}


        new-undos    (vec (concat this-undos [undo-state]))
        new-redos    (vec (rest this-redos))
        new-db       (-> db
                      (assoc-in [:parenoia :project page-id] (:page this-state)))]
                      ;(assoc-in [:parenoia :selected-zloc]   (:selection this-state)))]
    (println "redos:: "  (z/string (:page this-state)))
    (println "undos:: "  (count this-undos))
    (swap! undo-list assoc page-id new-undos)
    (swap! redo-list assoc page-id new-redos)
    new-db))

(defn undo-handler
  [db [_]]
  (let [page-id   (-> db :parenoia :selected-file)]
     (println "undoing?"  (undos? page-id))
    (if-not (undos? page-id)
      db
      (undo db page-id))))

(defn redo-handler
  [db [_]]
  (let [page-id   (-> db :parenoia :selected-file)]
    (if-not (redos? page-id)
      db
      (redo db page-id))))


(defn purge-redo-handler
  [db [_ page-id]]
  (if-not (redos? page-id)
    (re-frame/console :warn "re-frame: you did a (dispatch [:purge-redos]), but there is nothing to redo.")
    (clear-redos! page-id))
  db)


;; -- Interceptors ----------------------------------------------------------

(defn undoable []
  "returns a side-effecting Interceptor, which stores an undo checkpoint in
  `:after` processing.
   If the `:effect` cotnains an `:undo` key, then use the explanation provided
   by it. Otherwise, `explanation` can be:
     - a string (of explanation)
     - a function expected to return a string of explanation. It will be called
       with two arguments: `db` and `event-vec`.
     - a nil, in which case \"\" is recorded as the explanation
  "
  (re-frame/->interceptor
   :id     :undoable
   :before  (fn [context]
              context)
   :after (fn [context]
            (let [event         (re-frame/get-coeffect context :event)
                  undo-effect   (re-frame/get-effect   context :undo)]
              (store-now! @app-db)
              (update context :effects dissoc :undo)))))   ;; remove any `:undo` effect. Already handled.


;; -- register handlers for events and subscriptions


(defn register-events-subs!
  []
  (re-frame/reg-event-db
   :parenoia/undo
   []
   undo-handler)
  (re-frame/reg-event-db
   :parenoia/redo
   []
   redo-handler)
  (re-frame/reg-event-db
   :parenoia/purge-redos              ;; usage:  (dispatch [:purge-redos])
   purge-redo-handler))

(register-events-subs!)