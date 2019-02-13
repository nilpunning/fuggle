(ns fuggle.handlers
  (:require [bidi.bidi :refer [url-decode]]
            [ring.util.response :refer [resource-response]]
            [ring.util.http-status :as status]
            [fuggle.style :refer [css-text]]
            [fuggle.data :as data]
            [fuggle.routes :refer [path-for]]
            [fuggle.selectable :as selectable]
            [fuggle.wrapped-db :as db]
            [fuggle.nonce-middleware :refer [add-nonce]]
            [fuggle.util :as util]
            [fuggle.handler-utils :refer :all]))

(defn resources [req]
  (let [path (url-decode (get-in req [:route-params :rest]))]
    (if (not-empty path)
      (if-let [res (resource-response path {:root "public/"})]
        res
        (not-found req))
      (not-found req))))

(defn login [_]
  (render-login))

(defn error-status [e]
  (let [{:keys [type response]} (ex-data e)]
    (if (= type :ring.util.http-response/response)
      (:status response)
      status/internal-server-error)))

(defn error [req e]
  (let [status (error-status e)
        m (get status/status status)]
    (assoc
      (if (accepts-edn? req)
        (pr-edn m)
        (render-default
          (constantly m)
          (assoc req :handler :error :route-params [])))
      :status
      status)))

(defn style [req]
  (case (negotiate req)
    [:any :get] (css css-text)
    nil))

(defn search [req]
  (case (negotiate req)
    [:any :get] (render-default db/search req)
    [:edn :get] (pr-edn (db/search req))
    nil))

(defn style-test [req]
  (render-default (constantly nil) req))

(defn settings-view [req]
  (case (negotiate req)
    [:any :get] (render-default db/settings-view req)
    [:edn :get] (pr-edn (db/settings-view req))
    nil))

(defn- settings-edit-post
  [{{:keys [state new-category categories add delete save]} :params
    :as                                                     req}]
  (let [s (-> state
              util/safe-read
              (data/settings-edit-add-change new-category)
              (data/settings-edit-change categories)
              (data/settings-edit-add add new-category)
              (data/settings-edit-delete (first (keys delete))))]
    (if save
      (do
        (db/save-categories<! (assoc req :body (data/dirty-categories s)))
        {:status  303
         :headers {"Location" (path-for [:settings-view])}})
      (render-default (constantly (:page-state s)) (add-nonce req)))))

(defn settings-edit [req]
  (case (negotiate req)
    [:edn :get] (pr-edn (db/settings-edit req))
    [:edn :post] (pr-edn (db/save-categories<! req))
    [:any :get] (render-default db/settings-edit (add-nonce req))
    [:any :post] (settings-edit-post req)
    nil))

(defn recipe-edit-any-post [{{:keys [state
                                     photo
                                     remove-photo
                                     save
                                     remove-categories
                                     category
                                     fields]} :params
                             user-id          :user-id
                             :as              req}]
  (let [s (util/safe-read state)
        edit #(-> s
                  (data/recipe-edit-set-field fields)
                  (data/recipe-edit-add-category category))
        render #(render-default (constantly (:page-state %)) (add-nonce req))]
    (cond
      save (let [ps (:page-state (edit))
                 ns {:recipe     (:recipe ps)
                     :categories (selectable/changes
                                   (:categories-to-recipe ps)
                                   (:categories ps))}
                 {{id :id} :recipe} (db/save-recipe<! user-id ns photo)]
             {:status  303
              :headers {"Location" (path-for [:recipe-view :id id])}})
      remove-photo (render (data/recipe-edit-remove-photo (edit)))
      remove-categories (render
                          (data/recipe-edit-remove-category
                            (edit)
                            (first (keys remove-categories))))
      :else (render s))))

(defn recipe-edit-edn-post [{{:keys [state photo]} :params
                             user-id               :user-id}]
  (pr-edn
    (db/save-recipe<!
      user-id
      (util/safe-read state)
      photo)))

(defn recipe-edit [req]
  (case (negotiate req)
    [:any :get] (render-default db/recipe-and-categories-by-id (add-nonce req))
    [:any :post] (recipe-edit-any-post req)
    [:edn :get] (pr-edn (db/recipe-and-categories-by-id req))
    [:edn :post] (recipe-edit-edn-post req)
    nil))

(defn recipe-view [req]
  (case (negotiate req)
    [:any :get] (render-default db/recipe-view req)
    [:edn :get] (pr-edn (db/recipe-view req))
    nil))

(defn recipe-delete-post [req]
  (db/delete-recipe! req)
  {:status  303
   :headers {"Location" (path-for [:search])}})

(defn recipe-delete [req]
  (case (negotiate req)
    [:any :get] (render-default db/recipe-view (add-nonce req))
    [:any :post] (recipe-delete-post req)
    [:edn :get] (pr-edn (db/recipe-view req))
    [:edn :delete] (pr-edn (db/delete-recipe! req))
    nil))

(defn recipe-photo [req]
  (db/recipe-photo req))