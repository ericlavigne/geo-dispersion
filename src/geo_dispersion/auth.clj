(ns geo-dispersion.auth
  (:use compojure.core
        hiccup.core
        hiccup.page-helpers
        geo-dispersion.views
        [sandbar.forms :as forms]))

(forms/defform new-account-form "/new-account"
  :fields[(forms/hidden :id)
          (forms/textfield :username)
          (forms/password :password)]
  :buttons [[:save] [:cancel]]
  :title (fn [type] "Create new account")
  :on-cancel "/"
  :on-success (fn [req]
                "/"))

(defn new-account-view [req form]
  (layout {}
          (list [:p "Please select a username and password for your new account."]
                form)))

(defroutes auth-routes
  (new-account-form new-account-view)
  (GET "/login" [] (layout {} "Login")))

