(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies
 '[
   ;; Clojure stuff
   [org.clojure/clojure           "1.9.0-alpha17"]
   [org.clojure/clojurescript     "1.9.562"]
   [org.clojure/core.async        "0.3.442"]
   [org.clojure/test.check        "0.9.0"]

   ;; Boot setup
   [adzerk/boot-cljs              "2.0.0"]
   [adzerk/boot-cljs-repl         "0.3.3"]
   [com.cemerick/piggieback       "0.2.1"  :scope "test"]
   [weasel                        "0.7.0"  :scope "test"]
   [org.clojure/tools.nrepl       "0.2.12" :scope "test"]
   [adzerk/boot-reload            "0.5.1"]
   [pandeiro/boot-http            "0.8.0"]

   ;; App dependencies
   [com.rpl/specter               "1.0.2"]
   ])

(load-data-readers!)

(require
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[pandeiro.boot-http :refer [serve]])

(deftask build-dev
  []
  (comp
   (notify :title "PAKSHET"
           :visual true
           :audible true)
   (cljs :source-map true
         :optimizations :none
         ;; :compiler-options {:devcards true}
         )))

(deftask run-dev
  []
  (comp
   (watch)
   (reload)
   (cljs-repl)
   (build-dev)
   (serve :reload true
          :port 9000)))
