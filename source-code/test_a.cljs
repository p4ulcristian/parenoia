(ns test-a
  (:require [cljs.reader :refer [read-string]]))

(def x 45) (defn hello [a]
             (let [x 4]
               (+ x x))) (defn hello2 [] (x hello))


