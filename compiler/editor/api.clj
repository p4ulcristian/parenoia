(ns editor.api
  (:require [clojure.tools.namespace.repl :as tn]
            [org.httpkit.server :as http]
            [editor.load-files :as load-files]
            [ring.middleware.reload :refer [wrap-reload]]
            [reitit.ring                  :as reitit-ring]
            [editor.html :as html]
            [ring.middleware.transit      :refer [wrap-transit-params]]))


(defn request-wrap [status content-type body]
      {:status  status
       :headers {"Content-Type" content-type}
       :body    body})

(defn html-wrap [content]
      (request-wrap 200 "text/html" content))


(def app
  (reitit-ring/ring-handler
    (reitit-ring/router
      [["/"      {:get {:handler  (fn [req] (html-wrap (html/page)))}}]])
    (reitit-ring/routes
      (reitit-ring/create-resource-handler 
       {:path "/" :root "/frontend"})
      (reitit-ring/create-default-handler))
    {:middleware
     (concat
       [#(wrap-transit-params % {:opts {}})
        #(wrap-reload %)])}))
        

(defonce server (atom nil))

(def port 4200)

(defn start []
  (reset! server
          (http/run-server #'app {:port port :join? false}))
  (println (str "Listening on port " port)))

(defn stop []
  (when @server
    (@server :timeout 100)
    (reset! server nil)))

(defn restart []
  (stop)
  (tn/refresh :after 'your-project.core/start))