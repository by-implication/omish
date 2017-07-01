(ns example.students.remotes
  (:require
   [cljs.core.async :refer [chan <! timeout]])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

(defmulti http-fn (fn [{:keys [key]} _] key))

(defmethod http-fn 'students/get
  [_ cb]
  (go
    (<! (timeout 1000))
    (cb {:app/students [[:student/by-id 0]
                        [:student/by-id 1]]
         :student/by-id {0 {:student/id 0
                            :student/name "John"}
                         1 {:student/id 1
                            :student/name "Sally"}}})))


(defn remote-fn
  [{:keys [remote]} cb]
  (when remote
    (doseq [query remote]
      (http-fn query cb))))
