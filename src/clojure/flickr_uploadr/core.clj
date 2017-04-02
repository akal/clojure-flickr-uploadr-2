(ns flickr-uploadr.core
  (:require
      [clj-http.client :as client] ;; https://github.com/dakrone/clj-http#get
      [clojure.pprint :as pprint]
      [clojure.string :as string]
      [clojure.walk :as walk]
      [flickr-uploadr.config :as config]
   )
  )

;; (refer 'devtools) -> -info -all-methods

(def ^:dynamic *auth-endpoint* "https://www.flickr.com/services/oauth/")
(def ^:dynamic *rest-endpoint* "https://api.flickr.com/services/rest")

(defn- urlencode [x]
  (java.net.URLEncoder/encode x))

(defn- sha1-hash [key value-to-digest]
  (.encodeToString
   (java.util.Base64/getEncoder)
   (org.apache.commons.codec.digest.HmacUtils/hmacSha1 key value-to-digest)))

(defn- request-param-strings
  "Returns ordered vector of param=value strings"
  [^java.util.List ordered-params-list]
  (map #(str (first %) "=" (second %)) ordered-params-list))

(defn- request-signature-base
  "Return base string for request signature, VERB + & + URL + & + encoded sorted params"
  [^String verb ^String url ^java.util.Map params]
  (let [param-strings (request-param-strings (sort params))
        encoded-joined-param-string (urlencode (string/join "&" param-strings))
        all-strings (concat [verb (urlencode url) encoded-joined-param-string])]
    (string/join "&" all-strings)))

(defn- request-signature
  ""
  [^String url ^java.util.Map params ^String consumer-secret ^String token-secret]
  (let [key (str consumer-secret "&" token-secret)
        value-to-digest (request-signature-base "GET" url params)
        signature (sha1-hash key value-to-digest)]
    signature))

(defn- signed-request
  "Returns the complete url with the oauth_signature parameter"
  [^String url ^java.util.Map params ^String consumer-secret ^String token-secret]
  (let [signature (request-signature url params consumer-secret token-secret)
        all-params (conj params {"oauth_signature" (urlencode signature)})
        all-param-strings (request-param-strings all-params)
        complete-url (str url "?" (string/join "&" all-param-strings))]
    complete-url))

(defn- response-body-to-map
  "Parses returned values to a map and converts keys to keywords"
  [response]
  (walk/keywordize-keys
   (into {}
         (map #(string/split % #"=")
              (string/split (:body response) #"&")))))

(defn- get-default-params [configuration]
  {"oauth_nonce" (str (java.util.UUID/randomUUID))
   "oauth_timestamp" (str (.getTime (new java.util.Date)))
   "oauth_consumer_key" (:api-key configuration)
   "oauth_signature_method" "HMAC-SHA1"
   "oauth_version" "1.0"})

;; authenticate with flickr
(defn authenticate-request-token []
  (let [configuration (config/get-config)
        url (str *auth-endpoint* "request_token")
        params (conj (get-default-params configuration)
                      {"oauth_callback" (urlencode "https://www.example.com")})
        consumer-secret (:api-secret configuration)
        token-secret ""
        complete-url (signed-request url params consumer-secret token-secret)
        response (client/get complete-url)]
    (response-body-to-map response)))

(defn authorize-user [oauth-token]
  (let [configuration (config/get-config)
        url (str *auth-endpoint* "authorize")
        complete-url (str url "?oauth_token=" oauth-token)]
    (println "Open " complete-url)))

(defn get-access-token [oauth-token oauth-token-secret verifier]
  (let [configuration (config/get-config)
        url (str *auth-endpoint* "access_token")
        params (conj (get-default-params configuration)
                     {"oauth_verifier" verifier
                      "oauth_token" oauth-token})
        consumer-secret (:api-secret configuration)
        token-secret oauth-token-secret
        complete-url (signed-request url params consumer-secret token-secret)
        response (client/get complete-url)]
    (response-body-to-map response)))


(defn test-call [oauth-token oauth-token-secret]
  (let [configuration (config/get-config)
        url *rest-endpoint*
        params (conj (get-default-params configuration)
                     {"nojsoncallback" "1"
                      "format" "json"
                      "oauth_token" oauth-token
                      "method" "flickr.test.login"})
        consumer-secret (:api-secret configuration)
        token-secret oauth-token-secret
        complete-url (signed-request url params consumer-secret token-secret)
        response (client/get complete-url)]
    response))

(defn authenticate
  "Main authentication flow"
  []
  (let [authenticate-tokens (authenticate-request-token)
        _ (authorize-user (:oauth_token authenticate-tokens))
        _ (println "Enter the \"oauth_verifier\" from the url")
        authorize-verifier (read-line)
        access-tokens (get-access-token (:oauth_token authenticate-tokens)
                                        (:oauth_token_secret authenticate-tokens)
                                        authorize-verifier)]
    (test-call (:oauth_token access-tokens)
               (:oauth_token_secret access-tokens))))
