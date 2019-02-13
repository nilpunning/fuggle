(ns fuggle.xhr
  (:require [cljs.reader :refer [read-string]]
            [fuggle.routes :refer [path-for]])
  (:import [goog.net XhrIo EventType]))

(defn timeout [state]
  (-> state
      (assoc :progress (:progress-set state))
      (dissoc :progress-timeout :progress-set)))

(defn timeout! [state]
  (swap! state timeout))

(defn update-progress [state progress]
  (if (:progress-timeout state)
    (assoc state :progress-set progress :progress 1)
    (assoc state :progress-set progress :progress progress)))

(defn update-progress! [state progress]
  (swap! state update-progress progress))

(defn complete! [state]
  (update-progress! state 1)
  (js/clearTimeout (:progress-timeout @state))
  (swap!
    state
    assoc
    :progress-timeout
    (js/setTimeout (partial timeout! state) 500)))

(defn set-error-page [state body]
  (-> state
      (assoc :page [:error] :page-state body)
      (dissoc :timeout)))

(defn xhrio [state]
  (doto (XhrIo.)
    (.setProgressEventsEnabled true)
    (.listen
      (.-SUCCESS EventType)
      (fn [e]
        ((get-in @state [:tmp-state :on-success])
          (-> e .-target .getResponse read-string))))
    (.listen
      (.-ERROR EventType)
      (fn [e]
        (case (-> e .-target .getStatus)
          401 (.reload js/location true)
          (swap!
            state
            set-error-page
            (-> e .-target .getResponse read-string :body)))))
    (.listen
      (.-TIMEOUT EventType)
      #(swap! state assoc :timeout true))
    (.listen
      (.-PROGRESS EventType)
      #(update-progress!
         state
         (if (.-lengthComputable %)
           (+ (* (/ (.-loaded %) (.-total %)) 0.5) 0.25)
           0.50)))
    (.listen
      (.-COMPLETE EventType)
      #(complete! state))
    (.listen
      (.-READY EventType)
      #(update-progress! state 0))))

(defn reset-xhrio! [state xhrio on-success]
  (.abort xhrio)
  (update-progress! state 0.25)
  (swap! state assoc-in [:tmp-state :on-success] on-success))

(defn send
  "Send http request.
   {:xhr xhrio :csrf string} - atom
   method - string, ex. \"GET\"
   route - vector, ex. [:recipe :id 4]
   query-params - map ex. {\"q\" \"bacon\"}
   body - edn
   on-success - (fn [response-edn])"
  [state method route query-params body on-success]
  (let [{csrf :csrf {xhrio :xhrio} :tmp-state} @state]
    (reset-xhrio! state xhrio on-success)
    (.setTimeout
      js/window
      #(.send
         xhrio
         (path-for route query-params)
         method
         (pr-str body)
         (clj->js {"X-CSRF-Token" csrf
                   "Accept"       "application/edn"
                   "Content-Type" "application/edn"})))))

(defn xpost-multipart
  "Send http request.
   {:xhr xhrio :csrf string} - atom
   route - vector, ex. [:recipe :id 4]
   formdata - JS FormData object
   on-success - (fn [response-edn])"
  [state route formdata on-success]
  (let [{csrf :csrf {xhrio :xhrio} :tmp-state} @state]
    (reset-xhrio! state xhrio on-success)
    (.send
      xhrio
      (path-for route)
      "POST"
      (doto formdata (.append "__anti-forgery-token" csrf))
      (clj->js {"X-CSRF-Token" csrf
                "Accept"       "application/edn"}))))

(defn xget
  "Send GET request.
   {:xhr xhrio :csrf string} - atom
   route - vector, ex. [:recipe :id 4]
   query-params - map ex. {\"q\" \"bacon\"}
   on-success - (fn [response-edn])"
  [state route query-params on-success]
  (send state "GET" route query-params nil on-success))

(defn xpost
  "Send POST request.
   {:xhr xhrio :csrf string} - atom
   route - vector, ex. [:recipe :id 4]
   body - edn
   on-success - (fn [response-edn])"
  [state route body on-success]
  (send state "POST" route nil body on-success))


(defn xdelete
  "Send DELETE request.
   {:xhr xhrio :csrf string} - atom
   route - vector, ex. [:recipe :id 4]
   on-success - (fn [response-edn])"
  [state route on-success]
  (send state "DELETE" route nil nil on-success))