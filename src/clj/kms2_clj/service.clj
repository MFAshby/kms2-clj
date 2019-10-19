(ns kms2-clj.service
  "Functions for the full service."
  (:require [kms2-clj.errors :as errors])
  (:import (com.google.crypto.tink KeysetHandle)
           (com.google.crypto.tink.hybrid HybridKeyTemplates)
           (javax.crypto.spec PBEKeySpec)
           (javax.crypto SecretKeyFactory)
           (com.google.crypto.tink.subtle AesGcmJce)))

(defn- get-plain-private-key
  "Gets and decrypts the private key of node with id. Returns the private key, or an error"
  [id entrypoint secret]
  (errors/not-yet-implemented))
; Find the shortest path from entrypoint to id
; Decrypt the private key on entrypoint with the secret
; Use the private key to decrypt the chain of private keys
; Return the decrypted private key of node with id

(defn add-entrypoint
  "Adds an entrypoint node to the graph. Returns the node, or an error"
  [secret]
  (doto [(KeysetHandle/generateNew HybridKeyTemplates/ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM)
         ()
         ]))
; Derive a key from the secret
; Generate a key pair
; Encrypt the private key with the secret
; Save a node with both the public and encypted private key on it
; Return the saved thing

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
  (errors/not-yet-implemented))
; Get the public key from id
; Encrypt the value with the public key
; Update the node with the new property

(defn fetch
  "Fetches some encrypted data from a node, returns the data, or an error"
  [id key entrypoint-id secret]
  (errors/not-yet-implemented))
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