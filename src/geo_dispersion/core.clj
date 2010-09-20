(ns geo-dispersion.core
  (:require [clj-http.client :as client]))

; example of yahoo api request, returning json result
(client/get "http://where.yahooapis.com/geocode"
	    {:query-params 
	     {"q" "1600+Pennsylvania+Avenue,+Washington,+DC", 
	      "appid" "pItVa84o", "flags" "J"}})
