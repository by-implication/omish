(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies
 '[
   ;; Clojure stuff
   [org.clojure/clojure           "1.9.0-alpha17"]
   [org.clojure/clojurescript     "1.9.671"]
   [org.clojure/core.async        "0.3.443"]
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
   [rum                           "0.10.8"]
   [org.martinklepsch/derivatives "0.2.0"]
   ])

(load-data-readers!)

(require
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[pandeiro.boot-http :refer [serve]])

(deftask run-dev
  []
  (comp
   (watch)
   (reload)
   (notify :title "PAKSHET"
           :visual true
           :audible true)
   (cljs-repl)
   (cljs :source-map true
         :optimizations :none)
   (serve :reload true
          :port 9000)))
