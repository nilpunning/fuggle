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