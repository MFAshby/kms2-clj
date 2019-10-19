(ns kms2-clj.crypto
  "Provides all crypto operations required for implementing the kms
   Doesn't expose any implementation details. All keys and encrypted data
   are returned as byte arrays. All keys and plaintext data should be provided
   as byte arrays.

   Uses the google tink library internally.

   // TODO: MFA - Extract configuration options
   // TODO: MFA - Make derive-secret private as it should only be used internally
   "
  (:import (com.google.crypto.tink.config TinkConfig)
           (com.google.crypto.tink JsonKeysetWriter KeysetHandle CleartextKeysetHandle JsonKeysetReader)
           (com.google.crypto.tink.hybrid HybridKeyTemplates HybridEncryptFactory HybridDecryptFactory)
           (javax.crypto SecretKeyFactory AEADBadTagException)
           (com.google.crypto.tink.subtle AesGcmJce)
           (javax.crypto.spec PBEKeySpec)
           (java.io ByteArrayOutputStream)
           (java.security GeneralSecurityException)))

(TinkConfig/register)

; TODO: MFA - contemplating making this private
(defn derive-secret
  "Derives secret key from a password and some salt"
  [password salt]
  (let [spec (new PBEKeySpec (.toCharArray password) salt 100 128)
        secretkeyfactory (SecretKeyFactory/getInstance "PBKDF2WithHmacSHA1") ;
        secretkey (.generateSecret secretkeyfactory spec)
        ]
    (.getEncoded secretkey)))

(defn- encode-key
  "Encodes the key as a string"
  [keyset-handle]
  (let [output-stream (new ByteArrayOutputStream)
        keysetwriter (JsonKeysetWriter/withOutputStream output-stream)]
    (do
      (CleartextKeysetHandle/write keyset-handle keysetwriter)
      (.toByteArray output-stream))))

(defn generate-key-pair
  "Generates an asymmetric key pair. Returns the encoded keys, and the private key is encrypted
  with the provided master key"
  []
  (let [private-keyset-handle (KeysetHandle/generateNew HybridKeyTemplates/ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM)
        public-keyset-handle (.getPublicKeysetHandle private-keyset-handle)
        public-key (encode-key public-keyset-handle)
        private-key (encode-key private-keyset-handle)]
    {:public-key  public-key
     :private-key private-key}))

(defn- keyset-handle
  "Reads a JSON encoded key and returns a KeysetHandle"
  [key]
  (let [reader (JsonKeysetReader/withBytes key)
        keyset-handle (CleartextKeysetHandle/read reader)]
    keyset-handle))

(defn encrypt
  "Encrypts some data with an asymmetric key"
  [key data]
  (let [keyset-handle (keyset-handle key)
        hybrid-encrypt (HybridEncryptFactory/getPrimitive keyset-handle)]
    (.encrypt hybrid-encrypt data nil)))

(defn decrypt
  "Decrypts some data with an asymmetric key"
  [key encrypted]
  (try
    (let [keyset-handle (keyset-handle key)
          hybrid-decrypt (HybridDecryptFactory/getPrimitive keyset-handle)]
      (.decrypt hybrid-decrypt encrypted nil))
    (catch GeneralSecurityException _ nil)))

(defn password-encrypt-key
  "Encrypts the key with a password and some salt"
  [key password salt]
  (let [secret-key (derive-secret password salt)
        aead (new AesGcmJce secret-key)
        keyset-handle (keyset-handle key)
        output-stream (new ByteArrayOutputStream)
        keyset-writer (JsonKeysetWriter/withOutputStream output-stream)]
    (do (.write keyset-handle keyset-writer aead)
        (.toByteArray output-stream))))

(defn password-decrypt-key
  "Decrypts the key with a password and some salt. Returns nil if it couldn't be decrypted"
  [encrypted-key password salt]
  (try
    (let [secret-key (derive-secret password salt)
          aead (new AesGcmJce secret-key)
          reader (JsonKeysetReader/withBytes encrypted-key)
          output-stream (new ByteArrayOutputStream)
          writer (JsonKeysetWriter/withOutputStream output-stream)
          keyset-handle (KeysetHandle/read reader aead)]
      (do (CleartextKeysetHandle/write keyset-handle writer)
          (.toByteArray output-stream)))
    (catch GeneralSecurityException _ nil)))

(defn key-encrypt-key
  "Encrypts a key with another key"
  [key other-key]
  (encrypt other-key key))

(defn key-decrypt-key
  "Decrypts a key with another key"
  [encrypted-key other-key]
  (decrypt other-key encrypted-key))