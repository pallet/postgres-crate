(ns pallet.crate.postgres.config-test
  (:require
   [clojure.test :refer :all]
   [pallet.crate.postgres.config :refer [conf hba]]))

(deftest hba-test
  (is (= "" (hba []))))

(deftest conf-test
  (is (= "" (conf []))))
