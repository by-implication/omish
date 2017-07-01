(ns example.students.state)

(defn make-derivative-spec
  [state]
  {
   :db [[] state]
   :app/students [[:db] (fn [db] (mapv (partial get-in db)
                                       (:app/students db)))]
   })
