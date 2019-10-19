(ns kms2-clj.errors)

(defn not-yet-implemented
  "Throws UnsupportedOperationException with an appropriate message"
  []
  (throw (new UnsupportedOperationException "Not yet implemented")))