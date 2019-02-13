(ns fuggle.components.search
  (:require [clojure.string :refer [blank? lower-case]]
            [garden.color :as color]
            [rum.core :as rum]
            [fuggle.routes :as routes]
            [fuggle.data :as data]
            [fuggle.components.general :as general]
    #?(:cljs [fuggle.handlers :as handlers])))

(rum/defc recipe-link < rum/static [state recipe]
  [:div
   [:div.row
    [:div.cell
     [:a
      (general/clicker state #?(:cljs handlers/set-page) {:page [:recipe-view :id (:id recipe)]})
      (data/title (:title recipe))]]]
   (when-let [headline (:headline recipe)]
     [:div.row [:div.cell {:dangerouslySetInnerHTML {:__html headline}}]])])

(rum/defc categories-div < rum/static [state recipes query]
  [:div.center
   (when (blank? query)
     [:div.row
      [:h2.cell (let [category (-> recipes first :category)]
                  (if (blank? category)
                    "Uncategorized"
                    category))]])
   (map-indexed #(rum/with-key (recipe-link state %2) %1) recipes)])

(defn search-input-did-mount
  [{[state _] :rum/args comp :rum/react-component :as s}]
  #?(:cljs    (handlers/search-set-query!
                state
                (-> comp js/ReactDOM.findDOMNode .-value))
     :default nil)
  s)

(rum/defc search-input < rum/static {:did-mount search-input-did-mount}
  [state q]
  [:input.cell
   {:type          "text"
    :name          "q"
    :value         (if (nil? q) "" q)
    :auto-complete :off
    :on-change     #?(:cljs    #(handlers/search-set-query!
                                  state
                                  (general/event-value %))
                      :default nil)
    :on-key-down   #?(:cljs    #(when (= (.-key %) "Enter")
                                  (handlers/search-search state))
                      :default nil)}])

(defn secondary-header [state q]
  [:div.header
   [:div.center
    [:div.row
     (search-input state q)]
    [:div.row
     [:div.cell
      [:input.primary
       {:type     "submit"
        :value    "Search"
        :on-click #?(:cljs    (fn [e]
                                (handlers/search-search state)
                                (.preventDefault e))
                     :default nil)}]
      [:a.mleft
       (general/clicker
         state
         #?(:cljs handlers/set-page)
         {:page [:search] :query-params {"q" ""}})
       "Clear"]
      [:a.mleft
       (general/clicker state #?(:cljs handlers/set-page) {:page [:recipe-new]})
       "New"]]]]])

(rum/defc page < rum/static [state {recipes :page-state
                                    {q "q"} :query-params
                                    :as     state-map}]
  [:div
   (general/primary-header state state-map)
   [:form
    {:action (routes/path-for [:search])
     :method :get}
    (secondary-header state q)
    (map-indexed #(rum/with-key (categories-div state %2 q) %1) recipes)]])

(rum/defc error < rum/static [state {{:keys [name description]} :page-state
                                     :as                        state-map}]
  [:div
   (general/primary-header state state-map)
   [:div.center
    [:div.row [:h2.cell name]]
    [:div.row [:pan.cell description]]]])

(rum/defc style-test < rum/static [state state-map]
  [:div
   (general/primary-header state state-map)
   [:div.header
    [:div.center
     [:div.row
      [:div.cell "secondary-header"]]]]
   [:div.center
    [:h1.row "h1"]
    [:h2.row "h2"]
    [:h3.row "h3"]

    [:div.row
     (map-indexed
       (fn [i c]
         [:div.cell
          {:key   i
           :style {:background-color (color/as-hex c)}}
          (* (+ i 1) 10)])
       (color/shades (color/rgb 0 0 0)))]

    [:div.row
     [:div.cell
      {:style {:background-color "lightgray"}}
      "Graybeard"]]

    [:div.row
     ;{:style {:outline "red solid 1px"}}
     [:input.cell.primary.mright {:type "submit" :value "Primary button"}]
     [:input.cell {:type "submit" :value "Default"}]
     "Text"
     [:input.cell
      {:type     "submit"
       :value    "Error"
       :on-click #?(:cljs    #(handlers/set-page
                                state
                                {:page [:recipe-view :id "slkdfjlkdjf"]})
                    :default nil)}]]
    [:div.row
     [:a {:href "#"} "Click on me"]]
    [:div.row
     [:input.cell
      {:type "text"}]]
    [:div.row
     [:select.cell
      [:option {:value 0} "Zero"]
      [:option {:value 1} "One"]
      [:option {:value 2} "Two"]]]
    [:ol
     [:li "one"]
     [:li "two"]
     [:li "three"]]
    [:ul
     [:li "item"]
     [:li "item"]
     [:li "item"]]]])
