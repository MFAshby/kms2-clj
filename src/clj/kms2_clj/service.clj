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
  (let [encoder (Base64/getEncoder)]
    (.encodeToString encoder bytes)))

(defn- b64decode
  "Decodes the incoming base64 string as a byte array"
  [b64string]
  (let [decoder (Base64/getDecoder)]
    (.decode decoder b64string)))

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
        path (paths/shortest-between conn node entrypoint-id)
        relationship-urls (:relationships path)]
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
    new-node))

(defn grant-access
  "Grants access to an existing node. Returns true if access was granted, or an error"
  [id new-parent-id entrypoint-id secret]
  (errors/not-yet-implemented))
; Get the private key of id
; Get the public key of new-parent-id
; Encrypt the private key of id with the public key of

(defn ungrant-access
  "Removes access to an existing node. Returns true if access was un-granted"
  [id parent-id]
  (errors/not-yet-implemented))
; Remove the relationship between parent-id and id

(defn store
  "Stores some data encrypted on a node. Returns true if the data was stored, or an error"
  [id key value]
  (let [public-key (get-public-key id)
        encrypted-data (encrypt public-key value)]
    (nodes/set-property conn id key encrypted-data)))

(defn fetch
  "Fetches some encrypted data from a node, returns the data, or nil if you don't have access"
  [id key entrypoint-id password]
  (let [private-key (get-private-key id entrypoint-id password)
        encrypted-value (b64decode (get (nodes/get-properties conn id) (keyword key)))
        plaintext-value (decrypt private-key encrypted-value)]
    plaintext-value))

(defn unstore
  "Removes some encrypted data from a node. Returns true if the data was removed, or an error"
  [id key]
  (errors/not-yet-implemented))
; Removes the property with key from node with id

(defn delete
  "Deletes a node. Returns true if the data was removed, or an error"
  [id]
  (errors/not-yet-implemented))
; Deletes the node with id