(ns pallet.crate.postgres.kb-test
  (:require
   [clojure.test :refer :all]
   [pallet.crate.postgres.kb :as kb]))

;; This is brittle
(deftest pgdg-url-test
  (is (= "http://yum.postgresql.org/9.3/fedora/fedora-19-x86_64/pgdg-fedora93-9.3-1.noarch.rpm"
         (kb/pgdg-url "9.3" :fedora "19" "x86_64")))
  (is (= "http://yum.postgresql.org/9.3/fedora/fedora-20-i386/pgdg-fedora93-9.3-1.noarch.rpm"
         (kb/pgdg-url "9.3" :fedora "20" "i386")))
  (is (= "http://yum.postgresql.org/9.3/redhat/rhel-6-i386/pgdg-redhat93-9.3-1.noarch.rpm"
         (kb/pgdg-url "9.3" :rhel "6" "i386")))
  (is (= "http://yum.postgresql.org/9.3/redhat/rhel-6-x86_64/pgdg-centos93-9.3-1.noarch.rpm"
         (kb/pgdg-url "9.3" :centos "6" "x86_64"))))

(deftest layout-settings-test
  (is
   (->
    (kb/layout-settings :ubuntu :debian-base "9.1")
    :options :data_directory)))
