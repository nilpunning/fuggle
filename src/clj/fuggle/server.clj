(ns fuggle.server
  (:require [clojure.tools.nrepl.server :refer [start-server]]
            [com.stuartsierra.component :as component]
            [fuggle.system :as fuggle-system])
  (:gen-class))

(defn -main [& _]
  (start-server :bind "0.0.0.0" :port 5555)
  (component/start
    (fuggle-system/server
      {:port          80
       :open-browser? false
       :stacktraces?  false})))
