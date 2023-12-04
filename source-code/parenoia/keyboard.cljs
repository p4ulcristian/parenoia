(ns parenoia.keyboard
  (:require ["react" :as react]
            [parenoia.utils :as utils]
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
  (let [file-zloc      (zloc->file zloc)]
    (dispatch [:parenoia/set-file! file-zloc zloc])))

(defn run-on-key [^js event event-name]
  (when (check-key event event-name)))

(defn without-special-keys [^js event]
  (and
    (not (.-shiftKey event))
    (not (.-metaKey event))))

(defn on-q-fn [zloc]
  (fn [^js event]

    (when (and (without-special-keys event) (check-key event "q"))
      (.preventDefault event)
      (modify-file (paredit/slurp-backward zloc)))))

(defn on-w-fn [zloc]
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "w"))
      (.preventDefault event)
      (modify-file (paredit/barf-backward zloc)))))

(defn barf-test-with-string []
  (-> "((x) 1)"
    (z/of-string {:track-position? true})
    z/down
    paredit/barf-forward
    z/root-string))

(defn on-e-fn [zloc]
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "e"))
      (barf-test-with-string)
      (.preventDefault event)
      (modify-file (paredit/barf-forward zloc)))))

(defn on-r-fn [zloc]
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "r"))
      (.preventDefault event)
      (modify-file (paredit/slurp-forward zloc)))))

