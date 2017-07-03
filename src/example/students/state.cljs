(ns example.students.state)

(defn make-derivative-spec
  [state]
  {
   :db               [[] state]
   :students/loading [[:db] (fn [db] (:students/loading db))]
   :app/students     [[:db] (fn [db]
                              (mapv (fn [student-ident]
                                      (let [student (get-in db student-ident)]
                                        (update student
                                                :student/friend #(get-in db %)
                                                :student/parent (fn [parent-ident]
                                                                  (when (some? parent-ident)
                                                                    (get-in db parent-ident))))))
                                    (:app/students db)))]
   })
