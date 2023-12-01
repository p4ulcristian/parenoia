(ns editor.html 
  (:require [hiccup.page :refer [include-js include-css html5]]))

(defn head []
  [:head
   [:title "Parenoia"]
   (include-css "css/normalize.css")
   (include-css "css/parenoia.css")])

(defn page []
  (html5
    (head)
    [:body
     [:div#app "loading"]
     (include-js "https://kit.fontawesome.com/bdf6c8be51.js")
     (include-js "/js/libs/node-modules.js")
     (include-js "/js/core.js")
     ]))

