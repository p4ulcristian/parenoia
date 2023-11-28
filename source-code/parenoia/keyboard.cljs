(ns parenoia.keyboard
  (:require ["react" :as react]
            [re-frame.core :refer [dispatch subscribe]]
            [rewrite-clj.paredit :as paredit]
            [rewrite-clj.zip :as z]))

(defn has-position? [zloc]
  (try (z/position zloc)
    (catch js/Error e false)))

(defn add-listener [^js el the-fn]
  (.addEventListener el "keydown" the-fn))

(defn remove-listener [^js el the-fn]
  (.removeEventListener el "keydown" the-fn))

(defn check-key [^js event key-to-check]
  (= key-to-check (.-key event)))

(defn set-zloc [zloc]
  (when (has-position? zloc)
    (dispatch [:db/set [:parenoia :selected-zloc] zloc])))

(defn zloc->file [zloc]
  (z/of-node
    (z/root zloc)
    {:track-position? true}))

(defn modify-file [zloc]
  (let [file-zloc      (zloc->file zloc)
        selected-file  @(subscribe [:db/get [:parenoia :selected-file]])]
    (dispatch [:parenoia/set-file! file-zloc zloc])))

(defn run-on-key [^js event event-name]
  (when (check-key event event-name)))


(defn without-special-keys [^js event]
 (and 
  (not (.-shiftKey event))
  (not (.-metaKey event))))

(defn on-q-fn [current-zloc]
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "q"))
      (do
        (.preventDefault event)
        (modify-file (paredit/slurp-backward current-zloc))))))

(defn on-w-fn [current-zloc]
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "w"))
      (do
        (.preventDefault event)
        (modify-file (paredit/barf-backward current-zloc))))))


(defn on-e-fn [current-zloc]
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "e"))
      (do
        (.preventDefault event)
        (modify-file (paredit/barf-forward current-zloc))))))

(defn on-r-fn [current-zloc]
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "r"))
      (do
        (.preventDefault event)
        (modify-file (paredit/slurp-forward current-zloc))))))

