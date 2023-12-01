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

(defn on-q-fn []
  (fn [^js event]

    (when (and (without-special-keys event) (check-key event "q"))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (modify-file (paredit/slurp-backward current-zloc)))))))

(defn on-w-fn []
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "w"))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (modify-file (paredit/barf-backward current-zloc)))))))

(defn barf-test-with-string []
  (-> "((x) 1)"
    (z/of-string {:track-position? true})
    z/down
    paredit/barf-forward
    z/root-string))

(defn on-e-fn []
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "e"))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (barf-test-with-string)
          (.preventDefault event)
          (modify-file (paredit/barf-forward current-zloc)))))))

(defn on-r-fn []
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "r"))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (modify-file (paredit/slurp-forward current-zloc)))))))

(defn on-a-fn []
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "a"))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (dispatch [:db/set [:parenoia :editable?] true])
          (modify-file (z/left (z/insert-left current-zloc 'x))))))))

(defn on-s-fn []
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "s"))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (dispatch [:db/set [:parenoia :editable?] true])
          (modify-file (z/left (z/insert-left (z/down current-zloc) 'x))))))))

(defn on-d-fn []
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "d"))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (dispatch [:db/set [:parenoia :editable?] true])
          (modify-file (z/right (z/insert-right current-zloc 'x))))))))

(defn on-shift-a-fn []
  (fn [^js event]
    (when (and (.-shiftKey event) (check-key event "A"))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (dispatch [:db/set [:parenoia :editable?] true])
          (modify-file
            (z/left (z/insert-newline-left (z/insert-left current-zloc 'x)))))))))

(defn on-shift-d-fn []
  (fn [^js event]
    (when (and (.-shiftKey event) (check-key event "D"))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (dispatch [:db/set [:parenoia :editable?] true])
          (modify-file
            (z/right (z/insert-newline-right (z/insert-right current-zloc 'x)))))))))

(defn on-left-fn []
  (fn [^js event]
    (when (check-key event "ArrowLeft")
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (set-zloc
            (if (has-position? (z/left current-zloc))
              (z/left current-zloc)
              (z/prev current-zloc))))))))

(defn on-right-fn []
  (fn [^js event]
    (when (check-key event "ArrowRight")
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (set-zloc
            (if (has-position? (z/right current-zloc))
              (z/right current-zloc)
              (z/next current-zloc))))))))

(defn on-up-fn []
  (fn [^js event]
    (when (check-key event "ArrowUp")
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (set-zloc
            (cond (has-position? (z/prev current-zloc))
              (z/prev current-zloc))))))))

(defn on-down-fn []
  (fn [^js event]
    (when (check-key event "ArrowDown")
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (set-zloc
            (cond (has-position? (z/next current-zloc))
              (z/next current-zloc))))))))

(defn on-tab-fn []
  (fn [^js event]
    (when (and (check-key event "Tab") (not (.-shiftKey event)))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (modify-file (paredit/wrap-around current-zloc :list)))))))

(defn on-shift-tab-fn []
  (fn [^js event]
    (when (and (check-key event "Tab") (.-shiftKey event))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (modify-file  (paredit/splice (z/up current-zloc))))))))

(defn on-space-fn []
  (fn [^js event]
    (when (and (check-key event " ") (not  (.-shiftKey event)))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (modify-file (z/right (z/insert-right current-zloc 'x))))))))

(defn on-shift-space-fn []
  (fn [^js event]
    (when (and (check-key event " ") (.-shiftKey event))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (println "shift space")
          (.preventDefault event)
          (modify-file
            (z/right (z/insert-newline-right (z/insert-right current-zloc 'x)))))))))

(defn on-shift-enter-fn []
  (fn [^js event]
    (when (and (check-key event "Enter") (.-shiftKey event))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (println "Shift enter? ")
          (.preventDefault event)
          (modify-file (z/right (z/insert-newline-right current-zloc))))))))

(defn on-enter-fn []
  (fn [^js event]
    (when (and (check-key event "Enter") (not (.-shiftKey event)))
      (do
        (.preventDefault event)
        (dispatch [:db/set [:parenoia :editable?]
                   (not @(subscribe [:db/get [:parenoia :editable?]]))])))))

(defn on-esc-fn []
  (fn [^js event]
    (when (check-key event "Escape")
      (do
        (.preventDefault event)
        (dispatch [:db/set [:parenoia :editable?] false])
        (dispatch [:db/set [:parenoia :menu?] false])))))

(defn on-backspace-fn []
  (fn [^js event]
    (when (and (check-key event "Backspace") (not (.-shiftKey event)))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (let [removed-zloc (z/remove current-zloc)
                to-right (z/right removed-zloc)]
            (modify-file (if to-right
                           to-right
                           removed-zloc))))))))

(defn remove-till-prev-node [zloc]
  (println (z/tag zloc) " - " (z/whitespace-or-comment? zloc))
  (if (z/whitespace-or-comment? zloc)
    (remove-till-prev-node (z/remove* zloc))
    (z/insert-space-right zloc)))

(defn on-shift-backspace-fn []
  (fn [^js event]
    (when (and (check-key event "Backspace") (.-shiftKey event))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (modify-file (remove-till-prev-node (z/left* current-zloc))))))))

(defn on-command-z-fn []
  (fn [^js event]
    (when (and (check-key event "z")
            (.-metaKey event)
            (not (.-shiftKey event)))
      (do
        (dispatch [:db/set [:parenoia :editable?] false])
        (.preventDefault event)
        (println "ctrl-z")
        (dispatch [:parenoia/undo])))))

(defn on-command-shift-z-fn []
  (fn [^js event]
    (when (and (check-key event "z")
            (.-metaKey event)
            (.-shiftKey event))
      (do
        (dispatch [:db/set [:parenoia :editable?] false])
        (.preventDefault event)
        (println "ctrl-shift-z")
        (dispatch [:parenoia/redo])))))

(defn on-command-s-fn []
  (fn [^js event]
    (when (and (check-key event "s")
            (.-metaKey event))

      (do
        (.preventDefault event)
        (println "ctrl-shift-s")
        (dispatch [:parenoia/save!])))))

(defn on-m-fn []
  (fn [^js event]
    (when (check-key event "m")

      (do
        (.preventDefault event)
        (let [menu? @(subscribe [:db/get [:parenoia :menu?]])]
          (dispatch [:db/set [:parenoia :menu?] (not menu?)]))))))

(defn on-g-fn []
  (fn [^js event]
    (when (check-key event "g")
      (do
        (.preventDefault event)
        (let [project-map? @(subscribe [:db/get [:parenoia :project-map?]])]
          (dispatch [:db/set [:parenoia :project-map?] (not project-map?)]))))))

(defn effect [ref]
  (let [on-left            (on-left-fn)
        on-right           (on-right-fn)
        on-up              (on-up-fn)
        on-down            (on-down-fn)
        on-tab             (on-tab-fn)
        on-shift-tab       (on-shift-tab-fn)
        on-space           (on-space-fn)
        on-shift-space     (on-shift-space-fn)
        on-shift-enter     (on-shift-enter-fn)
        on-enter           (on-enter-fn)
        on-backspace       (on-backspace-fn)
        on-shift-backspace (on-shift-backspace-fn)
        on-esc             (on-esc-fn)
        on-command-z       (on-command-z-fn)
        on-command-shift-z       (on-command-shift-z-fn)
        on-command-s       (on-command-s-fn)
        on-m               (on-m-fn)
        on-g               (on-g-fn)
        on-q               (on-q-fn)
        on-w               (on-w-fn)
        on-e               (on-e-fn)
        on-r               (on-r-fn)
        on-a               (on-a-fn)
        on-s               (on-s-fn)
        on-d               (on-d-fn)
        on-shift-a         (on-shift-a-fn)
        on-shift-d         (on-shift-d-fn)]
    (react/useEffect
      (fn []
        (println "hanyszor")
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
      #js [])))

(defn block-some-keyboard-events [^js e]
  (if
    (or
      (check-key e "ArrowLeft")
      (check-key e "ArrowRight")
      (check-key e "ArrowDown")
      (check-key e "ArrowUp")
      (and (.-shiftKey e) (check-key e "Enter"))
      (check-key e " ")
      (check-key e "Backspace")
      (check-key e "Tab")
      (check-key e "m")
      (check-key e "g")
      (check-key e "q")
      (check-key e "w")
      (check-key e "e")
      (check-key e "r")
      (check-key e "a")
      (check-key e "s")
      (check-key e "d"))
    (.stopPropagation e)))

