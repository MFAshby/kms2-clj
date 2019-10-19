(ns kms2-clj.test.crypto
  (:require
    [kms2-clj.crypto :refer :all]
    [clojure.test :refer :all]
    )
  (:import (java.util Arrays)
           (com.google.crypto.tink.subtle AesGcmJce)))

(def some-salt (byte-array (map byte '(0 1 2 3 4))))
(def some-plaintext (byte-array (map byte '(5 4 8 7 8))))
(def some-password "some-password")

(deftest test-derive-secret
  (testing "a secret key can encrypt and decrypt"
    (let [salt some-salt
          plaintext some-plaintext
          key (derive-secret some-password salt)
          aead (new AesGcmJce key)
          encrypted (.encrypt aead plaintext nil)
          decrypted (.decrypt aead encrypted nil)]
      (is key)
      (is (not (Arrays/equals encrypted plaintext))
      (is (Arrays/equals decrypted plaintext))
      )))
  (testing "A different key is derived for different passwords "
    (let [salt some-salt
          plaintext some-plaintext
          key1 (derive-secret "secret-1" salt)
          key2 (derive-secret "secret-2" salt)
          aead1 (new AesGcmJce key1)
          aead2 (new AesGcmJce key2)
          encrypted1 (.encrypt aead1 plaintext nil)
          encrypted2 (.encrypt aead2 plaintext nil)
          ]
      (is (not (Arrays/equals encrypted1 encrypted2)))))
  (testing "A different key is derived for different salts"
    (let [salt1 (byte-array (map byte '(1 2 3)))
          salt2 (byte-array (map byte '(4 5 6)))
          plaintext some-plaintext
          key1 (derive-secret some-password salt1)
          key2 (derive-secret some-password salt2)
          aead1 (new AesGcmJce key1)
          aead2 (new AesGcmJce key2)
          encrypted1 (.encrypt aead1 plaintext nil)
          encrypted2 (.encrypt aead2 plaintext nil)]
      (is (not (Arrays/equals encrypted1 encrypted2))))))

(deftest test-encrypt-data
  (testing "Test encrypting & decrypting data with a generated key pair"
    (let [keypair (generate-key-pair)
          plaintext some-plaintext
          public-key (:public-key keypair)
          private-key (:private-key keypair)
          encrypted (encrypt public-key plaintext)
          decrypted (decrypt private-key encrypted)]
      (is (Arrays/equals plaintext decrypted))))
  (testing "Generating multiple keys they are not the same"
    (let [keypair1 (generate-key-pair)
          keypair2 (generate-key-pair)
          private-key1 (:private-key keypair1)
          private-key2 (:private-key keypair2)
          public-key1 (:public-key keypair1)
          public-key2 (:public-key keypair2)]
      (is (not (Arrays/equals private-key1 private-key2)))
      (is (not (Arrays/equals public-key1 public-key2)))))
  (testing "Decrypting with the wrong key fails"
    (let [keypair1 (generate-key-pair)
          keypair2 (generate-key-pair)
          encrypted-data (encrypt (:public-key keypair1) some-plaintext)
          decrypt-result (decrypt (:private-key keypair2) encrypted-data)]
      (is (nil? decrypt-result)))))

(deftest test-encrypt-key
  (testing "Test encrypting and decrypting key with a password"
    (let [key (:private-key (generate-key-pair))
          encrypted-key (password-encrypt-key key some-password some-salt)
          decrypted-key (password-decrypt-key encrypted-key some-password some-salt)]
      (is (Arrays/equals key decrypted-key))))
  (testing "Can't decrypt a key with the wrong password"
    (let [key (:private-key (generate-key-pair))
          encrypted-key (password-encrypt-key key some-password some-salt)
          decrypt-result (password-decrypt-key encrypted-key "bogus" some-salt)]
      (is (nil? decrypt-result))))
  (testing "Test encrypting and decrypting key with another key"
    (let [key (:private-key (generate-key-pair))
          other-key-pair (generate-key-pair)
          encrypted-key (key-encrypt-key key (:public-key other-key-pair))
          decrypted-key (key-decrypt-key encrypted-key (:private-key other-key-pair))]
      (is (Arrays/equals key decrypted-key)))))