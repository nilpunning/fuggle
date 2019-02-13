(defproject fuggle "0.2.6"
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/core.async "0.2.395"]
                 [com.stuartsierra/component "0.3.1"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [ring-server "0.4.0"]
                 [ring "1.5.0"]
                 [ring/ring-defaults "0.2.1"]
                 [metosin/ring-http-response "0.8.2"]
                 [amalloy/ring-gzip-middleware "0.1.3"]
                 [com.cemerick/friend "0.2.3"]
                 [cheshire "5.7.0"]
                 [org.postgresql/postgresql "9.4.1212"]
                 [yesql "0.5.3"]
                 [trptcolin/versioneer "0.2.0"]
                 [org.im4java/im4java "1.4.0"]
                 [com.amazonaws/aws-java-sdk-cloudformation "1.11.119"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.119"]
                 ; Fixed to resolve conflict between friend & amazonica
                 [org.apache.httpcomponents/httpclient "4.5.2"]
                 ; Fixed to resolve conflict between cljs & amazonica
                 [com.google.guava/guava "19.0"]
                 [org.clojure/clojurescript "1.9.293" :scope "provided"]
                 [rum "0.10.8"]
                 [garden "1.3.2"]
                 [bidi "2.0.14"]]
  :ring {:handler fuggle.handler/app}
  :min-lein-version "2.5.0"
  :main fuggle.server
  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]]
  :source-paths ["src/clj"
                 "src/cljc"]
  :cljsbuild {:builds {:app {:source-paths ["src/cljs" "src/cljc"]
                             :compiler     {:output-to     "resources/public/js/app.js"
                                            :output-dir    "resources/public/js/out"
                                            :asset-path    "/resources/js/out"
                                            :optimizations :none
                                            :pretty-print  true}}}}

  ; Uberjars uses advanced compilation so cljs and google closure source are unnecessary.
  ; Removed because they are very big.
  :uberjar-exclusions [#"goog/.*" #"cljsjs/.*" #"com/google/.*"]
  :uberjar-name "fuggle-standalone.jar"
  :profiles {:dev     {:repl-options {:init-ns fuggle.repl}
                       :dependencies [[ring/ring-mock "0.3.0"]
                                      [com.cemerick/piggieback "0.2.1"]
                                      [pjstadig/humane-test-output "0.8.1"]
                                      [org.clojure/test.check "0.9.0"]]
                       :source-paths ["env/dev/clj"]
                       :plugins      [[lein-figwheel "0.5.3"]
                                      [lein-cljsbuild "1.1.5"]
                                      [lein-ns-dep-graph "0.2.0-SNAPSHOT"]
                                      [lein-ancient "0.6.10"]
                                      [lein-count "1.0.3"]]
                       :injections   [(require 'pjstadig.humane-test-output)
                                      (pjstadig.humane-test-output/activate!)]
                       :figwheel     {:http-server-root "public"
                                      :server-ip        "0.0.0.0"
                                      :server-port      3449
                                      :nrepl-port       7002
                                      :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
                                      :css-dirs         ["resources/public/css"]
                                      :ring-handler     fuggle.handler/app
                                      :server-logfile   "target/figwheel_server.log"}
                       :cljsbuild    {:builds {:app {:source-paths ["env/dev/cljs"]
                                                     :compiler     {:main       "fuggle.dev"
                                                                    :source-map true}}}}}
             :uberjar {:source-paths ["env/prod/clj"]
                       :hooks       [leiningen.cljsbuild]
                       :aot         :all
                       :omit-source true
                       :cljsbuild   {:jar    true
                                     :builds {:app
                                              {:source-paths ["env/prod/cljs"]
                                               :compiler     {:optimizations :advanced
                                                              :pretty-print  false}}}}}})
