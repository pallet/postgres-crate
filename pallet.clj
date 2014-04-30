;;; Pallet project configuration file

(require
 '[pallet.crate.postgres-test
   :refer [test-server-spec]]
 '[pallet.crates.test-nodes :refer [node-specs]])

(defproject postgres-crate
  :provider node-specs                  ; supported pallet nodes
  :groups [(group-spec "pgtest"
             :extends [with-automated-admin-user
                       test-server-spec]
             :roles #{:live-test :default :postgres})])
