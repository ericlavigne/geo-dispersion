(ns geo-dispersion.routes
  (:use compojure.core
	ring.util.response)
  (:require [compojure.route :as route]
	    [geo-dispersion.views :as views]))

(defn wrap-utf8 [app]
  (fn [req]
    (content-type (app req) "text/html; charset=UTF-8")))

(defroutes main
  (GET "/" [] (views/index "\u00e9"))
  (POST "/" [text] (views/index text))
  (route/not-found "<h1>Page not found</h1>"))

(def app (-> #'main
	     (wrap-utf8)))
