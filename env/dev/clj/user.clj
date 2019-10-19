(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
    [kms2-clj.config :refer [env]]
    [clojure.spec.alpha :as s]
    [expound.alpha :as expound]
    [mount.core :as mount]
    [kms2-clj.core :refer [start-app]]
    [luminus-migrations.core :as migrations]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(defn start 
  "Starts application.
  You'll usually want to run this on startup."
  []
  (mount/start-without #'kms2-clj.core/repl-server))

(defn stop 
  "Stops application."
  []
  (mount/stop-except #'kms2-clj.core/repl-server))

(defn restart 
  "Restarts application."
  []
  (stop)
  (start))

(defn restart-db 
  "Restarts database."
  []
  ;(mount/stop #'kms2-clj.db.core/*db*)
  ;(mount/start #'kms2-clj.db.core/*db*)
  ;(binding [*ns* 'kms2-clj.db.core]
  ;  (conman/bind-connection kms2-clj.db.core/*db* "sql/queries.sql"))
  (println "Implement me!!!")
  )

(defn reset-db 
  "Resets database."
  []
  ;(migrations/migrate ["reset"] (select-keys env [:database-url]))
  (println "Implement me!!!")
  )

(defn migrate 
  "Migrates database up for all outstanding migrations."
  []
  ;(migrations/migrate ["migrate"] (select-keys env [:database-url]))
  (println "Implement me!!!")
  )

(defn rollback 
  "Rollback latest database migration."
  []
  ;(migrations/migrate ["rollback"] (select-keys env [:database-url]))
  (println "Implement me!!!")
  )

(defn create-migration 
  "Create a new up and down migration file with a generated timestamp and `name`."
  [name]
  ;(migrations/create name (select-keys env [:database-url]))
  (println "Implement me!!!")
  )


