(ns fuggle.repl
  (:require [com.stuartsierra.component :as component]
            [fuggle.system :as fuggle-system]))

(defonce system (atom nil))

(defn init []
  (reset!
    system
    (fuggle-system/server
      {:port          80
       :open-browser? false
       :stacktraces?  false})))

(defn start [] (swap! system component/start))
(defn stop [] (swap! system component/stop))

(defn go []
  (init)
  (start))

(defn restart []
  (stop)
  (start))

(comment
  (go)
  (restart)
  (stop)
  (start)
  (System/exit 0)
  )