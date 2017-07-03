(ns omish.core
  "An attempt to replicate om.next functionality without
  Ident, IQuery, and reads."
  (:refer-clojure :exclude [read])
  (:require
   [com.rpl.specter :as sp]
   #?@(:clj [[clojure.core.async :as a]
             [clojure.spec.alpha :as s]
             [clojure.spec.test.alpha :as stest]
             [clojure.test.check.generators :as gen]]
       :cljs [[cljs.core.async :as a]
              [cljs.spec.alpha :as s]
              [cljs.spec.test.alpha :as stest]
              [clojure.test.check.generators :as gen]
              [goog.object :as gobj]]))
  #?(:cljs
     (:require-macros
      [cljs.core.async.macros :as a]
      [com.rpl.specter :as sp])))


#?(:cljs (enable-console-print!))


;; Because we want to potentially support anything that could be a valid
;; defmulti dispatch value.
(s/def :query/key some?)


(s/def :query/params map?)


;; Note: all queries are mutates.
(s/def ::query
  (s/cat :key :query/key :params (s/? :query/params)))


(s/def ::conformed-query
  (s/keys :req-un [:query/key]
          :opt-un [:query/params]))


(s/def ::tx
  (s/coll-of ::query
             :kind vector?
             :into []))


(s/def ::local-mutates
  (s/coll-of fn?))


(s/def ::remote-key keyword?)


(s/def ::remote-keys (s/coll-of ::remote-key
                                :kind vector?
                                :into []))


(s/def ::env
  (s/keys :req-un [::state
                   ::parser]
          :opt-un [::merge!
                   ::merge-tree
                   ::remote-fn
                   ::remote-keys]))


(s/def ::parser-config
  (s/keys :req-un [::mutate]
          :opt-un [::mutate-local-key]))





(s/fdef make-parser
        :args (s/cat :parser-config ::parser-config)
        :ret (s/fspec :args (s/cat :env ::env
                                   :tx ::tx
                                   :remote-key ::remote-key)
                      :ret (s/or :local map?
                                 :remote (s/coll-of ::conformed-query))))


(s/fdef merge-tree
        :args (s/cat :old some? :new some?)
        :ret some?)


(s/fdef merge!
        :args (s/cat :env ::env :novelty some?)
        :ret some?)


(defn merge!
  [{:keys [state merge-tree] :as env} novelty]
  (swap! state merge-tree novelty))


(defn make-parser
  "Creates a parser that, when called, will appropriately respond to events"
  [{:keys [mutate mutate-local-key] :or {mutate-local-key :local}}]
  (fn parser-fn
    ([env tx] (parser-fn env tx nil))
    ;; When remote-key is specified, should return the remote
    ([{:keys [state] :as env} tx remote-key]
     (let [remote-cb    (partial merge! env)
           conformed-tx (s/conform ::tx tx)
           xq->parsed   (fn [{:keys [key params] :as xq}]
                          (mutate env key params))]
       (if (some? remote-key)
         ;; doesn't apply local mutations
         (mapv (fn [xq]
                 (let [parsed (xq->parsed xq)
                       remote (get parsed remote-key)]
                   (if (true? remote)
                     xq
                     remote)))
               conformed-tx)
         ;; applies local mutations but does not return
         ;; remote information
         (let [local-mutates (mapv (comp mutate-local-key
                                         xq->parsed)
                                   conformed-tx)]
           (assert (s/valid? ::local-mutates local-mutates)
                   (s/explain ::local-mutates local-mutates))
           (doseq [local-mutate local-mutates]
             (local-mutate))
           @state))))))


(defn gather-remotes
  [{:keys [parser] :as env} tx remote-keys]
  (into {}
        (comp
         ;; Run the parser given the query, and passing the remote
         (map #(vector % (parser env tx %)))
         ;; But only if there are entries for that remote.
         (filter (fn [[_ v]] (pos? (count v)))))
        remote-keys))


(defn transact!
  "Equivalent to calling the parser but allowing remotes to happen"
  [{:keys [parser remote-keys remote-fn merge!] :or {remote-keys [:remote]} :as env} tx]
  (let [res             (parser env tx)
        pending-remotes (gather-remotes env tx remote-keys)
        remote-cb       (partial merge! env)]
    (when (seq pending-remotes)
      ;; #?(:cljs (js/console.log pending-remotes))
      (remote-fn pending-remotes remote-cb))
    res))


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


(def derivatives-child-context-types
  "Child Context Types of Martin Klepsch's Derivatives
   for merging with our own stuff because things get overridden.
   Ditto for pushy history"
  #?(:cljs
     {"org.martinklepsch.derivatives/get"
      js/React.PropTypes.func
      "org.martinklepsch.derivatives/release"
      js/React.PropTypes.func
      "pushy/history"
      js/React.PropTypes.object}))


(defn rum-omish-env
  "Returns a Rum mixin for associating
  the omish environment to child context.

  Merges derivatives child context types because
  Rum naively merges class-properties."
  [env]
  ;; because we're passing the environment down to children
  ;; consider removing the :state.
  ;; Also, for convenience, add a :dispatch key with the event-chan closed over.
  #?(:cljs
     {:will-mount       (fn [state]
                          (assoc state :env env))
      :will-unmount     (fn [state]
                          (dissoc state :env))
      :class-properties {:childContextTypes
                         (assoc derivatives-child-context-types
                                "omish/env"
                                js/React.PropTypes.object)}
      :child-context    (fn [_] {"omish/env" env})}))


(defn rum-omish-sub
  "Returns a Rum mixin for associating
  the omish environment to local component state
  from the child context.

  Merges derivatives child context types because
  Rum naively merges class-properties."
  ([]
   (rum-omish-sub nil))
  ([will-unmount]
   #?(:cljs
      {:class-properties {:contextTypes (assoc derivatives-child-context-types
                                               "omish/env"
                                               js/React.PropTypes.object)}
       :will-mount       (fn [state]
                           (let [env (-> state
                                         :rum/react-component
                                         (gobj/get "context")
                                         (gobj/get "omish/env"))]
                             (assert env "No omish env found in component context.")
                             ;; should probably change implementation again to avoid js->clj
                             (assoc state :omish/env (js->clj env :keywordize-keys true))))
       :will-unmount     (fn [state]
                           (-> ((or will-unmount identity) state)
                               (dissoc :omish/env)))})))
