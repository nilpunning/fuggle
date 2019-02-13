(ns fuggle.wrapped-db
  (:require [clojure.string :refer [blank?]]
            [clojure.java.jdbc :as jdbc]
            [ring.util.http-response :as response]
            [fuggle.data :as data]
            [fuggle.photo :refer [photo-map]]
            [fuggle.selectable :as selectable]
            [fuggle.db :as db])
  (:import [java.io ByteArrayInputStream]))

(defn user-password-where-email [email]
  (first (db/user-password-where-email {:email email})))

(defn id-seq-into-map [seq-map-with-id]
  (into {} (map (fn [m] [(:id m) m]) seq-map-with-id)))

(defn categories
  ([user-id _ _ tx]
   (map #(dissoc % :user_id) (db/categories {:user_id user-id} {:connection tx})))
  ([user-id _ _] (categories user-id _ _ db/db-spec)))

(defn settings-view [{:keys [user-id]}]
  (categories user-id nil nil))

(defn settings-edit [req]
  {:categories (vec (settings-view req))})

(defn search [{:keys [user-id] {q :q} :params}]
  (partition-by
    :category_id
    (if (blank? q)
      (db/recipe-titles {:user_id user-id})
      (db/search-recipe {:user_id user-id :query q}))))

(defn only-recipe-by-id [user-id id tx]
  (dissoc (first (db/recipe-by-id {:user_id user-id :id id} {:connection tx})) :tsv))

(defn categories-to-recipe [recipe-id tx]
  (vec (db/categories-to-recipe {:recipe_id recipe-id} {:connection tx})))

(defn recipe-view [{:keys [user-id] {:keys [id]} :route-params}]
  (jdbc/with-db-transaction
    [tx db/db-spec]
    (if-let [recipe (only-recipe-by-id user-id id tx)]
      {:recipe               recipe
       :categories-to-recipe (categories-to-recipe id tx)}
      (response/not-found!))))

(defn recipe-and-categories-by-id [{:keys [user-id] {:keys [id]} :route-params}]
  (jdbc/with-db-transaction
    [tx db/db-spec]
    (let [categories-to-recipe (set (map :id (db/category-ids-to-recipe {:recipe_id id} {:connection tx})))]
      {:recipe               (only-recipe-by-id user-id id tx)
       :categories-to-recipe categories-to-recipe
       :categories           (selectable/create
                               (id-seq-into-map (categories user-id nil nil tx))
                               categories-to-recipe)})))

(defn insert-or-update-recipe<!
  [user-id recipe photo tx]
  (as-> (merge data/empty-recipe {:user_id user-id} recipe) r
        (if (contains? r :id)
          (db/update-recipe<! r {:connection tx})
          (db/insert-recipe<! r {:connection tx}))
        (if-let [pm (photo-map photo)]
          (db/update-recipe-photo<! (merge r pm))
          (if (:photo_name recipe)
            r
            (db/update-recipe-photo<! (merge r {:photo_name nil :photo nil}))))
        (dissoc r :user_id :tsv :photo)))

(defn insert-or-delete-categories-to-recipe<!
  [recipe-id {:keys [insert delete]} tx]
  (let [f (fn [category-id] {:category_id category-id :recipe_id recipe-id})]
    (doseq [category-id insert] (db/insert-category-to-recipe! (f category-id)))
    (doseq [category-id delete] (db/delete-category-to-recipe! (f category-id))))
  (categories-to-recipe recipe-id tx))

(defn save-recipe<! [user-id {:keys [recipe categories]} photo]
  (let [recipe (insert-or-update-recipe<! user-id recipe photo db/db-spec)
        m {:recipe               recipe
           :categories-to-recipe (insert-or-delete-categories-to-recipe<!
                                   (:id recipe)
                                   categories
                                   db/db-spec)}]
    (db/update-recipe-tsv! (select-keys recipe [:id]))
    m))

(defn delete-recipe! [{:keys [user-id] {:keys [id]} :route-params}]
  (db/delete-recipe! {:user_id user-id :id id}))

(defn recipe-photo [{:keys [user-id] {:keys [id photo_name]} :route-params}]
  (if-let [p (first (db/recipe-photo-by-id {:user_id    user-id
                                            :id         id
                                            :photo_name photo_name}))]
    {:status       200
     :content-type "image/png"
     :body         (ByteArrayInputStream. (:photo p))}
    {:status 404}))

(defn delete-categories! [user-id categories tx]
  (->> categories
       (filter #(and (contains? % :id) (:delete? %)))
       (map #(db/delete-category! {:user_id user-id :id (:id %)} {:connection tx}))
       vec))

(defn insert-categories! [user-id categories tx]
  (->> categories
       (filter #(and (not (contains? % :id)) (not (:delete? %))))
       (map #(db/insert-category! (assoc % :user_id user-id) {:connection tx}))
       vec))

(defn update-recipe-tsvs! [category-id tx]
  (doseq [r (db/recipe-id-by-category-id {:category_id category-id} {:connection tx})]
    (db/update-recipe-tsv! {:id (:recipe_id r)} {:connection tx})))

(defn update-category! [m user-id tx]
  (db/update-category! (assoc m :user_id user-id) {:connection tx})
  (update-recipe-tsvs! (:id m) tx))

(defn update-categories! [user-id categories tx]
  (->> categories
       (filter #(and (contains? % :id) (not (:delete? %))))
       (map #(update-category! % user-id tx))
       dorun))

(defn save-categories<! [{:keys [user-id body]}]
  (jdbc/with-db-transaction
    [tx db/db-spec]
    (update-categories! user-id body tx)
    (insert-categories! user-id body tx)
    (delete-categories! user-id body tx)
    (categories user-id nil nil tx)))