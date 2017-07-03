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
         :students/loading false
         :student/by-id {0 {:student/id 0
                            :student/name "John"
                            :student/parent [:parent/by-id 0]
                            :student/friend [:student/by-id 1]}
                         1 {:student/id 1
                            :student/name "Sally"
                            :student/friend [:student/by-id 0]}}
         :parent/by-id {0 {:parent/id 0
                           :parent/name "Harry"}}})))


(defn remote-fn
  [{:keys [remote blah]} cb]
  (when remote
    (doseq [query remote]
      (http-fn query cb)))
  (when blah
    (js/console.log "blah remote!")))
