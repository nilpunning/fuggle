(ns fuggle.components.settings-view
  (:require [rum.core :as rum]
            #?(:cljs [fuggle.handlers :as handlers])
            [fuggle.components.general :as general]))

(rum/defc page < rum/static [state {page-state :page-state
                                    :as state-map}]
  [:div
   (general/primary-header state state-map)
   [:div.center
    [:h2
     [:div.row
      [:span.cell "Categories"]
      [:span.cell.tright
       [:a
        (general/clicker state #?(:cljs handlers/set-page :default nil) {:page [:settings-edit]}) "Edit"]]]]
    [:div.row
     [:div.cell
      (if (empty? page-state)
        [:span "None"]
        [:ul (map (fn [v] [:li (:category v)]) page-state)])]]
    [:div.row
     [:br]
     [:br]
     [:br]
     [:a {:href "/logout"} "Log out"]]]])