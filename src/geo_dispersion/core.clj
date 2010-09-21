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

