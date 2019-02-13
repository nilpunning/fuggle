(ns fuggle.data
  (:require [clojure.data :refer [diff]]
            [clojure.string :refer [blank?]]
            [fuggle.selectable :as selectable]
            [fuggle.util :as util]))

(def empty-recipe
  {;:id          0
   :title        ""
   :source       ""
   :yield        ""
   :prep_time    ""
   :cooking_time ""
   :ingredients  ""
   :tools        ""
   :notes        ""
   :directions   ""})

(defn title [t]
  (if (blank? t)
    "Untitled"
    t))

(defn set-page-state [state page {:keys [version body]}]
  (-> state
      (merge {:query-params nil} page)
      (assoc-in [:tmp-state :last-push-state] (util/now))
      (dissoc :timeout)
      (assoc
        :server-version version
        :page-state body
        :tmp-page-state {})))


;; :settings-edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn settings-edit-add-change [state value]
  (assoc-in state [:page-state :new] value))

(defn settings-edit-add
  ([state value]
   (-> state
       (update-in
         [:page-state :categories]
         #(vec (cons {:category value :dirty? true} %)))
       (update-in [:page-state] dissoc :new)))
  ([state add value]
   (if add
     (settings-edit-add state value)
     state)))

(defn add-dirty-category-if-changed [m value]
  (if (not= (:category m) value)
    (merge m {:category value :dirty? true})
    m))

(defn settings-edit-change
  ([state index value]
   (update-in
     state
     [:page-state :categories index]
     #(add-dirty-category-if-changed % value)))
  ([state categories]
   (->> (get-in state [:page-state :categories])
        (map-indexed (fn [i c] [i (:category c)]))
        (into {})
        (diff categories)
        first
        (reduce (fn [s [i v]] (settings-edit-change s i v)) state))))

(defn dissoc-vec [v index]
  (vec (concat (subvec v 0 index) (subvec v (inc index)))))

(defn settings-edit-delete [state index]
  (if (nil? index)
    state
    (let [category (get-in state [:page-state :categories index])]
      (if (contains? category :id)
        (assoc-in state [:page-state :categories index :delete?] true)
        (update-in state [:page-state :categories] dissoc-vec index)))))

(defn dirty-categories [state]
  (->> (get-in state [:page-state :categories])
       (filter #(or (:dirty? %) (:delete? %)))))

;; :recipe-edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn recipe-edit-set-field
  ([state field value]
   (assoc-in state [:page-state :recipe field] value))
  ([state field-values]
   (reduce-kv recipe-edit-set-field state field-values)))

(defn recipe-edit-add-category [state id]
  (let [read-id (util/safe-read id)]
    (if (integer? read-id)
      (update-in state [:page-state :categories] selectable/add-selection read-id)
      state)))

(defn recipe-edit-remove-photo [state]
  (-> state
      (update-in [:tmp-page-state] #(dissoc % :photo :photo-preview))
      (update-in [:page-state :recipe] dissoc :photo_name)))

(defn recipe-edit-remove-category [state id]
  (update-in state [:page-state :categories] selectable/delete-selection id))