(ns example.students.mutations)

(defmulti mutate (fn [_ k _] k))