(defn on-a-fn [zloc]
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "a"))
      (.preventDefault event)
      (dispatch [:db/set [:parenoia :editable?] true])
      (modify-file (z/left (z/insert-left zloc 'x))))))

(defn on-s-fn [zloc]
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "s"))
      (.preventDefault event)
      (dispatch [:db/set [:parenoia :editable?] true])
      (modify-file (z/left (z/insert-left (z/down zloc) 'x))))))

(defn on-d-fn [zloc]
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "d"))
      (.preventDefault event)
      (dispatch [:db/set [:parenoia :editable?] true])
      (modify-file (z/right (z/insert-right zloc 'x))))))

(defn on-shift-a-fn [zloc]
  (fn [^js event]
    (when (and (.-shiftKey event) (check-key event "A"))
      (.preventDefault event)
      (dispatch [:db/set [:parenoia :editable?] true])
      (modify-file
        (z/left (z/insert-newline-left (z/insert-left zloc 'x)))))))

(defn on-shift-d-fn [zloc]
  (fn [^js event]
    (when (and (.-shiftKey event) (check-key event "D"))
      (.preventDefault event)
      (dispatch [:db/set [:parenoia :editable?] true])
      (modify-file
        (z/right (z/insert-newline-right (z/insert-right zloc 'x)))))))

(defn on-left-fn [zloc]
  (fn [^js event]
    (when (check-key event "ArrowLeft")
      (.preventDefault event)
      (set-zloc
        (if (has-position? (z/left zloc))
          (z/left zloc)
          (z/prev zloc))))))

(defn on-right-fn [zloc]
  (fn [^js event]
    (when (check-key event "ArrowRight")
      (.preventDefault event)
      (set-zloc
        (if (has-position? (z/right zloc))
          (z/right zloc)
          (z/next zloc))))))

(defn on-up-fn [zloc]
  (fn [^js event]
    (when (check-key event "ArrowUp")
      (.preventDefault event)
      (set-zloc
        (when (has-position? (z/prev zloc))
          (z/prev zloc))))))

(defn on-down-fn [zloc]
  (fn [^js event]
    (when (check-key event "ArrowDown")
      (.preventDefault event)
      (set-zloc
        (cond (has-position? (z/next zloc))
          (z/next zloc))))))

(defn on-tab-fn [zloc]
  (fn [^js event]
    (when (and (check-key event "Tab") (not (.-shiftKey event)))
      (.preventDefault event)
      (modify-file (paredit/wrap-around zloc :list)))))

(defn on-shift-tab-fn [zloc]
  (fn [^js event]
    (when (and (check-key event "Tab") (.-shiftKey event))
      (.preventDefault event)
      (modify-file  (paredit/splice (z/up zloc))))))

(defn on-space-fn [zloc]
  (fn [^js event]
    (when (and (check-key event " ") (not  (.-shiftKey event)))
      (.preventDefault event)
      (modify-file (z/right (z/insert-right zloc 'x))))))

(defn on-shift-space-fn [zloc]
  (fn [^js event]
    (when (and (check-key event " ") (.-shiftKey event))
      (println "shift space")
      (.preventDefault event)
      (modify-file
        (z/right (z/insert-newline-right (z/insert-right zloc 'x)))))))

(defn on-shift-enter-fn [zloc]
  (fn [^js event]
    (when (and (check-key event "Enter") (.-shiftKey event))
      (println "Shift enter? ")
      (.preventDefault event)
      (modify-file (z/right (z/insert-newline-right zloc))))))

(defn on-enter-fn [zloc]
  (fn [^js event]
    (when (and (check-key event "Enter") (not (.-shiftKey event)))
      (do
        (.preventDefault event)
        (dispatch [:db/set [:parenoia :editable?]
                   (not @(subscribe [:db/get [:parenoia :editable?]]))])))))

(defn on-esc-fn [zloc]
  (fn [^js event]
    (when (check-key event "Escape")
      (do
        (.preventDefault event)
        (dispatch [:db/set [:parenoia :global-search?] false])
        (dispatch [:db/set [:parenoia :editable?] false])
        (dispatch [:db/set [:parenoia :menu?] false])))))

(defn on-backspace-fn [zloc]
  (fn [^js event]
    (when (and (check-key event "Backspace")
            (not (.-shiftKey event)))
      (.preventDefault event)
      (let [removed-zloc (z/remove zloc)
            to-down (z/down removed-zloc)]

        (modify-file (if to-down
                       to-down
                       removed-zloc))))))

(defn remove-till-prev-node [[zloc]]
  (println (z/tag zloc) " - " (z/whitespace-or-comment? zloc))
  (if (z/whitespace-or-comment? zloc)
    (remove-till-prev-node (z/remove* zloc))
    (z/insert-space-right zloc)))

(defn on-shift-backspace-fn [zloc]
  (fn [^js event]
    (when (and (check-key event "Backspace") (.-shiftKey event))
      (let [current-zloc @(subscribe [:db/get [:parenoia :selected-zloc]])]
        (do
          (.preventDefault event)
          (modify-file (remove-till-prev-node (z/left* current-zloc))))))))

(defn on-command-z-fn [zloc]
  (fn [^js event]
    (when (and (check-key event "z")
            (.-metaKey event)
            (not (.-shiftKey event)))
      (do
        (dispatch [:db/set [:parenoia :editable?] false])
        (.preventDefault event)
        (println "ctrl-z")
        (dispatch [:parenoia/undo])))))

(defn on-command-shift-z-fn [zloc]
  (fn [^js event]
    (when (and (check-key event "z")
            (.-metaKey event)
            (.-shiftKey event))
      (do
        (dispatch [:db/set [:parenoia :editable?] false])
        (.preventDefault event)
        (println "ctrl-shift-z")
        (dispatch [:parenoia/redo])))))

(defn on-command-s-fn [zloc]
  (fn [^js event]
    (when (and (check-key event "s")
            (.-metaKey event))

      (do
        (.preventDefault event)
        (println "ctrl-shift-s")
        (dispatch [:parenoia/save!])))))

(defn on-m-fn [zloc]
  (fn [^js event]
    (when (check-key event "m")

      (do
        (.preventDefault event)
        (let [menu? @(subscribe [:db/get [:parenoia :menu?]])]
          (dispatch [:db/set [:parenoia :menu?] (not menu?)]))))))

(defn on-l-fn [zloc]
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "l"))
      (do
        (.preventDefault event)
        (let [the-def   @(subscribe [:db/get [:parenoia :definition]])
              {:keys [uri row col]} the-def]
          (when uri (dispatch [:parenoia/go-to! (utils/uri->path uri) [row col]])))))))

(defn on-g-fn [zloc]
  (fn [^js event]
    (when (check-key event "g")
      (do
        (.preventDefault event)
        (let [project-map? @(subscribe [:db/get [:parenoia :project-map?]])]
          (dispatch [:db/set [:parenoia :project-map?] (not project-map?)]))))))
(defn on-f-fn [zloc]
  (fn [^js event]
    (when (and (without-special-keys event) (check-key event "f"))
      (.preventDefault event)
      (dispatch [:parenoia/toggle-global-search!]))))

(defn effect [zloc]
  (let [on-left            (on-left-fn zloc)
        on-right           (on-right-fn zloc)
        on-up              (on-up-fn zloc)
        on-down            (on-down-fn zloc)
        on-tab             (on-tab-fn zloc)
        on-shift-tab       (on-shift-tab-fn zloc)
        on-space           (on-space-fn zloc)
        on-shift-space     (on-shift-space-fn zloc)
        on-shift-enter     (on-shift-enter-fn zloc)
        on-enter           (on-enter-fn zloc)
        on-backspace       (on-backspace-fn zloc)
        on-shift-backspace (on-shift-backspace-fn zloc)
        on-esc             (on-esc-fn zloc)
        on-command-z       (on-command-z-fn zloc)
        on-command-shift-z       (on-command-shift-z-fn zloc)
        on-command-s       (on-command-s-fn zloc)
        on-m               (on-m-fn zloc)
        on-g               (on-g-fn zloc)
        on-q               (on-q-fn zloc)
        on-w               (on-w-fn zloc)
        on-e               (on-e-fn zloc)
        on-r               (on-r-fn zloc)
        on-a               (on-a-fn zloc)
        on-s               (on-s-fn zloc)
        on-d               (on-d-fn zloc)
        on-l               (on-l-fn zloc)
        on-f (on-f-fn zloc)
        on-shift-a         (on-shift-a-fn zloc)
        on-shift-d         (on-shift-d-fn zloc)]
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
        (add-listener js/document on-l)
        (add-listener js/document on-f)
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
          (remove-listener js/document on-l)
          (remove-listener js/document on-f)
          (remove-listener js/document on-shift-a)
          (remove-listener js/document on-shift-d)))
      #js [zloc])))

(defn block-some-keyboard-events [^js e]
  (when (or
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
          (check-key e "d")
          (check-key e "l")
          (check-key e "f"))
    (.stopPropagation e)))

