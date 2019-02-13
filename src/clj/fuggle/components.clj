(ns fuggle.components
  (:require [ring.util.anti-forgery :refer [anti-forgery-field]]
            [rum.core :as rum]
            [fuggle.components.main :refer [main]]))

(rum/defc default [state loadjs]
  [:html
   [:head
    [:meta {:charset "utf-8"}]
    [:title "Fuggle"]
    [:meta {:name    "viewport"
            :content "width=device-width, initial-scale=1.0"}]
    [:link {:type "text/css" :href "/style.css" :rel "stylesheet"}]]
   [:body
    [:div#app {:dangerouslySetInnerHTML
               {:__html (rum/render-html (main (atom state)))}}]
    [:script {:dangerouslySetInnerHTML {:__html loadjs}}]]])

(rum/defc login []
  [:html
   [:head
    [:meta {:charset "utf-8"}]]
   [:body
    [:form
     {:action "/login"
      :method "POST"}
     [:div {:dangerouslySetInnerHTML {:__html (anti-forgery-field)}}]
     [:table
      [:tbody
       [:tr
        [:td [:label {:for "username"} "Email"]]
        [:td [:input {:type "text" :name "username"}]]]
       [:tr
        [:td [:label {:for "password"} "Password"]]
        [:td [:input {:type "password" :name "password"}]]]
       [:tr
        [:td
         {:col-span 2
          :style {:text-align "right"}}
         [:input {:type "submit" :value "Login"}]]]]]]]])