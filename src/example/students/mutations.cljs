(ns example.students.mutations)

(defmulti mutate (fn [_ k _] k))


(defmethod mutate 'students/get
  [{:keys [state]} _ _]
  {:local (fn []
            (swap! state assoc :students/loading true))
   :remote true
   :some-other-server true
   :blah {:circus "lion"
          :name "greymane"}})


(defmethod mutate 'blah
  [_ _ _]
  {:some-other-server true})
