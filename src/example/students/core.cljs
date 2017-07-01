(ns example.students.core
  "This is an app for assigning students to classes."
  (:require
   [example.students.mutations :refer [mutate]]
   [example.students.remotes :as remotes]
   [example.students.state :as state]
   [omish.core :as omish]
   [org.martinklepsch.derivatives :as d]
   [rum.core :as rum]))


(def state (atom {:app/students [[:student/by-id 0]]
                  :student/by-id {0 {:student/id 0
                                     :student/name "John"}}}))


(def parser (omish/make-parser {:mutate mutate}))


(def env
  {:state state
   :parser parser
   :merge-tree omish/merge-tree
   :merge! omish/merge!
   :remote-fn remotes/remote-fn})


(rum/defcs students-list <
  rum/reactive
  (d/drv :app/students)
  (omish/rum-omish-sub)
  {:did-mount (fn [state]
                (let [{:keys [omish/env]} state]
                  (omish/dispatch! env `[(students/get)]))
                state)}
  [s]
  [:div (str (d/react s :app/students))])


(rum/defc app <
  (d/rum-derivatives (state/make-derivative-spec state))
  (omish/rum-omish-env env)
  []
  [:div (students-list)])


(defn run []
  (rum/mount (app) (js/document.getElementById "app")))


(run)
