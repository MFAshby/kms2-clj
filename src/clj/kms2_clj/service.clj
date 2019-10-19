(ns kms2-clj.service
  "Functions for the full service."
  (:require [kms2-clj.errors :as errors]
            [kms2-clj.crypto :refer :all]
            [crypto.random :as random]
            [clojurewerkz.neocons.rest :as nc]
            [clojurewerkz.neocons.rest.nodes :as nodes]
            [clojurewerkz.neocons.rest.relationships :as rels])
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

(defn add-entrypoint
  "Adds an entrypoint node to the graph. Returns the node, or an error"
  [password]
  (let [salt (random/bytes 8) ; move this to config and crypto namespace
        keypair (generate-key-pair)
        public-key (:public-key keypair)
        private-key (:private-key keypair)
        encrypted-private-key (password-encrypt-key private-key password salt)
        node (nodes/create conn {:salt (b64encode salt)
                                 :public-key (b64encode public-key)
                                 :encrypted-private-key (b64encode encrypted-private-key)})]
    node))

(defn add-node
  "Adds a non-entrypoint node to the graph. Returns the node, or an error"
  [parent-id]
  (errors/not-yet-implemented))
; Get the public key of the parent
; Generate a key pair
; Encrypt the private key with the public key of the parent
; Save a node with the public key on it
; Save a relationship from the parent to this node with the encrypted private key on it

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
  (let [properties (nodes/get-properties conn id)
        public-key (b64decode (:public-key properties))
        encrypted-data (encrypt public-key value)]
    (nodes/set-property conn id key encrypted-data)))

(defn fetch
  "Fetches some encrypted data from a node, returns the data, or nil if you don't have access"
  [id key entrypoint-id password]
  (let [entrypoint-props (nodes/get-properties conn entrypoint-id)
        salt (b64decode (:salt entrypoint-props))
        encrypted-private-key (b64decode (:encrypted-private-key entrypoint-props))
        private-key (password-decrypt-key encrypted-private-key password salt)
        encrypted-value (b64decode (get (nodes/get-properties conn id) (keyword key)))
        plaintext-value (decrypt private-key encrypted-value)
        ]
    plaintext-value))
; Get the private key of node id
; Get the encrypted property referred to by key
; Decrypt it with the private key

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