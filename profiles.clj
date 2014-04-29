{:dev
 {:dependencies [[com.palletops/pallet "0.8.0-SNAPSHOT" :classifier "tests"]
                 [com.palletops/crates "0.1.2-SNAPSHOT"]
                 [com.palletops/pallet-test-env "RELEASE"]
                 [ch.qos.logback/logback-classic "1.0.9"]]
  :plugins [[com.palletops/lein-pallet-crate "RELEASE"]
            [lein-pallet-release "RELEASE"]
            [com.palletops/lein-test-env "RELEASE"]]}
 :provided
 {:dependencies [[org.clojure/clojure "1.6.0"]
                 [com.palletops/pallet "0.8.0-SNAPSHOT"]]}
 :aws {:pallet/test-env
       {:test-specs
        [;; {:selector :ubuntu-13-10}
         ;; {:selector :ubuntu-13-04
         ;;  :expected [{:feature ["oracle-java-8"]
         ;;              :expected? :not-supported}]}
         ;; {:selector :ubuntu-12-04}
         {:selector :amzn-linux-2013-092}
         ;; {:selector :centos-6-5}
         ;; {:selector :debian-7-4}
         ;; {:selector :debian-6-0}
         ]}}
 :vmfest {:pallet/test-env {:test-specs
                            [{:selector :ubuntu-13-04}]}}}
