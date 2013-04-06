{:dev
 {:dependencies [[com.palletops/pallet "0.8.0-beta.7" :classifier "tests"]
                 [com.palletops/crates "0.1.0"]
                 [ch.qos.logback/logback-classic "1.0.9"]]
  :plugins [[lein-set-version "0.3.0"]
            [lein-resource "0.3.2"]
            [com.palletops/pallet-lein "0.6.0-beta.8"]]
  :aliases {"live-test-up"
            ["pallet" "up"
             "--phases" "install,configure,test"
             "--selector" "live-test"]
            "live-test-down" ["pallet" "down" "--selector" "live-test"]
            "live-test" ["do" "live-test-up," "live-test-down"]}
  :test-selectors {:default (complement :live-test)
                   :live-test :live-test
                   :all (constantly true)}}
  :doc {:dependencies [[com.palletops/pallet-codox "0.1.0"]]
        :plugins [[codox/codox.leiningen "0.6.4"]
                  [lein-marginalia "0.7.1"]]
        :codox {:writer codox-md.writer/write-docs
                :output-dir "doc/0.8/api"
                :src-dir-uri "https://github.com/pallet/postgres-crate/blob/develop"
                :src-linenum-anchor-prefix "L"}
        :aliases {"marg" ["marg" "-d" "doc/0.8/annotated"]
                  "codox" ["doc"]
                  "doc" ["do" "codox," "marg"]}}
  :release
  {:set-version
   {:updates [{:path "README.md" :no-snapshot true}]}}}
