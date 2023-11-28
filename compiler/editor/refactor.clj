(ns editor.refactor
  (:require [rewrite-clj.zip :as z]
            [editor.utils :as utils]
            [clojure-lsp.queries :as lsp-queries]
            [clojure.java.io :as io]
            [clojure-lsp.internal-api :as internal-api :refer [db*]]
            [clojure-lsp.dep-graph :as lsp-graph]
            [clojure-lsp.api :as clojure-lsp]
            [clojure-lsp.refactor.edit :as lsp-edit]
            [clojure-lsp.feature.completion :as lsp-completion]
            [clojure-lsp.feature.inline-symbol :as lsp-inline-symbol]
            [clojure-lsp.feature.move-form :as lsp-move-form]
            [editor.config :as config]
            [clj-kondo.core :as clj-kondo]))


(defn get-zloc [{:keys [file-path position]}]
 (let [file-string (utils/read-file file-path)
       zloc        (z/of-string file-string {:track-position? true})]
  (z/find-tag-by-pos zloc position :list)))




;(clojure-lsp/analyze-project-and-deps! {:project-root (io/file ".")})
                                        
;; (utils/write-file 
;;  "hello.txt"
;;  (str (clojure-lsp.api/dump {:output {:format :edn
;;                                       :filter-keys [:source-paths :analysis]}})))

;(println (lsp-queries/find-all-var-definitions @internal-api/db*))


(defn var-usages-within [zloc uri db]
  (let [scope (meta (z/node zloc))]
    (str (lsp-queries/find-var-usages-under-form db uri scope))))
    


(defn get-references [symbol]
 (:references (clojure-lsp/references {:from symbol})))

(defn symbol->position [symbol]
 (let [first-reference (first (get-references symbol))
       row (:row first-reference)
       col (:col first-reference)]
    [row col]))

