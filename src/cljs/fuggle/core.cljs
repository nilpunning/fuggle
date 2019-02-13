(ns fuggle.core
  (:require [clojure.string :refer [blank?]]
            [rum.core :as rum]
            [fuggle.xhr :refer [xhrio]]
            [fuggle.handlers :as handlers]
            [fuggle.util :as util]
            [fuggle.components.main :refer [main]]))

(defn make-ref [state]
  (fn [comp]
    (if comp
      (do
        (add-watch state :render (fn [_ _ _ _] (rum/request-render comp)))
        (rum/request-render comp))
      (remove-watch state :render))))

(defn init-browser! []
  (let [state (atom (handlers/init-state))]
    (swap! state assoc-in [:tmp-state :xhrio] (xhrio state))
    (rum/mount
      (rum/with-ref (main state) (make-ref state))
      (.getElementById js/document "app"))
    (set!
      (.-onpopstate js/window)
      (fn [e]
        ; Safari calls onpopstate on page load with no state
        (when (.-state e)
          (handlers/set-page-history state (-> e .-state util/safe-read)))))))

(defn init! []
  (when (exists? js/document)
    (init-browser!)))
