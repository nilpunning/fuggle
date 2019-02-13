(ns fuggle.handler-utils
  (:require [clojure.string :refer [join]]
            [rum.core :refer [render-static-markup]]
            [fuggle.config :refer [dev]]
            [fuggle.core :refer [version]]
            [fuggle.components :as components]
            [fuggle.data :as data])
  (:use ring.middleware.anti-forgery))

(def content-type "Content-Type")
(def edn-type "application/edn")
(def html "text/html")
(def plain "text/plain")

(defn not-found [_]
  {:status  404
   :headers {content-type plain}
   :body    "Not Found"})

(defn static-state [{:keys [handler route-params query-params nonce]} page-state]
  (data/set-page-state
    {:version version
     :csrf    *anti-forgery-token*
     :nonce   nonce
     :dev     dev}
    {:page         (apply
                     vector
                     handler
                     (flatten (seq route-params)))
     :query-params query-params}
    {:body    page-state
     :version version}))

(defn render-component-html [markup]
  {:status  200
   :headers {content-type html}
   :body    (str "<!DOCTYPE html>" (render-static-markup markup))})

(defn render-login []
  (render-component-html (components/login)))

(defn render-default [db-fn {:keys [loadjs session] :as req}]
  (assoc
    (render-component-html
      (components/default (static-state req (db-fn req)) loadjs))
    :session session))

(defn pr-edn [body]
  {:status  200
   :headers {content-type edn-type}
   :body    (pr-str {:version version :body body})})

(defn css [body]
  {:status 200
   :body   body})

(defn accepts-edn? [request]
  (= (get-in request [:headers "accept"] "*/*") edn-type))

(defn negotiate [{:keys [request-method] :as request}]
  [(if (accepts-edn? request)
     :edn
     :any)
   request-method])
