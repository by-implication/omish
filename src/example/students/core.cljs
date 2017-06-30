(ns example.students.core
  (:require
   [omish.core :as omish]
   [rum.core :as rum]))


(rum/defc example-index
  []
  [:div "hi"])

(defn run []
  (rum/mount (example-index) (js/document.getElementById "app")))
