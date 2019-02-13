(ns fuggle.components.recipe-view
  (:require [clojure.string :refer [blank? split-lines]]
            [rum.core :as rum]
            [fuggle.routes :as routes]
            [fuggle.data :as data]
    #?(:cljs [fuggle.handlers :as handlers])
            [fuggle.components.general :as general])
  (:import #?(:cljs goog.i18n.DateTimeFormat
              :clj [java.time.format DateTimeFormatter])
    #?(:clj
                   [java.time ZoneId])))

(defn split-line-lis [value]
  (map
    (fn [v] [:li v])
    (split-lines (str value " "))))

(defn details-div-li [label value]
  (when (not (blank? value))
    [:li (str label value)]))

(defn not-all-blank? [values]
  (->> values
       (map blank?)
       (map not)
       (some identity)))

(rum/defc details-div < rum/static [label-values]
  (when (not-all-blank? (map last label-values))
    [:div
     [:h2 "Details"]
     [:ul
      (map
        (fn [[l v]] (details-div-li l v))
        label-values)]]))

(defn categories-to-recipe-p-str [categories-to-recipe]
  (->> categories-to-recipe
       (map :category)
       (interpose ", ")
       (reduce str)
       (str "Categories: ")))

(rum/defc categories-to-recipe-div < rum/static [categories-to-recipe]
  (when (not-all-blank? (map :category categories-to-recipe))
    [:div.dem (categories-to-recipe-p-str categories-to-recipe)]))

(rum/defc label-div < rum/static [label value]
  (when (not (blank? (str value)))
    [:div.dem [:em (str label value)]]))

(rum/defc ingredients < rum/static [value]
  (when (not (blank? value))
    [:div.mright
     {:style {:display        :inline-block
              :vertical-align :top}}
     [:h2 "Ingredients"]
     [:ul (split-line-lis value)]]))

(rum/defc tools < rum/static [value]
  (when (not (blank? value))
    [:div
     {:style {:display        :inline-block
              :vertical-align :top}}
     [:h2 "Tools"]
     [:ul (split-line-lis value)]]))

(rum/defc notes < rum/static [value]
  (when (not (blank? value))
    [:pre [:em value]]))

(rum/defc directions < rum/static [value]
  (when (not (blank? value))
    [:div
     [:h2 "Directions"]
     [:ol (split-line-lis value)]]))

(defn date-str [date]
  "Takes date inst and returns string, Ex. May 6, 2016"
  (if (nil? date)
    nil
    (let [format "MMM d, yyy"]
      #?(:cljs (.format (goog.i18n.DateTimeFormat. format) date)
         :clj  (let [formatter
                     (DateTimeFormatter/ofPattern format)]
                 (-> date
                     .toInstant
                     (.atZone (ZoneId/systemDefault))
                     .toLocalDate
                     (.format formatter)))))))

(defn main-body
  [state {{:keys [recipe categories-to-recipe]} :page-state}]
  [:div.center
   [:div.row
    [:h1.cell
     {:style {:display "flex"}}
     [:span.mright (data/title (:title recipe))]
     [:span.cell.no-print.tright
      [:a.cell
       (assoc
         (general/clicker
           state
           #?(:cljs handlers/set-page)
           {:page [:recipe-edit :id (:id recipe)]})
         :style
         {:vertical-align :middle})
       "Edit"]]]]
   [:div.row
    [:div.cell
     (let [{:keys [id photo_name]} recipe]
       (when photo_name
         [:img
          {:src (routes/path-for
                  [:recipe-photo :id id :photo_name photo_name])}]))
     (categories-to-recipe-div categories-to-recipe)
     (label-div "Source: " (:source recipe))
     (label-div "Last modified: " (date-str (:last_modified recipe)))
     (details-div
       [["Yield: " (:yield recipe)]
        ["Prep time: " (:prep_time recipe)]
        ["Cooking time: " (:cooking_time recipe)]])
     [:div
      (ingredients (:ingredients recipe))
      (tools (:tools recipe))]
     (notes (:notes recipe))
     (directions (:directions recipe))]]])

(rum/defc page < rum/static (general/title-mixin [:page-state :recipe :title])
  [state state-map]
  [:div
   (general/primary-header state state-map)
   (main-body state state-map)])

(rum/defc delete < rum/static (general/title-mixin [:page-state :recipe :title])
  [state {{:keys [recipe]} :page-state
          :as              state-map}]
  [:form
   {:action (routes/path-for [:recipe-delete :id (:id recipe)])
    :method :post}
   (general/post-hidden state-map)
   (general/primary-header state state-map)
   [:div.header.no-print
    [:div.center
     [:div.row
      [:span.cell "Are you sure you want to delete this recipe?"]
      [:span.cell.tright
       [:a.mright
        (general/clicker
          state
          #?(:cljs handlers/set-page)
          {:page [:recipe-view :id (:id recipe)]})
        "Cancel"]
       [:input
        {:key      :delete
         :type     "submit"
         :value    "OK"
         :on-click #?(:cljs    (fn [e]
                                 (handlers/recipe-delete state)
                                 (.preventDefault e))
                      :default nil)}]]]]]
   (main-body state state-map)])