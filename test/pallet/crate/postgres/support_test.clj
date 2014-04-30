(ns pallet.crate.postgres.support-test
  (:require
   [clojure.test :refer :all]
   [pallet.actions
    :refer [exec-checked-script package package-manager minimal-packages]]
   [pallet.api :refer [lift plan-fn group-spec]]
   [pallet.core.api :refer [phase-errors]]
   [pallet.crate.automated-admin-user :as automated-admin-user]
   [pallet.crate.network-service :refer [wait-for-port-listen]]
   [pallet.crate.postgres :as postgres]
   [pallet.crates.test-nodes :as test-nodes]
   [pallet.repl :refer [explain-session]]
   [pallet.test-env
    :refer [*compute-service* *node-spec-meta*
            with-group-spec test-env unique-name]]
   [pallet.test-env.project :as project]))

(test-env test-nodes/node-specs project/project)

(deftest ^:support port-listen-test
  (let [spec (group-spec (unique-name)
               :node-spec (:node-spec *node-spec-meta*)
               :extends [automated-admin-user/with-automated-admin-user
                         (postgres/server-spec
                          (postgres/settings-map
                           {:options {:listen_addresses "*"}
                            :permissions
                            [{:connection-type "host"
                              :database "all"
                              :user "all"
                              :address "10.0.2.2/24"
                              :auth-method "md5"}
                             {:connection-type "host"
                              :database "all"
                              :user "all"
                              :address "192.168.56.1/24"
                              :auth-method "md5"}]}))]
               :phases {:settings (plan-fn
                                    ;; (postgres/cluster-settings
                                    ;;  "db1" {:options {:port 5432}})
                                    )
                        :init (plan-fn
                                (postgres/create-database "db")
                                (postgres/create-role
                                 "u3"
                                 :user-parameters [:login :encrypted
                                                   :password ""'mypasswd'""]))
                        :test (plan-fn
                                (wait-for-port-listen 5432))})]
    (with-group-spec spec
      (let [session (lift spec
                          :phase [:install :configure :init :test]
                          :compute *compute-service*)]
        (testing "configure postgres"
          (is session)
          (is (not (phase-errors session)))
          (when (phase-errors session)
            (explain-session session)))))))

(deftest postgres
  (let [spec (group-spec (unique-name)
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
               :node-spec (:node-spec *node-spec-meta*))]
    (let [session (lift spec
                        :phase [:settings :install :configure :init :verify]
                        :compute *compute-service*)]
      (testing "configure postgres"
        (is session)
        (is (not (phase-errors session)))
        (when (phase-errors session)
          (explain-session session))))))
