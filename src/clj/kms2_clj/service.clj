(ns kms2-clj.service
  "Functions for the full service."
  (:require [kms2-clj.errors :as errors]
            [kms2-clj.crypto :refer :all]
            [crypto.random :as random]
            [clojurewerkz.neocons.rest :as nc]
            [clojurewerkz.neocons.rest.nodes :as nodes]
            [clojurewerkz.neocons.rest.relationships :as rels]
            [clojurewerkz.neocons.rest.paths :as paths])
  (:import (java.util Base64)))

(def conn (nc/connect "http://localhost:7474/db/data" "neo4j" "test")) ; Move this to config

(defn- b64encode
  "Encodes the incoming byte array as base 64 string"
  [bytes]
  (if bytes
    (let [encoder (Base64/getEncoder)]
      (.encodeToString encoder bytes))))

(defn- b64decode
  "Decodes the incoming base64 string as a byte array"
  [b64string]
  (if b64string
    (let [decoder (Base64/getDecoder)]
      (.decode decoder b64string))))

(defn- get-public-key
  "Fetches the public key from a node"
  [node-id]
  (let [properties (nodes/get-properties conn node-id)]
    (b64decode (:public-key properties))))

(defn- get-private-key
  "Fetches the private key for a node, using the desired entrypoint and password.
  Returns the desired private key, or nil if there was an error with the password,
  or if there was no path from the entrypoint"
  [node-id entrypoint-id password]
  (let [[entrypoint node] (nodes/get-many conn [entrypoint-id node-id])
        entrypoint-props (:data entrypoint)
        encrypted-private-key (b64decode (:encrypted-private-key entrypoint-props))
        salt (b64decode (:salt entrypoint-props))
        entrypoint-private-key (password-decrypt-key encrypted-private-key password salt)
        path (paths/shortest-between conn node entrypoint-id :max-depth 100)
        relationship-urls (reverse (:relationships path))]
    (reduce (fn [prev-pk relationship-url]
              (let [relationship (rels/fetch-from conn relationship-url)
                    encrypted-private-key (b64decode (:encrypted-private-key (:data relationship)))]
                (key-decrypt-key encrypted-private-key prev-pk)))
            entrypoint-private-key
            relationship-urls)))

(defn add-entrypoint
  "Adds an entrypoint node to the graph. Returns the node"
  [password]
  (let [salt (random/bytes 8)                               ; move this to config and crypto namespace
        keypair (generate-key-pair)
        public-key (:public-key keypair)
        private-key (:private-key keypair)
        encrypted-private-key (password-encrypt-key private-key password salt)
        node (nodes/create conn {:salt                  (b64encode salt)
                                 :public-key            (b64encode public-key)
                                 :encrypted-private-key (b64encode encrypted-private-key)})]
    node))

(defn add-node
  "Adds a non-entrypoint node to the graph. Returns the node, or an error"
  [parent-id]
  (let [parent (nodes/get conn parent-id)
        parent-public-key (get-public-key parent-id)
        keypair (generate-key-pair)
        private-key (:private-key keypair)
        public-key (:public-key keypair)
        encrypted-private-key (key-encrypt-key private-key parent-public-key)
        new-node (nodes/create conn {:public-key (b64encode public-key)})
        new-rel (rels/create conn parent new-node "can-decrypt" {:encrypted-private-key (b64encode encrypted-private-key)})]
    (if (and (map some? [new-node new-rel]))                ; Check both got created before returning a success value
      new-node)))

(defn grant-access
  "Grants access to an existing node. Returns true if access was granted."
  [id new-parent-id entrypoint-id password]
  (let [[node new-parent] (nodes/get-many conn [id new-parent-id])
        target-private-key (get-private-key id entrypoint-id password)
        new-parent-public-key (get-public-key new-parent-id)
        encrypted-private-key (key-encrypt-key target-private-key new-parent-public-key)
        new-rel (rels/create conn new-parent node "can-decrypt" {:encrypted-private-key (b64encode encrypted-private-key)})]
    (some? new-rel)))

(defn ungrant-access
  "Removes access to an existing node. Returns true if access was un-granted"
  [id parent-id]
  (let [[node parent] (nodes/get-many conn [id parent-id])
        relationships (rels/all-outgoing-between conn parent node nil)
        relationship-ids (map :id relationships)]
    (rels/delete-many conn relationship-ids)
    true))

(defn store
  "Stores some data encrypted on a node. Returns true if the data was stored"
  [id key value]
  (let [public-key (get-public-key id)
        encrypted-data (encrypt public-key value)
        set-result (nodes/set-property conn id key encrypted-data)]
    (some? set-result)))

(defn fetch
  "Fetches some encrypted data from a node, returns the data, or nil if you don't have access"
  [id key entrypoint-id password]
  (let [private-key (get-private-key id entrypoint-id password)
        encrypted-value (b64decode (get (nodes/get-properties conn id) (keyword key)))]
    (if encrypted-value
      (decrypt private-key encrypted-value))))

(defn unstore
  "Removes some encrypted data from a node. Returns true if the data was removed, or an error
  This could probably be a lot more efficient"
  [id key]
  (let [props (nodes/get-properties conn id)
        _ (nodes/delete-properties conn id)
        new-props (filter #(not (= (keyword key) (first %))) (seq props))]
    (doseq [[k v] new-props]
      (nodes/set-property conn id k v))
    true))

(defn delete
  "Deletes a node. Returns true if the data was removed, or an error"
  [id]
  (clojurewerkz.neocons.rest.cypher/query conn "match (a :id $id) detach delete a" {:id id}))
; Deletes the node with id