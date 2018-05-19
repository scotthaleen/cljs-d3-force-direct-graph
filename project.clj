(defproject haleen.fd.graph "0.1.0-SNAPSHOT"
  :description "d3 fd graph in clojurescript"
  :url "https://github.com/scotthaleen/cljs-d3-force-direct-graph"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.7.1"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/core.async  "0.4.474"]
                 [cljsjs/d3 "4.12.0-0"]]
  :plugins [[lein-figwheel "0.5.16"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]
  :source-paths ["src"]
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :figwheel {:on-jsload "haleen.fd.graph.core/on-js-reload"
                           :open-urls ["http://localhost:3449/index.html"]}
                :compiler {:main haleen.fd.graph.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/haleen/fd/graph.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/haleen/fd/graph.js"
                           :main haleen.fd.graph.core
                           :optimizations :advanced
                           :pretty-print false}}]}

  :figwheel {:http-server-root "public"
             :server-port 3449
             :server-ip "127.0.0.1"
             :css-dirs ["resources/public/css"]}
  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.9"]
                                  [figwheel-sidecar "0.5.16"]
                                  [cider/piggieback "0.3.1"]]
                   :source-paths ["src" "dev"]
                   :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                   :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                                     :target-path]}})
