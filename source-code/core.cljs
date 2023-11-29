(ns core
  (:require [parenoia.view :as parenoia]
            [reagent.core :as reagent]
            [reagent.dom :as reagent-dom]
            [reagent.impl.template :as reagent-template]))

(def functional-compiler (reagent/create-compiler {:function-components    true}))

(reagent-template/set-default-compiler! functional-compiler)

(def root  (.getElementById js/document "app"))

(defn start! [] (reagent-dom/render [parenoia/view] root ))