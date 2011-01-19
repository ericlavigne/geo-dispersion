(ns geo-dispersion.views
  (:use hiccup.core
	hiccup.page-helpers)) 

(defn index [text]
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
