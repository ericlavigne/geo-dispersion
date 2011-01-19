(ns geo-dispersion.test-location
  (:use [geo-dispersion.location] :reload)
  (:use [clojure.test]))

(deftest test-geosearch
  (is (= "Alachua County"
	 (:county (first (geosearch "Gainesville, FL"))))
      "Gainesville, FL is in Alachua County."))

(deftest test-distance-between-cities
  (let [calculated-km
        (distance (first (geosearch "Gainesville Florida"))
                  (first (geosearch "Melbourne Florida")))
        expected-km 239] ; approximate, since both cities are more than a few kilometers across
    (is (> calculated-km (* 0.95 expected-km))
        "Distance calculation between cities accurate to within 5%")
    (is (< calculated-km (* 1.05 expected-km))
        "Distance calculation between cities accurate to within 5%")))

