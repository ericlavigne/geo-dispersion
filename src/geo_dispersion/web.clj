(ns geo-dispersion.web
  (:use compojure.core
	hiccup.core
	hiccup.page-helpers
	ring.adapter.jetty
	ring.util.response)
  (:require [compojure.route :as route]))

(defn index-view [text]
  (xhtml {:lang "en"}
	 [:head
	  [:meta {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
	  [:title "Testing character encoding..."]]
	 [:body
	  [:form {:action "/" :method "POST"}
	   [:textarea {:name "text" :rows "20" :cols "70"} text]
	   [:br]
	   [:input {:type "Submit" :value "Submit"}]
	   [:p text]]]))

(defroutes main-routes
  (GET "/" [] (index-view "\u00e9"))
  (POST "/" [text] (index-view text))
  (route/not-found "<h1>Page not found</h1>"))

(defn wrap-utf8 [app]
  (fn [req]
    (content-type (app req) "text/html; charset=UTF-8")))

(def app (-> #'main-routes
	     (wrap-utf8)))

; lein run geo-dispersion.web run-server
(defn run-server []
  (run-jetty app {:port 8080}))
