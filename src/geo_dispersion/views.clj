(ns geo-dispersion.views
  (:use hiccup.core
	hiccup.page-helpers)) 

(defn layout [options content]
  (let [title (or (:title options) "Geo-Dispersion")
        sub-title (or (:sub-title options) "Measuring geographic distances and dispersion in social network data")]
    (xhtml {:lang "en"}
           [:head
            [:meta {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
            [:title (str title " - " sub-title)]]
           [:body
            [:h1 title]
            [:h2 sub-title]
            content
            [:p (str "Molina, JL, McCarty, Christopher & Eric Lavigne (2010). "
                     "Utility for calculating the geographical dispersion index "
                     "of personal networks collected with EgoNet. "
                     "Grant: MICINN CSO2009-07057 - Perfiles del "
                     "Empresariado \u00c9tnico en Espa\u00f1a (ITINERE).")]
            [:p (link-to "/jquery-ui-example.html" "jQuery UI example")]
            [:p (link-to "/encoding-test" "UTF-8 Encoding Test")]])))

(defn encoding-test [text]
  (layout {:title "Testing character encoding..."}
          [:form {:action "/encoding-test" :method "POST"}
	   [:textarea {:name "text" :rows "20" :cols "70"} text]
	   [:br]
	   [:input {:type "Submit" :value "Submit"}]
	   [:p text]]))

(defn index []
  (layout {}
          [:div
           [:p (link-to "/new-account" "Create new account")]
           [:p (link-to "/login" "Log in")]]))
