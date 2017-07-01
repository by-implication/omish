(ns example.students.remotes)

(defmulti http-fn (fn [[k _] _] k))

(defmethod http-fn 'students/get
  [_ cb]
  (js/console.log "get students"))

(defn remote-fn
  [{:keys [remote]} cb]
  (when remote
    (js/console.log "remote" remote)
    #_(http-fn remote cb)))
