(ns geo-dispersion.web
  (:use ring.adapter.jetty)
  (:require [geo-dispersion.routes :as routes]))

; lein run geo-dispersion.web main
(defn main []
  (run-jetty routes/main {:port 8080}))
