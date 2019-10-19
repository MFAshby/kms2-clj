(ns kms2-clj.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [kms2-clj.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[kms2-clj started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[kms2-clj has shut down successfully]=-"))
   :middleware wrap-dev})
