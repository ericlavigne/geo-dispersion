(ns geo-dispersion.location
  (:use [clojure.contrib.def :only (defn-memo)])
  (:require [clj-http.client :as client])
  (:require [clojure.contrib.json :as json])) 

; Raw HTTP response from geosearch for address like
; "1600+Pennsylvania+Avenue,+Washington,+DC".
; :body is in JSON format.
(defn-memo raw-geosearch [address]
  (client/get "http://where.yahooapis.com/geocode"
	      {:query-params {"q" address, 
			      "appid" "pItVa84o", 
			      "flags" "J"}}))

(defn geosearch [address]
  (if (.matches address "[0-9\\-]*") nil ; The researcher may disagree with Yahoo about what location the number "3" refers to.
      (let [raw-result (raw-geosearch address)]
        (if (= 200 (:status raw-result))
          (:Results (:ResultSet (json/read-json (:body raw-result))))
          (throw (Exception. (str "Geosearch failed for address: " address)))))))


(defn show [location]
  (apply str (interpose ", "
                        (filter #(> (count %) 0)
                                (map #(% location)
                                     [:line1 :line2 :line3 :line4])))))

(defn degrees-to-radians
  "radians = degrees * 2pi / 360"
  [degrees]
  (* degrees Math/PI (/ 2.0 360)))


(defn distance
  "Calculate distance(km) between lat1,long1,lat2,long2 (radians) or loc1,loc2 (yahoo api format)"
  ([lat1 long1 lat2 long2]
     (let [R 6371] ; km
       (* R
          (Math/acos (+ (* (Math/sin lat1) (Math/sin lat2))
                        (* (Math/cos lat1) (Math/cos lat2) (Math/cos (- long2 long1))))))))
  ([{lat1 :latitude long1 :longitude} {lat2 :latitude long2 :longitude}] ; Yahoo API format - lat1,lat2,long1,long2 are stringified degrees.
     (try (apply distance
                 (map #(degrees-to-radians (Double/parseDouble %))
                      [lat1 long1 lat2 long2]))
          (catch Exception _ nil))))
