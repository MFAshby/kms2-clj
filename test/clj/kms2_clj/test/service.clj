(ns kms2-clj.test.service
  (:require
    [kms2-clj.service :refer :all]
    [clojure.test :refer :all])
  (:import (java.util Arrays)))

(deftest test-add-entrypoint
  (testing "You can add an entrypoint, store a secret on it, and fetch it back with the appropriate password"
    (let [secret-data (.getBytes "some-secret-data")
          node (add-entrypoint "some-password")
          store-result (store (:id node) "some-key" secret-data)
          fetch-result (fetch (:id node) "some-key" (:id node) "some-password")]
      (is store-result)
      (is (Arrays/equals fetch-result secret-data)))))