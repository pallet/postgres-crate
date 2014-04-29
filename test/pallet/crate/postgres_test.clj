(ns pallet.crate.postgres-test
  (:require
   [pallet.actions
    :refer [exec-checked-script package package-manager minimal-packages]]
   [pallet.build-actions :refer [build-actions]]
   [pallet.api :refer [lift node-spec plan-fn server-spec] :as api]
   [pallet.crate.automated-admin-user :as automated-admin-user]
   [pallet.crate.network-service :refer [wait-for-port-listen]]
   [pallet.crate.postgres :as postgres]
   [pallet.live-test :as live-test]
   [pallet.script :refer [with-script-context]]
   [pallet.stevedore :refer [with-script-language]]
   [pallet.test-utils :as test-utils]
   [clojure.tools.logging :as logging])
  (:use clojure.test))

(deftest merge-settings-test
  (is (= {:options {:b 2 :a 1}
          :permissions [1 2]
          :recovery {:bb 2 :aa 1}
          :start {:start :disable}}
         (postgres/merge-settings
          {:options {:a 1}
           :permissions [1]
           :recovery {:aa 1}
           :start {:start :auto}}
          {:options {:b 2}
           :permissions [2]
           :recovery {:bb 2}
           :start {:start :disable}}))))


(deftest settings-test
  (build-actions {}
    (let [settings (postgres/settings
                    (postgres/settings-map
                     {:layout :debian-base}))]
      (is
       (get-in
        settings
        [:plan-state :host :id postgres/facility nil :options :data_directory])))))

(deftest postgres-test
  (is                                   ; just check for compile errors for now
   (build-actions {}
     (postgres/settings (postgres/settings-map {:version "8.0"}))
     (postgres/install {})
     (postgres/settings (postgres/settings-map {:version "9.0"}))
     (postgres/cluster-settings "db1" {})
     (postgres/install {})
     (postgres/hba-conf {})
     (postgres/postgresql-script :content "some script")
     (postgres/create-database "db")
     (postgres/create-role "user"))))

(deftest cluster-settings-test
  (let [settings
        (second (build-actions {}
                  (postgres/settings
                   (postgres/settings-map
                    {:version "9.0"
                     :wal_directory "/var/lib/postgres/%s/archive/"}))
                  (postgres/cluster-settings "db1" {})
                  (postgres/cluster-settings "db2" {})
                  (postgres/settings
                   (postgres/settings-map {:version "9.0"}))))
        pg-settings (get-in settings
                            [:plan-state :host :id postgres/facility nil])]
    (is (-> pg-settings :clusters :db1))
    (is (-> pg-settings :clusters :db2))
    (is
     (re-find #"db1/archive" (-> pg-settings :clusters :db1 :wal_directory)))
    (is
     (re-find #"db2/archive" (-> pg-settings :clusters :db2 :wal_directory)))))
