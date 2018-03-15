(defproject hacker-news "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [cljsjs/firebase "3.7.3-0"]
                 [cljsjs/moment "2.17.1-1"]
                 [reagent "0.6.2" :exclusions [cljsjs/react]]
                 [cljsjs/react-with-addons "15.4.2-2"]
                 ]
  :plugins [[lein-cljsbuild "1.1.6"]
            [lein-figwheel "0.5.8"]]
  :clean-targets ^{:protect false} [:target-path "out" "resources/public/cljs"]
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :figwheel true
                        :compiler {:main "hacker-news.core"
                                   :asset-path "cljs/out"
                                   :output-to "resources/public/cljs/main.js"
                                   :output-dir "resources/public/cljs/out"}}
                       {:id "min"
                        :source-paths ["src"]
                        :compiler {:output-to "resources/public/cljs/main.js"
                                   :optimizations :advanced}}]}
  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.8"]]
                   :plugins [[cider/cider-nrepl "0.15.0-SNAPSHOT"]]
                   :source-paths ["dev"]}}
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :figwheel {:css-dirs ["resources/public/css"]})
