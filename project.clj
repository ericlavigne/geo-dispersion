(defproject geo-dispersion "1.0" 
  :description "Calculate geographic dispersion index for personal networks"
  :main geo-dispersion.core
  :aot [geo-dispersion.core]
  :dependencies [[org.clojure/clojure "1.2.0"] 
		 [org.clojure/clojure-contrib "1.2.0"]
		 [clj-http "0.1.1"]
                 [clojure-csv "1.2.0"]]
  :dev-dependencies [[lein-run "1.0.0"]])
