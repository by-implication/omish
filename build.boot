(def +version+ "0.0.1")

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
   [adzerk/bootlaces              "0.1.13" :scope "test"]
   [com.cemerick/piggieback       "0.2.1"  :scope "test"]
   [weasel                        "0.7.0"  :scope "test"]
   [org.clojure/tools.nrepl       "0.2.12" :scope "test"]
   [adzerk/boot-reload            "0.5.1"]
   [pandeiro/boot-http            "0.8.0"]

   ;; App dependencies
   [com.rpl/specter               "1.0.2"]
   [rum                           "0.10.8"]
   [org.martinklepsch/derivatives "0.2.0"]

   ;; others
   [binaryage/devtools            "0.9.4" :scope "test"]
   ])

(load-data-readers!)

(require
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[adzerk.bootlaces            :as bootlaces]
 '[pandeiro.boot-http :refer [serve]])

(bootlaces/bootlaces!
 +version+
 ;; If not specified, bootlaces will set
 ;; resource-paths to `#{"src"}`
 :dont-modify-paths? true)

(task-options!
 pom {:project 'com.byimplication/omish
      :version +version+
      :description "derp"
      :url "https://github.com/by-implication/omish"
      :scm {:url "https://github.com/by-implication/omish"}
      :license {"name" "Eclipse Public License"
                "url"  "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask build-jar []
  ;; Necessary to expose other namespaces to checkouts
  (set-env! :resource-paths #{"src"})
  (bootlaces/build-jar))

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
