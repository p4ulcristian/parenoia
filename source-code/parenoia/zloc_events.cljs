(ns parenoia.zloc-events
  [re-frame.core :refer [reg-event-db]])

(reg-event-db
  :zloc/go-to-definition!
  (fn [db [_ zloc]]
    x
    (assoc-in db path value)))