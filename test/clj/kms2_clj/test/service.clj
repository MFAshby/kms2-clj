(ns kms2-clj.test.service
  (:require
    [kms2-clj.service :refer :all]
    [clojure.test :refer :all])
  (:import (java.util Arrays)))

(def some-secret-data (.getBytes "some-secret-data"))

(deftest test-add-entrypoint
  (testing "You can add an entrypoint, store a secret on it, and fetch it back with the appropriate password"
    (let [secret-data some-secret-data
          node (add-entrypoint "some-password")
          store-result (store (:id node) "some-key" secret-data)
          fetch-result (fetch (:id node) "some-key" (:id node) "some-password")]
      (is store-result)
      (is (Arrays/equals fetch-result secret-data)))))

(deftest test-add-node
  (testing "Add an entrypoint, then a linked node. Store and fetch some data on the linked node."
    (let [entrypoint (add-entrypoint "some-password")
          node (add-node (:id entrypoint))
          store-result (store (:id node) "some-key" some-secret-data)
          fetch-result (fetch (:id node) "some-key" (:id entrypoint) "some-password")]
      (is store-result)
      (is (Arrays/equals fetch-result some-secret-data))))
  (testing "Add an entrypoint, then a chain of several linked nodes.
            Store and fetch some data on the final node"
    (let [entrypoint (add-entrypoint "some-password")
          node1 (add-node (:id entrypoint))
          node2 (add-node (:id node1))
          node3 (add-node (:id node2))
          store-result (store (:id node3) "some-key" some-secret-data)
          fetch-result (fetch (:id node3) "some-key" (:id entrypoint) "some-password")]
      (is store-result)
      (is (Arrays/equals fetch-result some-secret-data)))))