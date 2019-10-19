(ns kms2-clj.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[kms2-clj started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[kms2-clj has shut down successfully]=-"))
   :middleware identity})
