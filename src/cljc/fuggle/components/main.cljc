(ns fuggle.components.main
  (:require [clojure.string :as string]
            [rum.core :as rum]
            [#?(:clj clojure.pprint :cljs cljs.pprint) :refer [pprint]]
            [fuggle.components.general :refer [dissoc-mutable-state]]
            [fuggle.components.settings-view :as settings-view]
            [fuggle.components.settings-edit :as settings-edit]
            [fuggle.components.search :as search]
            [fuggle.components.recipe-view :as recipe-view]
            [fuggle.components.recipe-edit :as recipe-edit]))

(defn remove-trailing-spaces
  "Causes conflict between clj and cljs pprint."
  [s]
  (string/replace s #" \n" "\n"))

(defn debug-pre [state]
  (when (or (:dev state) #?(:cljs (= (.-dev js/window) true) :clj false))
    [:pre.no-print
     {:style {:font-family "consolas, monaco, monospace"
              :font-size   "14px"}}
     (-> state
         dissoc-mutable-state
         pprint
         with-out-str
         remove-trailing-spaces)]))

(defn renderer [[page & _]]
  (case page
    :error search/error
    :settings-view settings-view/page
    :settings-edit settings-edit/page
    :search search/page
    :style-test search/style-test
    :recipe-new recipe-edit/page
    :recipe-view recipe-view/page
    :recipe-edit recipe-edit/page
    :recipe-delete recipe-view/delete))

(rum/defc do-main < rum/static [state s]
  [:div
   ((renderer (:page s)) state s)
   (debug-pre s)])

(rum/defc main < rum/static [state]
  (do-main state @state))