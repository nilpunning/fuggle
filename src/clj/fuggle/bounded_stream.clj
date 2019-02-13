(ns fuggle.bounded-stream
  (:require [ring.middleware.multipart-params.temp-file :refer [temp-file-store]])
  (:import [org.apache.commons.io IOUtils]
           [org.apache.commons.io.input BoundedInputStream]))

(defn to-stream [{:keys [stream]} limit]
  (BoundedInputStream. ^InputStream stream limit))

(defn store [limit]
  (let [f (temp-file-store)]
    (fn [item]
      (assoc
        (f (assoc item :stream (to-stream item (long limit))))
        :limit limit))))