(defn on-a-fn [current-zloc]
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "a"))
      (do
        (.preventDefault event)
        (modify-file (z/left (z/insert-left current-zloc 'x)))))))

(defn on-s-fn [current-zloc]
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "s"))
      (do
        (.preventDefault event)
        (modify-file (z/left (z/insert-left (z/down current-zloc) 'x)))))))

(defn on-d-fn [current-zloc]
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "d"))
      (do
        (.preventDefault event)
        (modify-file (z/right (z/insert-right current-zloc 'x)))))))

(defn on-shift-a-fn [current-zloc]
  (fn [^js event]
    (when (and (.-shiftKey event) (check-key event "A"))
      (do
        (.preventDefault event)
        (modify-file
          (z/left (z/insert-newline-left (z/insert-left current-zloc 'x))))))))


(defn on-shift-d-fn [current-zloc]
  (fn [^js event]
    (when (and (.-shiftKey event) (check-key event "D"))
      (do
        (.preventDefault event)
        (modify-file
          (z/right (z/insert-newline-right (z/insert-right current-zloc 'x))))))))



(defn on-left-fn [current-zloc]
  (fn [^js event]
    (when (check-key event "ArrowLeft")
      (do
        (.preventDefault event)
        (set-zloc
          (if (has-position? (z/left current-zloc))
            (z/left current-zloc)
            (z/prev current-zloc)))))))

(defn on-right-fn [current-zloc]
  (fn [^js event]
    (when (check-key event "ArrowRight")
      (do
        (.preventDefault event)
        (set-zloc
          (if (has-position? (z/right current-zloc))
            (z/right current-zloc)
            (z/next current-zloc)))))))


(defn on-up-fn [current-zloc]
  (fn [^js event]
    (when (check-key event "ArrowUp")
      (do
        (.preventDefault event)
        (set-zloc
          (cond (has-position? (z/prev current-zloc))
            (z/prev current-zloc)))))))

(defn on-down-fn [current-zloc]
  (fn [^js event]
    (when (check-key event "ArrowDown")
      (do
        (.preventDefault event)
        (set-zloc
          (cond (has-position? (z/next current-zloc))
            (z/next current-zloc)))))))

(defn on-tab-fn [current-zloc]
  (fn [^js event]
    (when (and (check-key event "Tab") (not (.-shiftKey event)))
      (do
        (.preventDefault event)
        (modify-file (paredit/wrap-around current-zloc :list))))))

(defn on-shift-tab-fn [current-zloc]
  (fn [^js event]
    (when (and (check-key event "Tab") (.-shiftKey event))
      (do
        (.preventDefault event)
        (modify-file  (paredit/splice (z/up current-zloc)))))))

(defn on-space-fn [current-zloc]
  (fn [^js event]
    (when (and (check-key event " ") (not  (.-shiftKey event)))
      (do
        (.preventDefault event)
        (modify-file (z/right (z/insert-right current-zloc 'x)))))))

(defn on-shift-space-fn [current-zloc]
  (fn [^js event]
    (when (and (check-key event " ") (.-shiftKey event))
      (do
        (println "shift space")
        (.preventDefault event)
        (modify-file
          (z/right (z/insert-newline-right (z/insert-right current-zloc 'x))))))))

(defn on-shift-enter-fn [current-zloc]
  (fn [^js event]
    (when (and (check-key event "Enter") (.-shiftKey event))
      (do
        (println "Shift enter? ")
        (.preventDefault event)
        (modify-file (z/right (z/insert-newline-right current-zloc)))))))

(defn on-enter-fn [current-zloc]
  (fn [^js event]
    (when (and (check-key event "Enter") (not (.-shiftKey event)))
      (do
        (.preventDefault event)
        (dispatch [:db/set [:parenoia :editable?]
                   (not @(subscribe [:db/get [:parenoia :editable?]]))])))))

(defn on-esc-fn [current-zloc]
  (fn [^js event]
    (when (check-key event "Escape")
      (do
        (.preventDefault event)
        (dispatch [:db/set [:parenoia :editable?] false])))))

(defn on-backspace-fn [current-zloc]
  (fn [^js event]
    (when (and (check-key event "Backspace") (not (.-shiftKey event)))
      (do
        (.preventDefault event)
        (let [removed-zloc (z/remove current-zloc)
              to-right (z/right removed-zloc)]
         (modify-file (if to-right 
                         to-right
                         removed-zloc)))))))

(defn remove-till-prev-node [zloc]
  (println (z/tag zloc) " - " (z/whitespace-or-comment? zloc))
  (if (z/whitespace-or-comment? zloc)
    (remove-till-prev-node (z/remove* zloc))
    (z/insert-space-right zloc)))

(defn on-shift-backspace-fn [current-zloc]
  (fn [^js event]
    (when (and (check-key event "Backspace") (.-shiftKey event))
      (do
        (.preventDefault event)
        (modify-file (remove-till-prev-node (z/left* current-zloc)))))))

(defn on-command-z-fn [current-zloc]
  (fn [^js event]
    (when (and (check-key event "z")
            (.-metaKey event)
            (not (.-shiftKey event)))
      (do
        (.preventDefault event)
        (println "ctrl-z")
        (dispatch [:parenoia/undo])))))

(defn on-command-shift-z-fn [current-zloc]
  (fn [^js event]
    (when (and (check-key event "z")
            (.-metaKey event)
            (.-shiftKey event))
      (do
        (.preventDefault event)
        (println "ctrl-shift-z")
        (dispatch [:parenoia/redo])))))

(defn on-command-s-fn [current-zloc]
  (fn [^js event]
    (when (and (check-key event "s")
            (.-metaKey event))

      (do
        (.preventDefault event)
        (println "ctrl-shift-s")
        (dispatch [:parenoia/save!])))))

(defn on-m-fn [current-zloc]
  (fn [^js event]
    (when (check-key event "m")

      (do
        (.preventDefault event)
        (let [menu? @(subscribe [:db/get [:parenoia :menu?]])]
          (dispatch [:db/set [:parenoia :menu?] (not menu?)]))))))

(defn on-g-fn [current-zloc]
  (fn [^js event]
    (when (check-key event "g")
      (do
        (.preventDefault event)
        (let [project-map? @(subscribe [:db/get [:parenoia :project-map?]])]
          (dispatch [:db/set [:parenoia :project-map?] (not project-map?)]))))))


(defn effect [ref]
  (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])

        on-left            (on-left-fn current-zloc)
        on-right           (on-right-fn current-zloc)
       
        on-up              (on-up-fn current-zloc)
        on-down            (on-down-fn current-zloc)
        on-tab             (on-tab-fn current-zloc)
        on-shift-tab       (on-shift-tab-fn current-zloc)
        on-space           (on-space-fn current-zloc)
        on-shift-space     (on-shift-space-fn current-zloc)
        on-shift-enter     (on-shift-enter-fn current-zloc)
        on-enter           (on-enter-fn current-zloc)
        on-backspace       (on-backspace-fn current-zloc)
        on-shift-backspace (on-shift-backspace-fn current-zloc)
        on-esc             (on-esc-fn current-zloc)
        on-command-z       (on-command-z-fn current-zloc)
        on-command-shift-z       (on-command-shift-z-fn current-zloc)
        on-command-s       (on-command-s-fn current-zloc)
        on-m               (on-m-fn current-zloc)
        on-g               (on-g-fn current-zloc)
        on-q               (on-q-fn current-zloc)
        on-w               (on-w-fn current-zloc)
        on-e               (on-e-fn current-zloc)
        on-r               (on-r-fn current-zloc)
        on-a               (on-a-fn current-zloc)
        on-s               (on-s-fn current-zloc)
        on-d               (on-d-fn current-zloc)
        on-shift-a         (on-shift-a-fn current-zloc)
        on-shift-d         (on-shift-d-fn current-zloc)]
    (react/useEffect
      (fn []
        ;(.log js/console (.-current ref))
        (add-listener js/document on-left)
        (add-listener js/document on-right)
       
        (add-listener js/document on-up)
        (add-listener js/document on-down)
        (add-listener js/document on-tab)
        (add-listener js/document on-shift-tab)
        (add-listener js/document on-space)
        (add-listener js/document on-shift-space)
        (add-listener js/document on-shift-enter)
        (add-listener js/document on-enter)
        (add-listener js/document on-backspace)
        (add-listener js/document on-shift-backspace)
        (add-listener js/document on-esc)
        (add-listener js/document on-command-z)
        (add-listener js/document on-command-shift-z)
        (add-listener js/document on-command-s)
        (add-listener js/document on-m)
        (add-listener js/document on-g)
        (add-listener js/document on-q)
        (add-listener js/document on-w)
        (add-listener js/document on-e)
        (add-listener js/document on-r)
        (add-listener js/document on-a)
        (add-listener js/document on-s)
        (add-listener js/document on-d)
        (add-listener js/document on-shift-a)
        (add-listener js/document on-shift-d)

        (fn []
          (remove-listener js/document on-left)
          (remove-listener js/document on-right)
          (remove-listener js/document on-up)
          (remove-listener js/document on-down)
          (remove-listener js/document on-tab)
          (remove-listener js/document on-shift-tab)
          (remove-listener js/document on-space)
          (remove-listener js/document on-shift-space)
          (remove-listener js/document on-shift-enter)
          (remove-listener js/document on-enter)
          (remove-listener js/document on-backspace)
          (remove-listener js/document on-shift-backspace)
          (remove-listener js/document on-esc)
          (remove-listener js/document on-command-z)
          (remove-listener js/document on-command-shift-z)
          (remove-listener js/document on-command-s)
          (remove-listener js/document on-m)
          (remove-listener js/document on-g)
          (remove-listener js/document on-q)
          (remove-listener js/document on-w)
          (remove-listener js/document on-e)
          (remove-listener js/document on-r)
          (remove-listener js/document on-a)
          (remove-listener js/document on-s)
          (remove-listener js/document on-d)
          (remove-listener js/document on-shift-a)
          (remove-listener js/document on-shift-d)))
      #js [current-zloc])))

