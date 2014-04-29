(ns pallet.crate.postgres.kb
  "Knowledge base for postgres install and configuration"
  (:require
   [clojure.string :as string :refer [split]]
   [pallet.version-dispatch :refer [os-map]]
   [pallet.compute :refer [os-hierarchy]]
   [pallet.utils :refer [deep-merge]]
   [schema.core :as schema :refer [enum validate]]))

(def Layout
  {:bin String
   :default-cluster-name String
   ;; :default-service String
   :has-multicluster-service schema/Bool
   :has-pg-wrapper schema/Bool
   :initdb-via (enum :initdb :service)
   :postgresql_file String
   :service String
   :share String
   :use-port-in-pidfile schema/Bool
   :owner String
   :wal_directory String
   :options {:external_pid_file String
             :data_directory String
             :hba_file String
             :ident_file String
             :unix_socket_directory String
             schema/Keyword schema/Any}})

(defmulti layout-settings
  "Determine the layout of packages for the specified os-family or layout."
  (fn [os-family layout version]
    {:pre [(keyword? layout)
           (string? version)]}
    layout)
  :hierarchy #'os-hierarchy)

(prefer-method layout-settings :amzn-linux :rh-base)

(defn base-layout
  "Base layout used as a default with common options."
  []
  {:service "postgresql"
   :owner "postgres"
   :has-pg-wrapper false
   :has-multicluster-service false
   :initdb-via :initdb
   :use-port-in-pidfile false
   :options {:external_pid_file "/var/run/postgresql.pid"}})


;;; # System Packages

;;; Default Postgres package version

(def postgres-package-version
  "Default version for distros."
  (atom                                 ; allow for open extension
   (os-map
    {{:os :linux} [8]
     {:os :amzn-linux :os-version [[2013]]} [9 2]
     {:os :ubuntu :os-version [[12] [13 10]]} [9 1]
     {:os :ubuntu :os-version [[14 04]]} [9 3]})))


;;; ## Yum system packages
(defn yum-packages
  "Return a sequence of package names for system packages on yum based
  systems."
  [version components]
  {:pre [(string? version)]}
  (map
   #(format "postgresql%s-%s" (first (split version #"\.")) (name %))
   components))

;;; ## Apt system packages
(defn apt-packages
  "Return a sequence of package names for system packages on apt based
  systems."
  [version components]
  {:pre [(string? version)]}
  (conj
   (map
    #(format "postgresql-%s-%s" version (name %))
    components)
   (format "postgresql-%s" version)))

;;; # Postgres RPM Repository

;;; See http://yum.postgresql.org/

(def ^{:dynamic true} *pgdg-repo-versions*
  "Versions available from the postgres RPM repository."
  {"9.0" "9.0-5"
   "9.1" "9.1-6"
   "9.2" "9.2-7"
   "9.3" "9.3-1"})

(defn base-name
  [os-family]
  (if (= os-family :fedora)
    "fedora"
    "redhat"))

(defn base-pkg-name
  [os-family]
  (if (= os-family :fedora)
    "fedora"
    "rhel"))

(defn distro-name
  [os-family]
  (name os-family))

(def pkg-distro-names
  {:rhel "redhat"})

(defn pkg-distro-name
  [os-family]
  (os-family pkg-distro-names (name os-family)))

(defn pgdg-url
  [version os-family os-version arch]
  {:pre [(string? version)
         (#{"i386" "x86_64"} arch)]}
  (format
   "http://yum.postgresql.org/%s/%s/%s-%s-%s/pgdg-%s%s-%s.noarch.rpm"
   version
   (base-name os-family)
   (base-pkg-name os-family) os-version arch
   (pkg-distro-name os-family)
   (string/replace version "." "")
   (*pgdg-repo-versions* version)))

(defn pgdg-packages
  "Return package names for Postgres RPM repository packages, for the given
  postgres version and sequence of component keywords."
  [version components]
  (map
   #(str "postgresql" (string/replace version "." "") "-" (name %))
   components))

;;; # Postgres APT Repository

;;; See http://wiki.postgresql.org/wiki/Apt

(def postgres-apt
  "Repository for postgres Apt packages."
  {:url "http://apt.postgresql.org/pub/repos/apt/"
   :key-url "https://www.postgresql.org/media/keys/ACCC4CF8.asc"})

(defn postgres-apt-packages
  "Return a sequence of package names for the given version."
  [version]
  {:pre [(string? version)]}
  [(str "postgresql-" version)])


(defmethod layout-settings :debian-base
  [os-family layout version]
  {:post [(validate Layout %)]}
  (deep-merge
   (base-layout)
   {:default-cluster-name "main"
    :bin (format "/usr/lib/postgresql/%s/bin/" version)
    :share (format "/usr/lib/postgresql/%s/share/" version)
    :wal_directory (format "/var/lib/postgresql/%s/%%s/archive" version)
    :postgresql_file (format
                      "/etc/postgresql/%s/%%s/postgresql.conf" version)
    :has-pg-wrapper true
    :has-multicluster-service true
    :options
    {:data_directory (format "/var/lib/postgresql/%s/%%s" version)
     :hba_file (format "/etc/postgresql/%s/%%s/pg_hba.conf" version)
     :ident_file (format "/etc/postgresql/%s/%%s/pg_ident.conf" version)
     :external_pid_file (format "/var/run/postgresql/%s-%%s.pid" version)
     :unix_socket_directory "/var/run/postgresql"}}))

(defmethod layout-settings :rh-base
  [os-family layout version]
  {:post [(validate Layout %)]}
  (let [major (first (split version #"\."))]
    (deep-merge
     (base-layout)
     {:bin "/usr/bin"
      :default-cluster-name "data"
      :share "/usr/share/pgsql"
      :wal_directory (format "/var/lib/pgsql/%s/%%s/archive" version)
      :postgresql_file (format "/var/lib/pgsql/%s/%%s/postgresql.conf" version)
      :options
      {:data_directory (format "/var/lib/pgsql/%s/%%s" version)
       :hba_file (format "/var/lib/pgsql/%s/%%s/pg_hba.conf" version)
       :ident_file (format "/var/lib/pgsql/%s/%%s/pg_ident.conf" version)
       :external_pid_file (format "/var/run/postmaster-%s-%%s.pid" version)
       :unix_socket_directory "/var/run/postgresql"}})))

(defmethod layout-settings :amzn-linux
  [os-family layout version]
  {:post [(validate Layout %)]}
  (let [major (first (split version #"\."))
        data (format "/var/lib/pgsql%s/data/" major)]
    (deep-merge
     (base-layout)
     {:bin "/usr/bin"
      :default-cluster-name "data"
      :share "/usr/share/pgsql"
      :wal_directory (format "%s/%%s/archive" data)
      :postgresql_file (format "%s/%%s/postgresql.conf" data)
      :options
      {:data_directory data
       :hba_file (format "%s/pg_hba.conf" data)
       :ident_file (format "%s/pg_ident.conf" data)
       :external_pid_file (format "/var/run/postmaster-%s.pid" version)
       :unix_socket_directory "/var/run/postgresql"}})))

(defmethod layout-settings :pgdg
  [os-family package-source version]
  {:post [(validate Layout %)]}
  (deep-merge
   (base-layout)
   (layout-settings os-family :rh-base version)
   {:bin (format "/usr/pgsql-%s/bin/" version)
    :share (format "/usr/pgsql-%s/share/" version)
    :default-cluster-name "data"
    :service (str "postgresql-" version "-%s")
    :default-service (str "postgresql-" version)
    :use-port-in-pidfile true
    :wal_directory (format "/var/lib/pgsql/%s/%%s/archive" version)
    :postgresql_file (format "/var/lib/pgsql/%s/%%s/postgresql.conf" version)
    :options
    {:data_directory (format "/var/lib/pgsql/%s/%%s" version)
     :hba_file (format "/var/lib/pgsql/%s/%%s/pg_hba.conf" version)
     :ident_file (format "/var/lib/pgsql/%s/%%s/pg_ident.conf" version)}}))

(defmethod layout-settings :arch
  [os-family package-source version]
  {:post [(validate Layout %)]}
  (deep-merge
   (base-layout)
   {:components []
    :default-cluster-name "data"
    :initdb-via :initdb
    :wal_directory "/var/lib/postgres/%%s/archive/"
    :postgresql_file  "/var/lib/postgres/%%s/postgresql.conf"
    :options
    {:data_directory "/var/lib/postgres/%%s/"
     :hba_file  "/var/lib/postgres/%%s/pg_hba.conf"
     :ident_file "/var/lib/postgres/%%s/pg_ident.conf"}}))
