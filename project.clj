(defproject geo-dispersion "1.0" 
  :description "Calculate geographic dispersion index for personal networks"
  :main geo-dispersion.core
  :aot [geo-dispersion.core]
  :dependencies [[org.clojure/clojure "1.2.0"] 
		 [org.clojure/clojure-contrib "1.2.0"]
		 [clj-http "0.1.1"]
                 [clojure-csv "1.2.0"]
		 [com.h2database/h2 "1.2.147"]
		 [compojure "0.5.1"]
		 [hiccup "0.3.0"]
                 [opencsv-clj "1.1.0"]
		 [ring/ring-devel "0.3.0"]
		 [ring/ring-jetty-adapter "0.3.0"]
		 [ring/ring-servlet "0.3.0"]]
  :dev-dependencies [[lein-run "1.0.0"]]
  :jvm-opts ["-Xmx1g"])
