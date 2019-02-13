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
                    {:subname "//postgres:5432/fuggle"
                     :user    "docker"}))

(declare update-recipe-tsv!)
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
(declare update-recipe-photo<!)

(defqueries "db.sql" {:connection db-spec})