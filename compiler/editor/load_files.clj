(ns editor.load-files
  (:require
   [cljfmt.core :as fmt]
   [clojure.string :as clojure.string]
   [editor.config :as config]
   [editor.utils :as utils]
   [rewrite-clj.zip :as z]))



(defn get-all-files []
  (let [directory (clojure.java.io/file @config/project-path)
        dir? #(.isDirectory %)]
    (mapv #(.getPath %)
      (filter (comp not dir?)
        (tree-seq dir? #(.listFiles %) directory)))))



(defn get-all-clojure-files []
  (vec (filter
         (fn [file] (or
                      (= "clj" (last (clojure.string/split file #"\.")))
                      (= "cljs" (last (clojure.string/split file #"\.")))
                      (= "cljc" (last (clojure.string/split file #"\.")))))
         (get-all-files))))



(defn project-structure []
  (let [file-names (get-all-clojure-files)]
    (reduce merge
      (mapv
        (fn [file-name]
          {file-name (utils/read-file file-name)})
        file-names))))



(defn get-forms-divided-by-newlines-recursion [zloc]
  (if (z/rightmost? zloc)
    zloc
    (get-forms-divided-by-newlines-recursion
      (z/right (z/edit-node zloc (fn [this-zloc]  (z/insert-newline-right zloc 2)))))))



(defn get-forms-divided-by-newlines [text]
  (z/root-string (get-forms-divided-by-newlines-recursion
                   (z/of-string text))))



(defn clj-format [text]
  (fmt/reformat-string text
    {:split-keypairs-over-multiple-lines? true
     :sort-ns-references? true
     :indents {#".*" [[:inner 0]]}}))



(defn save-file [{:keys [path content]}]
  (let []
    (utils/write-file path (get-forms-divided-by-newlines (clj-format content)))
    (str path " written successfully.")))



;{:document-changes ({:text-document {:version 0, :uri "file:///Users/paulcristian/Projects/zgen/wizard/source-code/wizard/editor_events/editor.cljs"}, :edits [{:range {:start {:line 158, :character 41}, :end {:line 158, :character 57}}, :new-text ""}]} {:text-document {:version 0, :uri "file:///Users/paulcristian/Projects/zgen/wizard/source-code/wizard/editor_events/areas.cljs"}, :edits [{:range {:start {:line 151, :character 36}, :end {:line 151, :character 52}}, :new-text ""} {:range {:start {:line 162, :character 28}, :end {:line 162, :character 44}}, :new-text ""} {:range {:start {:line 170, :character 48}, :end {:line 170, :character 64}}, :new-text ""} {:range {:start {:line 180, :character 48}, :end {:line 180, :character 64}}, :new-text ""} {:range {:start}}]})} 


(defn count-till-line-index [file line]
   (reduce + 
     (map (fn [this-line] (inc (count this-line))) 
      (take line
        (clojure.string/split file #"\n")))))

(defn line-and-char->char-index [file line-and-char]
  (let [{:keys [line character]} line-and-char]
    (+ (:character line-and-char) 
       (count-till-line-index file line))))



(defn get-split-text-at-ranges [file positions]
  (let [changed-strings (reduce 
                         (fn [{:keys [result last-index]} [pos-start pos-end edit-text]]
                            (println "heheee" (subs file last-index pos-start))
                            {:last-index pos-end
                             :result (vec (conj result 
                                           (subs file last-index pos-start)
                                           edit-text))})
                         {:last-index 0
                          :result []}
                         positions)
        string-before (apply str (:result changed-strings))
        string-after   (subs file (:last-index changed-strings))]
     
      (str string-before string-after)))
  

(defn rename-positions-to [uri file positions]
 (let [splitted-at-ranges (get-split-text-at-ranges file positions)]
     (utils/write-file uri splitted-at-ranges)))  
        
  
  

(defn replace-at-ranges
  ([uri file edits] 
   (replace-at-ranges uri file edits 0 0))
  ([uri file edits edit-line edit-character]
   (rename-positions-to 
    uri
    file
    (mapv 
      (fn [edit]
       (let [this-edit edit
             edit-range (:range this-edit)
             edit-start (:start edit-range)
             edit-start-index (line-and-char->char-index file edit-start)
             edit-end   (:end edit-range)
             edit-end-index (line-and-char->char-index file edit-end)
             edit-text (:new-text this-edit)]
             
         [edit-start-index edit-end-index edit-text]))
      edits))))
   


(defn edit-file [change]
 (let [uri (-> change :text-document :uri)
       edits (-> change :edits)
       file-str (utils/read-file uri)]
    (replace-at-ranges uri file-str edits)))   
   

(defn edit-files [edits]
 (let [changes (:document-changes edits)]
    
     (doseq [change changes] 
         (edit-file change))))

