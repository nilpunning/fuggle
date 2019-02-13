(ns fuggle.nonce-middleware)

(def nonce-limit 10)

(def nonces (atom {}))

(defn conj-nonce [nonces nonce]
  (conj (take (dec nonce-limit) nonces) nonce))

(defn remove-nonce [nonces nonce]
  (remove #(= nonce %) nonces))

(defn add-nonce [{:keys [user-id] :as req}]
  (let [nonce (.toString (java.util.UUID/randomUUID))]
    (swap! nonces update user-id conj-nonce nonce)
    (assoc req :nonce nonce)))

(defn wrap-nonce-middleware [handler]
  (fn [{{:keys [nonce]} :params :keys [uri user-id] :as req}]
    (if nonce
      (if (some #(= nonce %) (get @nonces user-id))
        (do
          (swap! nonces update user-id remove-nonce nonce)
          (handler req))
        {:status  303
         :headers {"Location" uri}})
      (handler req))))