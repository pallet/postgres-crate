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

(deftest default-settings-test
  (is
   (->
    (with-script-language :pallet.stevedore.bash/bash
      (with-script-context [:ubuntu :aptitude]
        (postgres/default-settings
          {:server {:image {:os-family :ubuntu} :node-id :id}}
          :debian :debian-base (postgres/settings-map {}))))
    :options :data_directory)))

(deftest settings-test
  (let [settings (with-script-language :pallet.stevedore.bash/bash
                   (with-script-context [:ubuntu :aptitude]
                     (postgres/settings
                      {:server {:image {:os-family :ubuntu} :node-id :id}}
                      (postgres/settings-map
                       {:layout :debian-base}))))]
    (is
     (->
      settings
      :parameters :host :id :postgresql :default :options :data_directory))))

(deftest postgres-test
  (is                                   ; just check for compile errors for now
   (build-actions {}
     (postgres/settings (postgres/settings-map {:version "8.0"}))
     (postgres/install)
     (postgres/settings (postgres/settings-map {:version "9.0"}))
     (postgres/cluster-settings "db1" {})
     (postgres/install)
     (postgres/hba-conf)
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
        pg-settings (-> settings :parameters :host :id :postgresql :default)]
    (is (-> pg-settings :clusters :db1))
    (is (-> pg-settings :clusters :db2))
    (is
     (re-find #"db1/archive" (-> pg-settings :clusters :db1 :wal_directory)))
    (is
     (re-find #"db2/archive" (-> pg-settings :clusters :db2 :wal_directory)))))


(def test-server-spec
  (server-spec
   :extends [(postgres/server-spec {})]
   :phases {:settings (plan-fn
                        ;; (postgres/cluster-settings
                        ;;  "db1" {:options {:port 5432}})
                        )
            :init (plan-fn
                    (postgres/create-database "db")
                    (postgres/create-role "u1"))
            :test (plan-fn
                    (wait-for-port-listen 5432))}))


(def pgsql-9-unsupported
  [{:os-family :debian :os-version-matches "5.0.7"}
   {:os-family :debian :os-version-matches "5.0"}])

(deftest live-test
  (live-test/test-for
   [image (live-test/exclude-images (live-test/images) pgsql-9-unsupported)]
   (logging/tracef "postgres live test: image %s" (pr-str image))
   (live-test/test-nodes
    [compute node-map node-types]
    {:pgtest
     (->
      (server-spec
       :phases
       {:bootstrap (plan-fn
                    (minimal-packages)
                    (package-manager :update)
                    (automated-admin-user/automated-admin-user))
        :settings (plan-fn
                   (postgres/settings (postgres/settings-map {}))
                   (postgres/cluster-settings "db1" {:options {:port 5433}}))
        :configure (plan-fn (postgres/install))
        :verify (plan-fn
                 (postgres/log-settings)
                 (postgres/initdb)
                 (postgres/initdb :cluster "db1")
                 (postgres/hba-conf)
                 (postgres/hba-conf :cluster "db1")
                 (postgres/postgresql-conf)
                 (postgres/postgresql-conf :cluster "db1")
                 (postgres/service-config)
                 (postgres/service :action :restart :if-config-changed false)
                 (postgres/create-database "db")
                 (postgres/postgresql-script
                  :content "create temporary table table1 ();"
                  :show-stdout true)
                 (postgres/create-role "user1")
                 (postgres/create-database "db" :cluster "db1")
                 (postgres/create-role "user1" :cluster "db1")
                 (postgres/postgresql-script
                  :content "create temporary table table2 ();"
                  :show-stdout true :cluster "db1")
                 (wait-for-port-listen 5432)
                 (wait-for-port-listen 5433))}
       :count 1
       :node-spec (node-spec :image image)))}
    (is
     (lift
      (val (first node-types)) :phase [:settings :verify] :compute compute)))))
