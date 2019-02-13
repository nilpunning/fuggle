(ns fuggle.routes
  (:require [clojure.string :refer [join blank?]]
            [bidi.bidi :as bidi]))

(def routes ["/" {["resources/" [#".*" :rest]] :resources
                  "style.css"                  :style.css
                  "login"                      :login
                  "logout"                     :logout
                  ""                           :search
                  "style-test"                 :style-test
                  "settings"                   :settings-view
                  "settings/edit"              :settings-edit
                  "recipe/"                    {"new" :recipe-new
                                                [:id] {""        :recipe-view
                                                       "/edit"   :recipe-edit
                                                       "/delete" :recipe-delete
                                                       "/photo/" {[:photo_name] :recipe-photo}}}}])

(defn url-encode [s]
  (#?(:clj java.net.URLEncoder/encode :cljs goog.string/urlEncode) s))

(defn query-string [query-params]
  (join
    "&"
    (->> query-params
         (map (partial map str))
         (map (fn [[k v]] (str k "=" (url-encode v)))))))

(defn path-for
  ([args query-params]
   (let [qp (->> query-params
                 (filter (fn [[_ v]] (not (blank? v))))
                 (into {}))]
     (str
       (apply bidi/path-for routes args)
       (if (seq qp) "?" "")
       (query-string qp))))
  ([args]
   (path-for args nil)))

(comment

  ;[com.rpl.specter :refer [setval MAP-VALS NONE] :include-macros true]
  (setval [MAP-VALS blank?] NONE query-params)
  (->> {:1 "1" :2 ""}
       (filter (fn [[_ v]] (not (blank? v))))
       (into {}))

  (bidi/path-for routes :recipe-view :id 4)
  (bidi/path-for routes :search)
  (bidi/path-for routes :search)

  (path-for [:search] {"q" "stuff"})
  (path-for [:search])
  (path-for [:settings-view] {})
  (path-for [:settings-edit])
  (path-for [:recipe-view :id 8])
  (path-for [:recipe-new])
  (path-for [:recipe-edit :id 3])
  (path-for [:recipe-edit :id nil])

  (path-for [:recipe-photo :id 1 :photo_name 4])
  (bidi/match-route routes "/recipe/1/photo/5")

  (bidi/match-route routes "/resources/stuffandjunk")
  (bidi/url-decode "")

  (boolean "")

  (bidi/match-route routes "/recipe/38" :query-params {"stuff" ["a" "b"]})

  )