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
            [clojure.pprint :as pprint]
            [clojure-lsp.queries :as lsp-queries]
            [clojure.core.async :as async]
            [clojure.java.io :as io]
            [editor.utils :as utils]
            [clojure-lsp.internal-api :as internal-api]
            [editor.config :as config]))
            

(defn request-wrap [status content-type body]
      {:status  status
       :headers {"Content-Type" content-type}
       :body    body})

(defn html-wrap [content]
      (request-wrap 200 "text/html" content))

(defn string-wrap [content]
      (request-wrap 200 "text/plain" content))

;(refactor/move-form 'test-a/a 'test-b/a)

(def app
  (reitit-ring/ring-handler
    (reitit-ring/router
      [["/"           {:get  {:handler  (fn [req]  (html-wrap (html/page)))}}]
       ["/db"         {:get  {:handler  (fn [req]  (string-wrap 
                                                     "oi"))}}]
       ["/completion" {:post {:handler (fn [req] 
                                        (let [body (:params req)
                                              {:keys [file-path position]} body]
                                            (string-wrap 
                                             (refactor/get-completion
                                              file-path position))))}}]
       ["/get-definition" {:post {:handler (fn [req] 
                                            (let [body (:params req)
                                                  {:keys [file-path position]} body]
                                             (string-wrap (refactor/get-definition file-path position))))}}]
       ["/kondo-lints" {:post {:handler (fn [req] 
                                           (let [body (:params req)
                                                 {:keys [file-path position]} body]
                                            (string-wrap (refactor/get-kondo-lints file-path))))}}]
       ["/variable-info" {:post {:handler (fn [req] 
                                           (let [body (:params req)
                                                 {:keys [file-path position]} body]
                                            (string-wrap 
                                             (refactor/get-variable-details 
                                              file-path position))))}}]
       ["/form-info" {:post {:handler (fn [req] 
                                          (let [body (:params req)
                                                {:keys [file-path position]} body]
                                           (string-wrap 
                                            (refactor/get-form-details 
                                             file-path position))))}}]                                                                                             
       ["/rename" {:post {:handler (fn [req]
                                     (let [body (:params req)
                                           {:keys [from to]} body]
                                        (string-wrap 
                                         (refactor/rename from to))))}}]
       ["/set-project-path" {:post {:handler (fn [req]
                                              (let [body (:params req)
                                                    {:keys [path]} body]
                                                (clojure-lsp.api/dump {:project-root (io/file @config/project-path)})
                                                (string-wrap 
                                                 (str 
                                                  (reset! config/project-path path)))))}}]                                 
       ["/refactor"   {:get  {:handler  (fn [req]  (string-wrap (refactor/move-form 'test-a/a 'test-b/a)))}}]   
       ["/references" {:get  {:handler  (fn [req]  (string-wrap (str (refactor/get-references 'test-a/a))))}}]                                                                                                                               
       ["/file"  {:post {:handler  (fn [req]  
                                     (println "hello " req)
                                     (string-wrap (load-files/save-file (:params req))))}}]
       ["/files" {:get  {:handler  (fn [req]  (string-wrap (str (load-files/project-structure))))}}]])
       
    (reitit-ring/routes
      (reitit-ring/create-resource-handler 
       {:path "/" :root "/frontend"}))
      
    {:middleware
     (concat
       [#(wrap-reload % {:dirs ["compiler" "source-code"]})
        #(wrap-keyword-params %)
        #(wrap-params %)
        #(wrap-transit-params % {:opts {}})])}))
        
        

(defonce server (atom nil))

(def port 4200)

(defn start []
   (reset! server
           (http/run-server app {:port port :join? false}))
   (println (str "Listening on port " port)))

(defn stop []
  (when @server
    (@server :timeout 100)
    (reset! server nil)))

(defn restart []
  (stop)
  (tn/refresh :after 'your-project.core/start))