(ns geo-dispersion.test-core
  (:use [geo-dispersion.core] :reload)
  (:use [clojure.test])
  (:use [clojure-csv.core :only (parse-csv write-csv)]))

(def example-file "examples/small-disperse.csv")
(def example-alter-head
     ["EgoID" "egocat" "egohome" "egodog"
      "AlterID" "altercat" "alterdog" "alterhome" "alterfish"])

(def example-alter-pair-head
     ["EgoID" "Alter1ID" "Alter2ID" "know" "like"])

(deftest csv-dependency
  (let [matrix (parse-csv (slurp example-file))]
    (is (= example-alter-head (first matrix))
        "Heading matches expectation.")
    (is (every? empty? (nth matrix 11))
        "12th line is separator between alter and alter pair sections.")
    (is (= matrix (take (count matrix) (parse-csv (write-csv matrix))))
        "parse-csv and write-csv are inverses, except for a newline at the end.")))

(deftest test-parse-network-data
  (let [parsed (parse-network-data example-file)]
    (is (= example-alter-head (:alter-head parsed))
        "Alter heading matches expectation.")
    (is (= example-alter-pair-head (:alter-pair-head parsed))
        "Alter pair heading matches expectation")))
