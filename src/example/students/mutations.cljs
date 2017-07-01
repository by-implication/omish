(ns example.students.mutations)

(defmulti mutate (fn [_ k _] k))


(defmethod mutate 'students/get
  [_ _ _]
  {:local identity
   :remote true})
