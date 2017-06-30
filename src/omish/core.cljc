(ns omish.core
  (:refer-clojure :exclude [read])
  (:require
   #?@(:clj [[clojure.core.async :as a]
             [clojure.spec.alpha :as s]
             [clojure.test.check.generators :as gen]]
       :cljs [[cljs.core.async :as a]
              [cljs.spec.alpha :as s]
              [clojure.test.check.generators :as gen]]))
  #?(:cljs
     (:require-macros
      [cljs.core.async.macros :as a])))

#_(s/def ::mutate
  (s/with-gen symbol?
    #(s/gen #{'sjp/add-foo 'sjp/add-derp})))

#_(s/def :mutate/txs (s/coll-of ::mutate))

(def state
  (atom {:sjps [[:sjp/by-id 0]
                [:sjp/by-id 1]] ;; a list of IDs
         :selected-sjp nil ;; an ID
         :sjp/by-id {0 {:foo nil
                        :id 0}
                     1 {:foo nil
                        :id 1}}
         :user/by-name {}}))


(defn merge!
  [{:keys [state merge-tree] :as env} novelty]
  (swap! state merge-tree novelty))

(defmulti read (fn [_ k _] k))

(defmethod read :default
  [{:keys [state]} k _]
  (let [st @state]
    {:value (get st k)}))

(defmethod read :sjps
  [{:keys [state]} k _]
  (let [st @state
        sjp-idents (get st k)]
    {:value (if (some? sjp-idents)
              (mapv
               (fn [sjp-ident]
                 ;; sjp-ident [:sjp/by-id 0]
                 (get-in st sjp-ident))
               sjp-idents)
              "Loading SJPs")
     :remote (nil? sjp-idents)}))

(defmulti mutate (fn [_ k _] k))

(defmethod mutate 'sjp/add-foo
  [{:keys [state]} _ {:keys [sjp/id] :as params}]
  (let [st @state]
    {:local (fn [] (swap! state assoc-in
                          [:sjp/by-id id :foo]
                          "bar"))
     :remote params}))

(defmethod mutate 'sjp/add-derp
  [_ _ _]
  {:local identity
   :remote nil})

(defn make-parser
  "Creates a parser that, when called, will appropriately respond to events"
  [{:keys [mutate read]} remote-fn]
  (fn parser-fn
    ([env txs]
     (parser-fn env txs false))
    ([{:keys [state] :as env} txs enable-remote?]
     ;; fucking doseq will return nil
     (doseq [[tx-key tx-params] txs]
       (cond
         (keyword? tx-key) (let [{:keys [value remote]} (read env tx-key tx-params)]
                             (println "hello?" value)
                             (when (and remote enable-remote?)
                               (remote-fn env tx-key tx-params))
                             value)
         (symbol? tx-key)  (let [{:keys [local remote]} (mutate env tx-key tx-params)]
                             (println @state)
                             (local)
                             (println @state)
                             (when (and remote enable-remote?)
                               (remote-fn [tx-key tx-params]
                                          (fn cb [novelty]
                                            (merge! state novelty)
                                            (parser-fn env [tx-key tx-params])))
                               #_(remote-fn env tx-key tx-params)))
         :else             (throw (str "tx-key is an invalid data type" tx-key)))
       ))))

(defmulti remote-fn (fn [[k _] _] k))

(defmethod remote-fn :sjps
  [[_ params] cb]
  (println "sjps")
  
  )

(defmethod remote-fn 'sjp/add-foo
  [[_ params] cb]
  (println "fooooo")
  (cb {:sjps [[:sjp/by-id 0]]
       :sjp/by-id {0 {:hi 1}}}))
#_(defn remote-fn
  [key params]
  (case key
    :foo->7 (a/go
              (a/<! (a/timeout 1000))
              (println "supplies!" params))
    :bar->5 (a/go
              (a/<! (a/timeout 1000))
              (println "shiznit" params))))


(def parser (make-parser {:mutate mutate
                          :read read} remote-fn))


(defn dispatch!
  "Equivalent to calling the parser but allowing remotes to happen"
  [{:keys [parser] :as env} txs]
  (parser env txs true))


(defn merge-tree
  "Merges when it encounters map, replaces otherwise."
  [old new]
  (if (and (map? old) (map? new))
    (merge-with
     (fn [old-val new-val]
       (merge-tree old-val new-val))
     old
     new)
    new))


(def env
  {:state state
   :parser parser
   :merge-tree merge-tree
   :merge! merge!})
