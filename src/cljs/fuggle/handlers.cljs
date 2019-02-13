(ns fuggle.handlers
  (:require [clojure.string :refer [blank?]]
            [fuggle.xhr :refer [xget xpost xpost-multipart xdelete]]
            [fuggle.data :as data]
            [fuggle.selectable :as selectable]
            [fuggle.routes :refer [path-for]]
            [fuggle.util :as util]))

;; :init-state ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-init-state []
  (-> js/document
      (.getElementById "fuggle-init-state")
      .-value
      util/safe-read))

(defn replace-state [{:keys [page query-params] :as p}]
  (.replaceState
    js/history
    (pr-str p)
    ""
    (path-for page query-params)))

(defn init-state []
  (doto (get-init-state)
    (replace-state)))

;; :set-page ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn push-state [{:keys [page query-params] :as p}]
  (.pushState
    js/history
    (pr-str p)
    ""
    (path-for page query-params)))

(defn update-history [route-changed page last-push-state]
  (if (or route-changed (> (- (util/now) last-push-state) 3000))
    (push-state page)
    (replace-state page)))

(defn set-page-done [state page page-state]
  (let [route-changed (not= (:page @state) (:page page))]
    (update-history
      route-changed
      page
      (get-in @state [:tmp-state :last-push-state]))
    ; Only scroll to top if route changed
    (when route-changed
      (.scroll js/window 0 0)))
  (swap! state data/set-page-state page page-state))

(defn get-set-page [state {:keys [page query-params] :as p} done-fn]
  (xget state page query-params #(done-fn state p %)))

(defn set-page [state page]
  (get-set-page state page set-page-done))

(defn set-page-history-done [state page page-state]
  (swap! state data/set-page-state page page-state))

(defn set-page-history [state page]
  (get-set-page state page set-page-history-done))

;; :search ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn search-search [state]
  (set-page state (select-keys @state [:page :query-params])))

(defn nts [x]
  (if (nil? x)
    ""
    x))

(defn search-set-query! [state query]
  (let [loc [:query-params "q"]
        old-query (get-in @state loc)]
    (swap! state assoc-in loc query)
    (when (not= (nts query) (nts old-query))
      (search-search state))))

;; :settings-edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn settings-edit-save [state]
  (xpost
    state
    [:settings-edit]
    (data/dirty-categories @state)
    #(set-page-done state {:page [:settings-view]} %)))

;; :recipe-edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn recipe-edit-save [state]
  (let [s @state
        ps (:page-state s)
        id (get-in ps [:recipe :id])]
    (xpost-multipart
      state
      (if id [:recipe-edit :id id] [:recipe-new])
      (doto (js/FormData.)
        (.append
          "state"
          (pr-str
            {:recipe     (:recipe ps)
             :categories (selectable/changes
                           (:categories-to-recipe ps)
                           (:categories ps))}))
        (.append "photo" (get-in s [:tmp-page-state :photo])))
      #(set-page-done
         state
         {:page [:recipe-view :id (get-in % [:body :recipe :id])]}
         %))))

(defn recipe-edit-read-file [file on-load]
  (when file
    (let [reader (js/FileReader.)]
      (.addEventListener
        reader
        "load"
        #(on-load (.-result reader)))
      (.readAsDataURL reader file))))

(defn recipe-edit-photo-on-change [state e]
  (let [file (-> e .-target .-files (.item 0))]
    (swap! state assoc-in [:tmp-page-state :photo] file)
    (swap! state update-in [:tmp-page-state] dissoc :photo-preview)
    (recipe-edit-read-file
      file
      #(swap! state assoc-in [:tmp-page-state :photo-preview] %))))

;; :recipe-delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn recipe-delete [state]
  (xdelete
    state
    [:recipe-delete :id (get-in @state [:page-state :recipe :id])]
    #(set-page state {:page [:search]})))