(defproject interdependency "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [lein-clique "0.1.2"]
                 [lacij "0.8.0"]
                 [aysylu/loom "0.4.2"]]
  :main ^:skip-aot interdependency.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
