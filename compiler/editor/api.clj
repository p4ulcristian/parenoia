(ns editor.api
  (:require [clojure.tools.namespace.repl :as tn]
            [org.httpkit.server :as http]
            [editor.load-files :as load-files]
            [ring.middleware.reload :refer [wrap-reload]]
            [reitit.ring                  :as reitit-ring]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [editor.html :as html]
            [ring.middleware.transit      :refer [wrap-transit-params]]
            [clojure-lsp.api :as clojure-lsp]
            [clojure-lsp.feature.move-form :as move-form]
            [clojure-lsp.db :as lsp-db]
            [editor.refactor :as refactor]
            [rewrite-clj.zip :as z]
            [clojure.pprint :as pprint]))


(defn request-wrap [status content-type body]
      {:status  status
       :headers {"Content-Type" content-type}
       :body    body})

(defn html-wrap [content]
      (request-wrap 200 "text/html" content))

(defn string-wrap [content]
      (request-wrap 200 "text/plain" content))


(def app
  (reitit-ring/ring-handler
    (reitit-ring/router
      [["/"      {:get  {:handler  (fn [req]  (html-wrap (html/page)))}}]
       ["/refactor"      {:get  {:handler  (fn [req]  (string-wrap (refactor/move-form 'test-a/a 'test-b/a)))}}]                                                                 
       ["/file"  {:post {:handler  (fn [req]  
                                     (println "hello " req)
                                     (string-wrap (load-files/save-file (:params req))))}}]
       ["/files" {:get  {:handler  (fn [req]  (string-wrap (str (load-files/project-structure))))}}]])
       
    (reitit-ring/routes
      (reitit-ring/create-resource-handler 
       {:path "/" :root "/frontend"}))
      
    {:middleware
     (concat
       [#(wrap-keyword-params %)
        #(wrap-params %)
        
        #(wrap-transit-params % {:opts {}})])}))
        
        

(defonce server (atom nil))

(def port 4200)

(defn start []
  (reset! server
          (http/run-server 
           (wrap-reload #'app)
           {:port port :join? false}))
  (println (str "Listening on port " port)))

(defn stop []
  (when @server
    (@server :timeout 100)
    (reset! server nil)))

(defn restart []
  (stop)
  (tn/refresh :after 'your-project.core/start))