(ns example.students.remotes
  (:require
   [cljs.core.async :refer [chan <! timeout]])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

(defmulti http-fn (fn [{:keys [key]} _] key))

(defmethod http-fn 'students/get
  [_ cb]
  (go
    (<! (timeout 400))
    (cb {:app/students [{:student/id 1}
                        {:student/id 0}]})))

(defn remote-fn
  [{:keys [remote]} cb]
  (when remote
    (doseq [query remote]
      (http-fn query cb))))
