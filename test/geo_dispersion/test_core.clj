(ns geo-dispersion.test-core
  (:use [geo-dispersion.core] :reload)
  (:use [clojure.test]))

(deftest test-geosearch
  (is (= "Alachua County"
	 (:county (first (geosearch "Gainesville, FL"))))
      "Gainesville, FL is in Alachua County."))
