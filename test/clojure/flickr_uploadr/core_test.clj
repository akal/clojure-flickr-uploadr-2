(ns flickr-uploadr.core-test
  (:require [clojure.test :refer :all]
            [flickr-uploadr.core :refer :all]))

;; thanks to http://www.wackylabs.net/2011/12/oauth-and-flickr-part-2/

(def ^:dynamic consumer-secret "CONSUMER_SECRET")
(def ^:dynamic token-secret "")
(def ^:dynamic test-verb "GET")
(def ^:dynamic test-url "https://www.flickr.com/services/oauth/request_token")
(def ^:dynamic test-params {"oauth_callback" "https%3A%2F%2Fwww.example.com"
                            "oauth_consumer_key" "CONSUMER_KEY"
                            "oauth_nonce" "3d37fb9e-662a-48f3-aed3-4695ebbdf4f5"
                            "oauth_signature_method" "HMAC-SHA1"
                            "oauth_timestamp" "1491092938017"
                            "oauth_version" "1.0"})

(def ^:dynamic expected-request-signature-base "GET&https%3A%2F%2Fwww.flickr.com%2Fservices%2Foauth%2Frequest_token&oauth_callback%3Dhttps%253A%252F%252Fwww.example.com%26oauth_consumer_key%3DCONSUMER_KEY%26oauth_nonce%3D3d37fb9e-662a-48f3-aed3-4695ebbdf4f5%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1491092938017%26oauth_version%3D1.0")
(def ^:dynamic expected-request-signature "CRB5XCQzlQSI3/QHaqhx2YR+V58=")

(deftest request-signature-base-test
  (testing "request signature base string"
    (is (=
         expected-request-signature-base
         (#'flickr-uploadr.core/request-signature-base test-verb
                                                       test-url
                                                       test-params)))))

(deftest request-signature-test
  (testing "request signature"
    (is (=
         expected-request-signature
         (#'flickr-uploadr.core/request-signature test-url
                                                  test-params
                                                  consumer-secret
                                                  token-secret)))))
