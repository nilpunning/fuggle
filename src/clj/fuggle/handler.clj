(ns fuggle.handler
  (:require [clojure.edn :as edn]
            [clojure.stacktrace :as stacktrace]
            [clojure.java.io :refer [resource]]
            [bidi.bidi :refer [match-route]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.nested-params :refer [parse-nested-keys]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [fuggle.bounded-stream :refer [store]]
            [ring.util.response :refer [redirect]]
            [ring.util.request :refer [content-type]]
            [cemerick.friend :as friend]
            [cemerick.friend [workflows :as workflows] [credentials :as creds]]
            [fuggle.handlers :as handlers]
            [fuggle.handler-utils :refer [not-found accepts-edn?]]
            [fuggle.routes :refer [routes]]
            [fuggle.wrapped-db :refer [user-password-where-email]]
            [fuggle.util :as util]
            [fuggle.nonce-middleware :refer [wrap-nonce-middleware]])
  (:use ring.middleware.anti-forgery))

(def handlers
  (merge
    {:login     handlers/login
     :logout    (friend/logout (fn [_] (redirect "/")))
     :resources (friend/wrap-authorize handlers/resources #{::user})}
    (->> {:style.css     handlers/style
          :search        handlers/search
          :style-test    handlers/style-test
          :settings-view handlers/settings-view
          :settings-edit handlers/settings-edit
          :recipe-new    handlers/recipe-edit
          :recipe-view   handlers/recipe-view
          :recipe-edit   handlers/recipe-edit
          :recipe-delete handlers/recipe-delete
          :recipe-photo  handlers/recipe-photo}
         (map (fn [[k h]] [k (friend/wrap-authorize h #{::user})]))
         (into {}))))

(defn read-route-params [route-params]
  (->> route-params
       (map (fn [[k v]] [k (edn/read-string v)]))
       (into {})))

(defn route-handler [{:keys [uri] :as req}]
  (let [{:keys [handler] :as match} (match-route routes uri)]
    (if-let [handler (handlers handler)]
      (handler (merge req (update match :route-params read-route-params)))
      (not-found req))))

(defn user-lookup [username]
  (if-let [password (user-password-where-email username)]
    (merge password {:username username
                     :roles    #{::user}})
    nil))

(defn wrap-read-edn-body [handler]
  (fn [request]
    (handler
      (if (= (content-type request) "application/edn")
        (update request :body #(edn/read-string (slurp %)))
        request))))

(defn parse-and-read-nested-keys [s]
  (let [keys (parse-nested-keys s)]
    (cons (first keys) (map util/safe-read (rest keys)))))

(def defaults
  (-> site-defaults
      (assoc-in
        [:params :nested]
        {:key-parser parse-and-read-nested-keys})
      (assoc-in
        [:params :multipart]
        ; 10 MB limit
        {:store (store 10e6)})))

(defn user-id [{:keys [session]}]
  (let [ident (:cemerick.friend/identity session)]
    (get-in ident [:authentications (:current ident) :id])))

(defn wrap-make-user-id-easier-to-find [handler]
  (fn [req]
    (handler
      (assoc req :user-id (user-id req)))))

(defn wrap-vary-accept [handler]
  (fn [req]
    (assoc-in (handler req) [:headers "Vary"] "Accept")))

(defn unauthenticated-handler [request]
  (let [response (friend/default-unauthenticated-handler request)]
    (if (accepts-edn? request)
      (-> response
          (assoc :status 401)
          (assoc-in [:headers "WWW-Authenticate"] "form"))
      response)))

(defn wrap-loadjs [handler]
  (let [loadjs (slurp (resource "load.js"))]
    (fn [request]
      (handler (assoc request :loadjs loadjs)))))

(defn wrap-exception [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (binding [*out* *err*]
          (stacktrace/print-cause-trace e)
          (println (ex-data e)))
        (handlers/error request e)))))

(def app
  (-> route-handler
      (friend/authenticate
        {:credential-fn           (partial
                                    creds/bcrypt-credential-fn
                                    user-lookup)
         :unauthenticated-handler unauthenticated-handler
         :workflows               [(workflows/interactive-form)]})
      (wrap-exception)
      (wrap-nonce-middleware)
      (wrap-make-user-id-easier-to-find)
      (wrap-defaults defaults)
      (wrap-read-edn-body)
      (wrap-vary-accept)
      (wrap-gzip)
      (wrap-loadjs)))