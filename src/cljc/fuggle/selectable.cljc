(ns fuggle.selectable
  (:require [clojure.data :refer [diff]]
            [fuggle.util :as util]))

(defn create [m selections]
  "m - map
   selections - set of map keys
   => {:m - m
       :selections - selections
       :selected - m with only the selections
       :selectables - m without the selections}"
  {:m          m
   :selections selections
   :selected   (vals (filter (fn [[k _]] (selections k)) m))
   :selectable (vals (remove (fn [[k _]] (selections k)) m))})

(defn with-timestamp-meta [m k]
  (update m k #(with-meta % {:timestamp (util/now)})))

(defn add-selection [{:keys [m selections]} k]
  "Adds selection with key k."
  (create (with-timestamp-meta m k) (conj selections k)))

(defn delete-selection [{:keys [m selections]} k]
  "Removes selection with key k."
  (create (with-timestamp-meta m k) (disj selections k)))

(defn changes [original {selections :selections}]
  (let [[delete insert _] (diff original selections)]
    {:insert insert
     :delete delete}))