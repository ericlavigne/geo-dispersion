(ns geo-dispersion.test-core
  (:use [geo-dispersion.core] :reload)
  (:use [clojure.test])
  (:use [clojure-csv.core :only (parse-csv write-csv)]))

(deftest test-geosearch
  (is (= "Alachua County"
	 (:county (first (geosearch "Gainesville, FL"))))
      "Gainesville, FL is in Alachua County."))

(deftest csv-dependency
  (let [text (slurp "examples/small-disperse.csv")
        matrix (parse-csv text)
        heading1 ["EgoID" "egocat" "egohome" "egodog"
                  "AlterID" "altercat" "alterdog" "alterhome" "alterfish"]]
    (is (= heading1 (first matrix))
        "Heading matches expectation.")
    (is (every? empty? (nth matrix 11))
        "12th line is separator between alter and alter pair sections.")
    (is (= matrix (take (count matrix) (parse-csv (write-csv matrix))))
        "parse-csv and write-csv are inverses, except for a newline at the end.")))

