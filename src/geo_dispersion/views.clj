(ns geo-dispersion.views
  (:use hiccup.core
	hiccup.page-helpers)) 

(defn layout [options content]
  (xhtml {:lang "en"}
	 [:head
	  [:meta {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
	  [:title (or (:title options) "Geo-Dispersion")]]
	 [:body
          content
          [:p (str "Molina, JL, McCarty, Christopher & Eric Lavigne (2010). "
                   "Utility for calculating the geographical dispersion index "
                   "of personal networks collected with EgoNet. "
                   "Grant: MICINN CSO2009-07057 - Perfiles del "
                   "Empresariado \u00c9tnico en Espa\u00f1a (ITINERE).")]
	  [:p (link-to "/jquery-ui-example.html" "jQuery UI example")]
          [:p (link-to "/encoding-test" "UTF-8 Encoding Test")]]))

(defn encoding-test [text]
  (layout {:title "Testing character encoding..."}
          [:form {:action "/encoding-test" :method "POST"}
	   [:textarea {:name "text" :rows "20" :cols "70"} text]
	   [:br]
	   [:input {:type "Submit" :value "Submit"}]
	   [:p text]]))

(defn index []
  (layout {}
          [:p "Front Page"]))
