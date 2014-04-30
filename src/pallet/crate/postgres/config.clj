(ns pallet.crate.postgres.config
  "Postgres configuration"
  (:require
   [clojure.string :as string :refer [join]]))

;;; pg_hba.conf

(def ^{:private true}
  auth-methods #{"trust" "reject" "md5" "password" "gss" "sspi" "krb5"
                                     "ident" "ldap" "radius" "cert" "pam"})
(def ^{:private true}
  ip-addr-regex #"[0-9]{1,3}.[0-9]{1,3}+.[0-9]{1,3}+.[0-9]{1,3}+")

(defn- valid-hba-record?
  "Takes an hba-record as input and minimally checks that it could be a valid
   record."
  [{:keys [connection-type database user auth-method address ip-mask]
    :as record-map}]
  (and (#{"local" "host" "hostssl" "hostnossl"} (name connection-type))
       (every? #(not (nil? %)) [database user auth-method])
       (auth-methods (name auth-method))))

(defn- vector-to-map
  [record]
  (case (name (first record))
    "local" (apply
             hash-map
             (interleave
              [:connection-type :database :user :auth-method
               :auth-options]
              record))
    ("host"
     "hostssl"
     "hostnossl") (let [[connection-type database user address
                         & remainder] record]
     (if (re-matches
          ip-addr-regex (first remainder))
       ;; Not nil so must be an IP mask.
       (apply
        hash-map
        (interleave
         [:connection-type :database :user
          :address :ip-mask :auth-method
          :auth-options]
         record))
       ;; Otherwise, it may be an auth-method.
       (if (auth-methods
            (name (first remainder)))
         (apply
          hash-map
          (interleave
           [:connection-type :database :user
            :address :auth-method
            :auth-options]
           record))
         (throw
          (ex-info
           (format
            "The fifth item in %s does not appear to be an IP mask or auth method."
            (pr-str record))
           {:type :postgres-invalid-hba-record})))))
    (throw
     (ex-info
      (format
       "The first item in %s is not a valid connection type."
       (name record)))
     {:type :postgres-invalid-hba-record})))

(defn- record-to-map
  "Takes a record given as a map or vector, and turns it into the map version."
  [record]
  (cond
   (map? record) record
   (vector? record) (vector-to-map record)
   :else
   (throw
    (ex-info
     (format "The record %s must be a vector or map." (name record))
     {:type :postgres-invalid-hba-record}))))

(defn- format-auth-options
  "Given the auth-options map, returns a string suitable for inserting into the
   file."
  [auth-options]
  (string/join "," (map #(str (first %) "=" (second %)) auth-options)))

(defn format-hba
  [record]
  (let [record-map (record-to-map record)
        record-map (assoc record-map :auth-options
                          (format-auth-options (:auth-options record-map)))
        ordered-fields (map #(% record-map "")
                            [:connection-type :database :user :address :ip-mask
                             :auth-method :auth-options])
        ordered-fields (map name ordered-fields)]
    (if (valid-hba-record? record-map)
      (str (string/join "\t" ordered-fields) "\n"))))

(defn hba
  "Return content for pg_hba.conf given a sequence of hba permission
  entries."
  [permissions]
  (join (map format-hba permissions)))

;;; postgresql.conf, recovery.conf

(defn database-data-directory
  "Given a settings map and a database name, return the data directory
   for the database."
  [settings cluster]
  (format "%s/%s/recovery.conf" (-> settings :options :data_directory) cluster))

(defn- parameter-escape-string
  "Given a string, escapes any single-quotes."
  [string]
  (apply str (replace {\' "''"} string)))

(defn- format-parameter-value
  [value]
  (cond (number? value)
        (str value)
        (string? value)
        (str "'" value "'")
        (vector? value)
        (str "'" (string/join "," (map name value)) "'")
        (or (= value true) (= value false))
        (str value)
        :else
        (throw
         (ex-info
          (format
           (str
            "Parameters must be numbers, strings, or vectors of such. "
            "Invalid value %s") (pr-str value))
          {:type :postgres-invalid-parameter
           :value value}))))

(defn format-conf
  "Given a key/value pair in a vector, formats it suitably for the
   postgresql.conf file.
   The value should be either a number, a string, or a vector of such."
  [[key value]]
  (let [key-str (name key)
        parameter-str (format-parameter-value value)]
    (str key-str " = " parameter-str "\n")))

(defn conf
  "Return content for postgresql.conf given a sequence of entries."
  [entries]
  (join (map format-conf entries)))

;;; start.conf

(defn format-start
  [[key value]]
  (name value))

(defn start
  "Return content for postgresql.conf given a sequence of entries."
  [entries]
  (join (map format-start entries)))
