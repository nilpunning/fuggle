(ns fuggle.core
  (:require [trptcolin.versioneer.core :refer [get-version]]))

(def version
   (get-version "fuggle" "fuggle"))