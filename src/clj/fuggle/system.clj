(ns fuggle.system
  (:require [com.stuartsierra.component :as component]
            [ring.server.standalone :refer [serve]]
            [fuggle.handler :refer [app]]))

(defrecord Server [options]
  component/Lifecycle

  (start [this]
    (if (contains? this :server)
      this
      (assoc
        this
        :server
        (serve
          app
          (assoc options :join? false)))))

  (stop [this]
    (if (contains? this :server)
      (do
        (.stop (:server this))
        (.join (:server this))
        (dissoc this :server))
      this)))

(defn server [jetty-options]
  (->Server jetty-options))