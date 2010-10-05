(ns geo-dispersion.core
  (:require [clj-http.client :as client])
  (:require [clojure.contrib.json :as json]))

; Raw HTTP response from geosearch for address like
; "1600+Pennsylvania+Avenue,+Washington,+DC".
; :body is in JSON format.
(defn raw-geosearch [address]
  (client/get "http://where.yahooapis.com/geocode"
	      {:query-params {"q" address, 
			      "appid" "pItVa84o", 
			      "flags" "J"}}))

(defn geosearch [address]
  (let [raw-result (raw-geosearch address)]
    (if (= 200 (:status raw-result))
      (:Results (:ResultSet (json/read-json (:body raw-result))))
      (throw (Exception. (str "Geosearch failed for address: " address))))))

; Will be working with CSV files next. See example in example folder. The relevant functions
; are (parse-csv "1,2,3\n4,5,6\n") and (write-csv [["1" "2" "3"] ["4" "5" "6"]])
; in clojure-csv.core. Note that the argument to write-csv must be sequence of sequences of strings.

; Also see description of work in doc folder.