(defn to-lodash-string [ns-name]
 (clojure.string/replace ns-name #"-" "_"))

(defn to-file-path [ns-name]
 (clojure.string/replace ns-name #"\." "/"))

(defn keep-namespace-from-symbol [text]
 (first
   (clojure.string/split text #"\/")))


(defn namespace-to-file [symbol]
  (str "source-code/"
   (to-file-path
    (keep-namespace-from-symbol  
     (to-lodash-string (str symbol))))
   ".cljs")) 
      


(defn move-form [from to]
  (let [from-file-path (namespace-to-file from)
        to-file-path   (namespace-to-file to)
        from-position  (symbol->position from)
        from-zloc      (get-zloc {:file-path from-file-path 
                                  :position from-position})
        new-from-zloc  (z/remove from-zloc)
        to-position     [1 1]
        to-zloc        (get-zloc {:file-path to-file-path 
                                  :position to-position})
        new-to-zloc   (z/insert-newline-right
                         (z/insert-right 
                           (z/rightmost to-zloc)
                           (z/sexpr from-zloc))
                         2)
        test-db   @internal-api/db* 
        test-uri  "file:///Users/paulcristian/Projects/zgen/parenoia/source-code/test_a.cljs" 
        test-ns   'test-a]    
    ;(println "local-var usages" (map :name (lsp-queries/find-local-var-usages-to-namespace test-db test-uri test-ns)))

    ;; (println "ns dependencies"  (lsp-graph/ns-dependencies-uris @internal-api/db* 'test-a))
    ;; (println "ns dependents:  " (lsp-graph/ns-dependents-uris @internal-api/db* 'test-a))
    ;; ;(println "internal-definitions" (str (mapv :name (lsp-queries/find-internal-definitions @internal-api/db*))))
    ;; (println "Mar nem is tudom" (str (lsp-queries/find-all-project-namespace-definitions @internal-api/db* 'test-a)))
    ;; (println "namespace definitions: " (str (lsp-queries/find-namespace-definitions @internal-api/db* "file:///Users/paulcristian/Projects/zgen/parenoia/source-code/test_a.cljs")))
    ;; (println "ns dependencies: " (map :name (lsp-queries/uri-dependencies-analysis @internal-api/db* "file:///Users/paulcristian/Projects/zgen/parenoia/source-code/test_a.cljs")))
    ;; (println "definition: " 
    ;;  (lsp-queries/find-definition-from-cursor test-db test-uri 10 8))
                             
    ;; (println "element: " (let [ans (lsp-queries/find-element-under-cursor 
    ;;                                  @internal-api/db* 
    ;;                                  "file:///Users/paulcristian/Projects/zgen/parenoia/source-code/test_a.cljs" 
    ;;                                   10 8)]
    ;;                       (str (:name ans) " - " (:to ans))))                       
    
    ;; (println 
    ;;  "vars in namespace: "
    ;;     (map :name (lsp-queries/find-var-definitions @internal-api/db* "file:///Users/paulcristian/Projects/zgen/parenoia/source-code/test_a.cljs" true)))


    ;; (println "vars in a function " 
    ;;  (map :name (var-usages-within from-zloc "file:///Users/paulcristian/Projects/zgen/parenoia/source-code/test_a.cljs" @internal-api/db*)))                          
    ;; (println "namespace-definitions: " (map :name (lsp-queries/find-all-project-namespace-definitions @internal-api/db* 'test-a)))
    ;; ;(utils/write-file from-file-path (z/root-string new-from-zloc))
    ;; ;(utils/write-file to-file-path   (z/root-string new-to-zloc))
    (str "success")))   



(defn get-absolute-path []
  (let [file-path (-> (java.io.File. ".") .getAbsolutePath)
        no-point (apply str (butlast file-path))]
     no-point))

(defn path->uri [path]
 (str "file://" path))



(defn get-element-below-cursor [uri row col]
 (lsp-queries/find-element-under-cursor @db* uri row col))

(defn get-variable-details [path position]
  (let [[row col] position]
   (clojure-lsp.api/analyze-project-only! {:project-root (io/file @config/project-path)})
   ;(println "1: " (path->uri path))
   ;(println "2: " (map :name (lsp-queries/find-var-definitions @internal-api/db* (path->uri path) true)))
   (str
    (get-element-below-cursor 
     (path->uri path)
     row col)))) 


(defn find-top-form-recursion [last-zloc zloc]
 (let [up-loc (z/up zloc)]
  (cond 
    (nil? up-loc) last-zloc
    :else (recur zloc up-loc))))
 

(defn find-top-form [zloc]
 (find-top-form-recursion nil zloc))


(defn get-kondo-lints [path]
 (println "Linting " path)
 (str (mapv (fn [tip] {:type (:type tip)
                       :row  (:row tip)
                       :col  (:col tip)
                       :level (:level tip)})
            (-> (clj-kondo/run! {:lint [path]})
                :findings))))

(defn get-form-details [path position]
 (let [[row col] position] 

  (clojure-lsp.api/analyze-project-only! {:project-root (io/file @config/project-path)})
  (str
   (var-usages-within 
    (find-top-form
     (get-zloc {:file-path (path->uri path)
                :position position}))
    (path->uri path) @db*))))
     

(defn get-definition [path position]
 (let [[row col] position] 
  (clojure-lsp.api/analyze-project-only! {:project-root (io/file @config/project-path)})
  (let [the-def (lsp-queries/find-definition-from-cursor @db* (path->uri path) row col)]
       (str {:uri (:uri the-def)
             :col (:col the-def)
             :row (:row the-def)}))))

(defn get-completion [path position]
 (let [[row col] position]
  (lsp-completion/completion 
    (path->uri path)
    row 
    col
    @db*)))


(defn rename [from to]
 (clojure-lsp.api/analyze-project-only! {:project-root (io/file @config/project-path)})
 (clojure-lsp/rename!
    {:project-root (io/file @config/project-path)
     :from (symbol from)
     :to   (symbol to)}))
 


