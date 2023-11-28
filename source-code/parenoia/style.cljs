(ns parenoia.style)

;(def box-shadow "rgb(0,0,0,0.4) 3px 3px 6px 0px inset, rgba(0,0,0,0.4) 3px 3px 6px 0px inset")

(def box-shadow "rgba(0, 0, 0, 0.6) 0px 3px 8px")

(def colors {:vector {:background-color "#1E91D6"
                      :text-color "white"}
             :map {:background-color "#FFF68D"
                   :text-color "black"}
             :ns  {:background-color "#7891D9"
                   :text-color "black"}
             :defn {:background-color "#7891D9"
                   :text-color "black"}
             :function {:background-color "#55D6BE"
                        :text-color "black"}
             :if {:condition-color :orange
                  :false-color :red
                  :true-color :lightgreen}
             :cond {:condition-color :orange
                    :eval-color :lightgreen}
             :symbol? {:background-color "lightblue"
                       :text-color "#333"}
             :keyword? {:background-color "#FFBF00"
                       :text-color "#333"}
              :string? {:background-color "lightgreen"
                       :text-color "#333"}
              :selection {:background-color "linear-gradient(to right, #eea2a2 0%, #bbc1bf 19%, #57c6e1 42%, #b49fda 79%, #7ac5d8 100%)"
                          :text-color "#333"}
              :same-as-selection {:background-color "white"
                                  :text-color "#333"}
              :unused-binding {:background-color "#333"
                                :text-color "#AAA"}
               :deref {:background-color "maroon"
                                :text-color "white"}
               :reader-macro {:background-color "purple"
                                :text-color "#AAA"}})

(defn color [path] (get-in colors path))

