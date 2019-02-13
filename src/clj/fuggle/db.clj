(ns fuggle.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as j :refer [IResultSetReadColumn]])
  (:import [java.sql Timestamp]
           [java.util Date]))

; TODO: Switch over to Java 8 dates.
; http://rundis.github.io/blog/2015/clojure_dates.html
(extend-protocol IResultSetReadColumn
  Timestamp
  (result-set-read-column [v _ _] (Date/from (.toInstant v))))

(def db-spec (merge {:classname   "org.postgresql.Driver"
                     :subprotocol "postgresql"}
                    {:subname  "//postgres:5432/fuggle_db"
                     :user     "fuggleuser"
                     :password "pw"}))

(declare create-table-user!)
(declare create-table-recipe!)
(declare create-index-tsv!)
(declare create-index-recipe!)
(declare create-table-category!)
(declare create-index-category!)
(declare create-table-category-to-recipe!)
(declare alter-recipe-add-last_modified!)
(declare alter-recipe-add-notes!)
(declare alter-recipe-add-tsv!)
(declare drop-recipe-search-trigger!)
(declare update-recipe-tsv!)
(declare create-table-recipe!)
(declare user-password-where-email)
(declare categories)
(declare recipe-titles)
(declare search-recipe)
(declare recipe-by-id)
(declare categories-to-recipe)
(declare update-recipe<!)
(declare insert-recipe<!)
(declare insert-category-to-recipe!)
(declare delete-category-to-recipe!)
(declare delete-recipe!)
(declare delete-category!)
(declare insert-category!)
(declare recipe-id-by-category-id)
(declare update-category!)
(declare alter-recipe-add-photo!)
(declare update-recipe-photo<!)

(defqueries "db.sql" {:connection db-spec})

(comment
  (search-recipe {:query "fri" :user_id 1})
  (categories {:user_id 1})

  (require '[fuggle.photo :refer [photo-map]])

  (import [java.io File])

  (recipe-by-id {:user_id 1 :id 1})

  (dissoc (first (recipe-by-id {:user_id 1 :id 132323})) :tsv)

  (update-recipe-photo<!
    (merge
      {:user_id 1
       :id      1}
      (photo-map (File. "/Users/dave/Desktop/fries-small.png"))))

  (recipe-photo-by-id {:user_id    1
                       :id         1
                       :photo_name "cQIWQwf4Bs8cAPQSBIKuWg.png"})

  (update-recipe-photo<!)
  (recipe-titles {:user_id 1})
  )

(defn create-db []
  (create-table-user!)
  (create-table-recipe!)
  (create-index-tsv!)
  (create-index-recipe!)
  (create-table-category!)
  (create-index-category!)
  (create-table-category-to-recipe!))

(defn migration0 []
  (alter-recipe-add-last_modified!)
  (alter-recipe-add-notes!)
  (alter-recipe-add-tsv!)
  (create-index-tsv!))

(defn migration1 []
  (create-index-recipe!)
  (create-table-category!)
  (create-index-category!)
  (create-table-category-to-recipe!)
  (drop-recipe-search-trigger!)
  (doseq [id (j/query db-spec "SELECT id from recipe;")]
    (update-recipe-tsv! id))
  )

(defn migration2 []
  (alter-recipe-add-photo!))

(comment

  (drop-category-index!)
  (drop-category!)
  (drop-category-to-recipe!)

  (j/with-db-transaction
    [tx db-spec]
    (time-of-day nil {:connection tx}))

  (drop-table-recipe!)

  (require '[cemerick.friend [credentials :as creds]])
  (create-db)
  (insert-user<! {:email      "fake@email.com"
                  :password   (creds/hash-bcrypt "pw")
                  :first_name "Big"
                  :last_name  "Fake"})

  (user-password-where-email {:email "fake@email.com"})


  (require '[fuggle.data :refer [empty-recipe]])
  (insert-recipe<! (merge empty-recipe {:user_id 1
                                        :title   "Soup"}))

  (update-recipe<! (merge empty-recipe {:user_id 1
                                        :id      1
                                        :title   "Tomato Soup"}))

  (recipe-titles {:user_id 1})

  (delete-recipe! {:user_id 1 :id 3})

  ; Estimate data size
  (-> (map #(identity {:id % :title "aalskjfdlkasjdf alksdjfklas djflk asdfasdfkjasdlkf sakf"}) (range 1000))
      pr-str
      (.getBytes "UTF-8")
      alength)

  (-> (reduce str (map (fn [_] "a") (range (* 128 128))))
      (.getBytes "UTF-8")
      alength)

  (insert-category<! {:user_id 1 :category "Entrées"})

  (categories {:user_id 1})

  (insert-category-to-recipe<! {:category_id 17 :recipe_id 1})

  (categories-to-recipe {:recipe_id 1})

  )