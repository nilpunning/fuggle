(ns fuggle.components.general
  (:require [rum.core :as rum]
            [clojure.string :refer [blank? escape]]
            [fuggle.routes :refer [path-for]]
    #?(:cljs [fuggle.handlers :as handlers])))

(defn clicker
  ([state handler {:keys [page query-params] :as p}]
   {:href     (path-for page query-params)
    :on-click #?(:cljs    #(do
                             (when (not (or (.-altKey %)
                                            (.-ctrlKey %)
                                            (.-metaKey %)
                                            (.-shiftKey %)))
                               (.preventDefault %)
                               (handler state p))
                             nil)
                 :default nil)})
  ([state args]
   (clicker state nil args)))

(defn dissoc-mutable-state [m]
  (dissoc m :tmp-page-state :tmp-state))

(rum/defc init-state < rum/static [state]
  [:input
   {:id    "fuggle-init-state"
    :type  "hidden"
    :name  :state
    :value (pr-str (dissoc-mutable-state state))}])

(defn progress-bar-width [progress]
  (if (number? progress)
    (str (* progress 100) "%")
    0))

(rum/defc progress-bar < rum/static [progress]
  [:div.progress-bar
   [:div.content
    {:style {:width (progress-bar-width progress)}}]])

(defn message-header [message]
  [:div.header
   [:div.center
    [:div.row
     [:span.cell message]]]])

(rum/defc timeout-header < rum/static [timeout]
  (when timeout
    (message-header "Last request timed out please try again.")))

(rum/defc old-version-div < rum/static [version server-version]
  (when (not= version server-version)
    (message-header
      "Refresh required.  New code has been released please refresh to receive it.")))

(rum/defc primary-header-div [state]
  [:div.header.no-print
   [:div.center
    [:div.row
     [:span.cell
      [:a.mright
       (clicker state #?(:cljs handlers/set-page) {:page [:search]})
       "Search"]]
     [:span.cell.tright
      [:a
       (clicker state #?(:cljs handlers/set-page) {:page [:settings-view]})
       "Settings"]]]]])

(defn primary-header [state
                      {:keys [progress timeout version server-version]
                       :as   state-map}]
  (map-indexed
    #(rum/with-key %2 %1)
    [(progress-bar progress)
     (timeout-header timeout)
     (old-version-div version server-version)
     (init-state state-map)
     (primary-header-div state)]))

(defn event-value [event]
  (-> event .-target .-value))

(def default-title "Fuggle")

(defn set-title! [title]
  #?(:cljs
     (when (exists? js/document)
       (set!
         (.-title js/document)
         (if (blank? title)
           default-title
           title)))))

(defn set-title-will-mount [path-to-title this]
  (let [[_ state] (:rum/args this)]
    (set-title! (get-in state path-to-title)))
  this)

(defn set-title-transfer-state [path-to-title old-this this]
  (let [[_ old-state] (:rum/args old-this)
        [_ new-state] (:rum/args this)
        new-title (get-in new-state path-to-title)]
    (when (not= (get-in old-state path-to-title) new-title)
      (set-title! new-title)))
  this)

(defn set-title-will-unmount [this]
  (set-title! default-title)
  this)

(defn title-mixin [path-to-title]
  {:will-mount     (partial set-title-will-mount path-to-title)
   :transfer-state (partial set-title-transfer-state path-to-title)
   :will-unmount   (partial set-title-will-unmount path-to-title)})

(rum/defc csrf-input < rum/static [csrf]
  [:input {:key   :csrf
           :type  "hidden"
           :name  "__anti-forgery-token"
           :value csrf}])

(rum/defc nonce-input < rum/static [nonce]
  [:input {:key   :nonce
           :type  "hidden"
           :name  "nonce"
           :value nonce}])

(defn post-hidden [{:keys [csrf nonce]}]
  (map-indexed
    #(rum/with-key %2 %1)
    [(csrf-input csrf)
     (nonce-input nonce)]))