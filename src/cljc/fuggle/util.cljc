(ns fuggle.util
  (:require
    [clojure.string :refer [blank?]]
    #?(:cljs [cljs.reader :as safe-reader]
       :clj
    [clojure.edn :as safe-reader])))

(defn now []
  (.getTime (#?(:clj java.util.Date. :cljs js/Date.))))

(defn safe-read [edn-str]
  (when (not (blank? edn-str))
    (safe-reader/read-string edn-str)))