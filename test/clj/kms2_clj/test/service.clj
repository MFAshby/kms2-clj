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

(deftest test-delete
  (testing "You can delete a node and an entrypoint"
    (let [entrypoint (add-entrypoint "some-password")
          node (add-node (:id entrypoint))
          e-store-result (store (:id entrypoint) "some-key" (.getBytes "some-secret-entrypoint-data"))
          n-store-result (store (:id node) "some-key" (.getBytes "some-secret-node-data"))
          entrypoint-delete-result (delete (:id entrypoint))
          node-delete-result (delete (:id node))
          e-fetch-result (fetch (:id entrypoint) "some-key" (:id entrypoint) "some-password")
          n-fetch-result (fetch (:id node) "some-key" (:id entrypoint) "some-password")]
      (is e-store-result)
      (is n-store-result)
      (is entrypoint-delete-result)
      (is node-delete-result)
      (is (not e-fetch-result))
      (is (not n-fetch-result)))))

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

(deftest test-grant-access
  (testing "Add two entrypoints and a node. Entrypoint one grants access to entrypoint 2
            and then entrypoint 2 can be used to read the data"
    (let [e1 (add-entrypoint "pass-1")
          e2 (add-entrypoint "pass-2")
          n1 (add-node (:id e1))
          grant-result (grant-access (:id n1) (:id e2) (:id e1) "pass-1")
          store-result (store (:id n1) "some-key" some-secret-data)
          fetch-result (fetch (:id n1) "some-key" (:id e2) "pass-2")]
      (is grant-result)
      (is store-result)
      (is (Arrays/equals fetch-result some-secret-data)))))

(deftest test-ungrant-access
  (testing "Remove access between two nodes"
    (let [e1 (add-entrypoint "pass-1")
          n1 (add-node (:id e1))
          store-result (store (:id n1) "some-key" some-secret-data)
          ungrant-result (ungrant-access (:id n1) (:id e1))
          fetch-result (fetch (:id n1) "some-key" (:id e1) "pass-1")]
      (is store-result)
      (is ungrant-result)
      (is (not fetch-result)))))

(deftest test-unstore
  (testing "Remove a property from a node"
    (let [e (add-entrypoint "something")
          store-result (store (:id e) "some-key" (.getBytes "some-secret-data"))
          unstore-result (unstore (:id e) "some-key")
          fetch-result (fetch (:id e) "some-key" (:id e) "something")]
      (is store-result)
      (is unstore-result)
      (is (nil? fetch-result)))))