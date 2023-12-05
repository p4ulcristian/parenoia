(ns routes
  (:require
   [re-frame.core :refer [dispatch]]
   [reitit.frontend :as reitit]
   [clerk.core :as clerk]
   [reagent.core :as reagent]
   [accountant.core :as accountant])
  (:import goog.History))

(def router
  (reitit/router
   [["/"  :wizard]
    ["/navigation/:path/:position" :website]]))


(defn add-routing! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (let [match          (reitit/match-by-path router path)
            current-page   (:name (:data  match))
            route-params   (:path-params match)]
        ;(reagent/after-render clerk/after-render!)
        (println "Something does happen ")
        (dispatch [:parenoia/navigate-to! 
                    (:path route-params) 
                    (cljs.reader/read-string (:position route-params))])
        (clerk/navigate-page! path)))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!))