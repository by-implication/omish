(ns example.students.state)

(defn make-derivative-spec
  [state]
  {
   :db [[] state]
   :route [[:db] (fn [db] (:route db))]
   })
