(ns omish.core
  (:refer-clojure :exclude [read])
  (:require
   [com.rpl.specter :as sp]
   #?@(:clj [[clojure.core.async :as a]
             [clojure.spec.alpha :as s]
             [clojure.test.check.generators :as gen]]
       :cljs [[cljs.core.async :as a]
              [cljs.spec.alpha :as s]
              [clojure.test.check.generators :as gen]
              [goog.object :as gobj]]))
  #?(:cljs
     (:require-macros
      [cljs.core.async.macros :as a]
      [com.rpl.specter :as sp])))


#?(:cljs (enable-console-print!))


(s/def :tx/params map?)


(s/def :tx/mutate
  (s/and list?
         (s/cat :key symbol? :params (s/? :tx/params))))


;; Try not to get carried away and implement om.next/datomic's query syntax
(s/def :tx/read
  (s/cat :key keyword? :params (s/? :tx/params)))


(s/def ::tx
  (s/or :mutate :tx/mutate
        :read :tx/read))


(s/def ::txs
  (s/coll-of ::tx
             :kind vector?
             :into []))


(s/def ::local-muts
  (s/coll-of fn?))


(defn merge!
  [{:keys [state merge-tree] :as env} novelty]
  (swap! state merge-tree novelty))


(defn make-parser
  "Creates a parser that, when called, will appropriately respond to events"
  [{:keys [mutate read mutate-local-key read-value-key]
    ;; defaults
    :or   {mutate-local-key :local
           read-value-key   :value}}]
  (fn parser-fn
    ([env txs] (parser-fn env txs false))
    ([{:keys [state remote-fn remote-keys merge! merge-tree] :or {remote-keys [:remote]} :as env}
      txs
      enable-remote?]
     (let [remote-cb     (partial merge! env)
           conformed-txs (s/conform ::txs txs)
           readmutate    {:read read :mutate mutate}
           parseds       (mapv
                          (fn [[method {:keys [key params] :as expanded-tx}]]
                            [expanded-tx ((readmutate method) env key params)])
                          conformed-txs)
           local-muts    (into []
                               (comp (map (comp mutate-local-key second))
                                     (remove nil?))
                               parseds)
           _             (assert (s/valid? ::local-muts local-muts)
                                 (s/explain ::local-muts local-muts))
           values        (reduce
                          (fn [acc [expanded-tx parsed]]
                            (if-let [value (get parsed read-value-key)]
                              (assoc acc key value)
                              acc))
                          {}
                          parseds)
           remotes       (reduce
                          (fn [acc remote-key]
                            (let [txs-with-remote (into []
                                                        (comp (filter
                                                               (fn [[expanded-tx parsed]]
                                                                 (contains? parsed remote-key)))
                                                              (map first))
                                                        parseds)]
                              (if (seq txs-with-remote)
                                (assoc acc remote-key (vec txs-with-remote))
                                acc)))
                          {}
                          remote-keys)]
       (doseq [local-mut local-muts]
         (local-mut))
       (when (seq remotes)
         (remote-fn remotes remote-cb))
       values))))


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
   an event channel to child context.
   Properly mounts and unmounts itself.

   Takes a mutation function which transforms state.
   State is the db atom itself, and the mutate functions
   can decide whether or not to modify the app state.

   Optionally takes an environment map.
   Required keys are: event-chan, mutate, and state."
  [env]
  ;; because we're passing the environment down to children
  ;; consider removing the :state.
  ;; Also, for convenience, add a :dispatch key with the event-chan closed over.
  #?(:cljs
     {:will-mount       (fn [state]
                          (assoc state :env env))
      :will-unmount     (fn [state]
                          (dissoc state :env))
      :class-properties {:childContextTypes (assoc derivatives-child-context-types
                                                   "omish/env"
                                                   js/React.PropTypes.object)}
      :child-context    (fn [_] {"omish/env" env})}))


(defn rum-omish-sub
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
                           (-> (will-unmount state)
                               (dissoc :omish/env)))})))
