(ns geo-dispersion.routes
  (:use compojure.core
        ring.util.response
        sandbar.stateful-session)
  (:require [compojure.route :as route]
            [geo-dispersion.views :as views]))

(defn apply-utf8 [content]
  (content-type content "text/html; charset=UTF-8"))

(defn wrap-utf8 [app]
  (fn [req]
    (let [resp (app req)]
      (if (or (nil? resp) (= :next req))
	resp
	(let [headers (:headers resp)
	      resp-type (if headers (headers "Content-Type") nil)]
	  (if (or (nil? resp-type)
		  (.contains resp-type "text"))
	    (content-type resp "text/html; charset=UTF-8")
	    resp))))))

(def text-routes
     (-> (routes
          (GET "/" [] (views/index))
          (GET "/new-account" [] (views/layout {} "New account"))
          (GET "/login" [] (views/layout {} "Login"))
	  (GET "/encoding-test" [] (views/encoding-test "\u00e9"))
	  (POST "/encoding-test" [text] (views/encoding-test text))
	  (ANY "*" [] :next))
	 (wrap-utf8)))

(defroutes combined
  text-routes
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))

(def main (-> combined
              wrap-stateful-session))
