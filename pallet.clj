;;; Pallet project configuration file

(require
 '[pallet.crate.postgres-test
   :refer [postgres-test-spec]]
 '[pallet.crates.test-nodes :refer [node-specs]])

(defproject postgres-crate
  :provider node-specs                  ; supported pallet nodes
  :groups [(group-spec "postgres-test"
             :extends [with-automated-admin-user
                       postgres-test-spec]
             :roles #{:live-test :default :postgres})])
