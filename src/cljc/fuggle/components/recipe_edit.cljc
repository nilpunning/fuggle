(ns fuggle.components.recipe-edit
  (:require [rum.core :as rum]
            [fuggle.routes :as routes]
            [fuggle.data :as data]
    #?(:cljs [fuggle.handlers :as handlers])
            [fuggle.components.general :as general]))

(defn input-did-mount
  [{[state _ field] :rum/args comp :rum/react-component :as s}]
  #?(:cljs    (swap!
                state
                data/recipe-edit-set-field
                field
                (-> comp js/ReactDOM.findDOMNode .-firstChild .-value))
     :default nil)
  s)

(defn form-field-name [field]
  (str "fields[" field "]"))

(rum/defc input < rum/static {:did-mount input-did-mount}
  [state value field]
  [:div.row
   [:input.cell
    {:type          "text"
     :name          (form-field-name field)
     :value         value
     :auto-complete :off
     :on-change     #?(:cljs    #(swap!
                                   state
                                   data/recipe-edit-set-field
                                   field
                                   (general/event-value %))
                       :default nil)}]])

(defn textarea-id [field]
  (str "textarea-" (name field)))

(defn textarea-will-mount
  [{[state _ field] :rum/args :as s}]
  #?(:cljs    (when-let [elem (->> field
                                   textarea-id
                                   (.getElementById js/document))]
                (swap!
                  state
                  data/recipe-edit-set-field
                  field
                  (.-value elem)))
     :default nil)
  s)

(rum/defc textarea < rum/static {:will-mount textarea-will-mount}
  [state value field]
  [:div.row
   [:textarea.cell
    {:id        (textarea-id field)
     :rows      6
     :name      (form-field-name field)
     :value     value
     :on-change #?(:cljs    #(swap!
                               state
                               data/recipe-edit-set-field
                               field
                               (general/event-value %))
                   :default nil)}]])

(defn select-did-mount
  [{[state _] :rum/args comp :rum/react-component :as s}]
  #?(:cljs    (swap!
                state
                data/recipe-edit-add-category
                (-> comp js/ReactDOM.findDOMNode .-value))
     :default nil)
  s)

(rum/defc categories-select < rum/static {:did-mount select-did-mount}
  [state selectable]
  [:div.row
   [:select.cell
    (merge {:value     :add
            :name      "category"
            :on-change #?(:cljs    #(swap!
                                      state
                                      data/recipe-edit-add-category
                                      (general/event-value %))
                          :default nil)}
           (when (empty? selectable)
             {:disabled "disabled"}))
    [:option {:value :add} "Add category"]
    (map
      (fn [c] [:option {:key (:id c) :value (:id c)} (:category c)])
      selectable)]])

(defn category-div [i category on-click]
  [:div.row
   {:key i}
   [:input.mhright
    {:type     "submit"
     :name     (str "remove-categories[" i "]")
     :value    "Remove"
     :on-click on-click}]
   (:category category)])

(defn remove-categories [state selected]
  (map
    (fn [c]
      (category-div
        (:id c)
        c
        #?(:cljs    (fn [e]
                      (swap! state data/recipe-edit-remove-category (:id c))
                      (.preventDefault e))
           :default nil)))
    (sort-by #(:timestamp (meta %)) selected)))

(defn label [l]
  [:div.row [:h3 l]])

(defn photo-input-file [state]
  [:div.row
   [:input.cell
    {:type      "file"
     :name      "photo"
     :on-change #?(:cljs    (partial handlers/recipe-edit-photo-on-change state)
                   :default nil)}]])

(defn photo-input-remove [state {:keys [photo_name]} photo photo-preview]
  [:div.row
   [:input.cell
    (merge
      {:name     "remove-photo"
       :type     "submit"
       :value    "Remove"
       :style    {:max-width "300px"}
       :on-click #?(:cljs    (fn [e]
                               (swap! state data/recipe-edit-remove-photo)
                               (.preventDefault e))
                    :default nil)}
      (if (not (or photo_name photo photo-preview))
        {:disabled "disabled"}
        {}))]])

(defn secondary-header [state recipe progress-set]
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
          :on-click #?(:cljs    (fn [e]
                                  (handlers/recipe-edit-save state)
                                  (.preventDefault e))
                       :default nil)}
         (if (and (number? progress-set) (> progress-set 0))
           {:disabled "disabled"}
           {}))]]
     [:span.cell.tright
      (when (:id recipe)
        [:a.mright
         (assoc
           (general/clicker
             state
             #?(:cljs handlers/set-page :default nil)
             {:page [:recipe-delete :id (:id recipe)]})
           :key :delete)
         "Delete"])
      [:a
       (assoc
         (if (:id recipe)
           (general/clicker
             state
             #?(:cljs handlers/set-page :default nil)
             {:page [:recipe-view :id (:id recipe)]})
           (general/clicker
             state
             #?(:cljs handlers/set-page :default nil)
             {:page [:search]}))
         :key
         :cancel)
       "Cancel"]]]]])

(defn photo-img-src [{:keys [id photo_name]} photo photo-preview]
  (if photo-preview
    photo-preview
    (when (and photo_name (not photo))
      (routes/path-for [:recipe-photo :id id :photo_name photo_name]))))

(rum/defc page < rum/static (general/title-mixin [:page-state :recipe :title])
  [state {{{:keys [selectable selected]} :categories
           :keys                         [recipe]} :page-state
          {:keys [photo photo-preview]}            :tmp-page-state
          :keys                                    [progress-set]
          :as                                      state-map}]
  [:form
   {:action   (routes/path-for
                (if-let [id (:id recipe)]
                  [:recipe-edit :id id]
                  [:recipe-new]))
    :method   :post
    :enc-type "multipart/form-data"}
   (general/post-hidden state-map)
   (general/primary-header state state-map)
   (secondary-header state recipe progress-set)
   [:div.center
    (let [input-fn (fn [c] #(c state (% recipe) %))
          in (input-fn input)
          ta (input-fn textarea)]
      [:div
       (label "Title")
       (in :title)
       (label "Photo")
       (if-let [src (photo-img-src recipe photo photo-preview)]
         [:div.row
          [:div.cell
           [:img
            {:src   src
             :style {:max-width  "300px"
                     :max-height "300px"}}]]]
         (when photo
           [:div.row [:div.cell "Loading photo preview..."]]))
       (photo-input-remove state recipe photo photo-preview)
       (photo-input-file state)
       [:div.row [:div.cell "File must be less than 10 MB."]]
       (label "Categories")
       (categories-select state selectable)
       (remove-categories state selected)
       (label "Source")
       (in :source)
       (label "Yield")
       (in :yield)
       (label "Preparation time")
       (in :prep_time)
       (label "Cooking time")
       (in :cooking_time)
       (label "Ingredients")
       (ta :ingredients)
       (label "Tools")
       (ta :tools)
       (label "Notes")
       (ta :notes)
       (label "Directions")
       (ta :directions)])]])