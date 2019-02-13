(ns fuggle.components.settings-edit
  (:require [rum.core :as rum]
            [fuggle.data :as data]
            [fuggle.routes :as routes]
    #?(:cljs [fuggle.handlers :as handlers])
            [fuggle.components.general :as general]))

(defn edit-category-did-mount
  [{[_ _ on-change _] :rum/args comp :rum/react-component :as s}]
  #?(:cljs    (on-change (-> comp js/ReactDOM.findDOMNode .-value))
     :default nil)
  s)

(rum/defc edit-category-input < rum/static {:did-mount edit-category-did-mount}
  [input-name value on-change on-key-down]
  [:input.cell.mhright
   {:name          input-name
    :type          "text"
    :value         (if (nil? value) "" value)
    :auto-complete :off
    :on-change     #(do (on-change (general/event-value %)) nil)
    :on-key-down   (fn [e]
                     (when (= (.-key e) "Enter")
                       (on-key-down)
                       (.preventDefault e)))}])

(rum/defc edit-category-submit [button-name button-title on-click]
  [:input
   {:type     "submit"
    :name     button-name
    :value    button-title
    :on-click (fn [e]
                (on-click)
                (.preventDefault e))
    :style    {:width "100px"}}])

(defn edit-category [input submit & [k]]
  [:div.row {:key k} input submit])

(defn secondary-header [state progress-set]
  [:div.header
   [:div.center
    [:div.row
     [:span.cell
      [:input.primary.mright
       (merge
         {:key      :save
          :type     "submit"
          :name     :save
          :value    "Save"
          :href     "#"
          :on-click #?(:cljs    (fn [e]
                                  (handlers/settings-edit-save state)
                                  (.preventDefault e))
                       :default nil)}
         (if (and (number? progress-set) (> progress-set 0))
           {:disabled "disabled"}
           {}))]]
     [:span.cell.tright
      [:a
       (assoc
         (general/clicker state #?(:cljs handlers/set-page :default nil) {:page [:settings-view]})
         :key
         :cancel)
       "Cancel"]]]]])

(rum/defc page < rum/static [state {{:keys [new categories]} :page-state
                                    :keys                    [progress-set]
                                    :as                      state-map}]
  [:form
   {:action (routes/path-for [:settings-edit])
    :method :post}
   (general/post-hidden state-map)
   (general/primary-header state state-map)
   (secondary-header state progress-set)
   [:div.center
    [:div.row [:h2.cell "Categories"]]
    (edit-category
      (edit-category-input
        "new-category"
        new
        #(swap! state data/settings-edit-add-change %)
        #(swap! state data/settings-edit-add new))
      (edit-category-submit
        "add"
        "Add"
        #(swap! state data/settings-edit-add new)))
    (map-indexed
      (fn [i v]
        (when (not (:delete? v))
          (edit-category
            (edit-category-input
              (str "categories[" i "]")
              (:category v)
              #(swap! state data/settings-edit-change i %)
              (fn [_] nil))
            (edit-category-submit
              (str "delete[" i "]")
              "Delete"
              #(swap! state data/settings-edit-delete i))
            i)))
      categories)]])