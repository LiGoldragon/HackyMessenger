(ns hacky-messenger.cli-test
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [hacky-messenger.core :as hm]
            [hacky-messenger.typed-store :as store]))

(def native-thread "00000000-0000-0000-0000-000000000000")

(defn invoke [environment wrapper & arguments]
  (apply shell {:out :string :err :string :continue true :extra-env environment}
         (str (fs/absolutize (fs/path "bin" wrapper))) arguments))

(deftest public-wrappers-use-one-isolated-typed-database
  (let [root (str (fs/create-temp-dir {:prefix "hm-cli-store-"}))
        tools (fs/create-temp-dir {:prefix "hm-cli-tools-"})
        state (str (fs/path root "fake-herdr-pane"))
        evidence (fs/create-temp-file {:prefix "hm-cli-evidence-"})
        herdr (fs/path tools "herdr")
        orchestrate (fs/path tools "orchestrate")
        environment {"PATH" (str tools ":" (System/getenv "PATH"))
                     "HM_REGISTRY" root
                     "FLOW_ID" "sender"
                     "FAKE_HERDR_STATE" state}]
    (try
      (fs/copy "test/fake-herdr" herdr)
      (spit (str orchestrate)
            "#!/usr/bin/env bash\ncase \"$1\" in Lock.*) echo 'Locked.{ 1 Test sender [ /tmp ] test }';; Release.*) echo 'Released.{ 1 Test sender [ /tmp ] test }';; esac\n")
      (.setExecutable (java.io.File. (str herdr)) true)
      (.setExecutable (java.io.File. (str orchestrate)) true)
      (spit (str evidence) "retirement witness")

      (let [registered (invoke environment "hm-clj-register" "00f95a" "Mind Sol 00f95a"
                               "--session" "s" "--native-thread" native-thread)]
        (is (zero? (:exit registered)) (:err registered))
        (is (str/includes? (:out registered) "Registered 00f95a")))
      (let [listed (invoke environment "hm-clj-list")]
        (is (zero? (:exit listed)) (:err listed))
        (is (str/includes? (:out listed) "00f95a\tMind Sol 00f95a\ts\tworking")))
      (let [sent (invoke environment "hm-clj-send" "00f95a" "isolated-success")]
        (is (zero? (:exit sent)) (:err sent))
        (is (str/includes? (:out sent) "Transported.{ 00f95a working }"))
        (is (some #(= :Submitting (:reason %)) (store/attempts-for root "00f95a"))))
      (let [moved (invoke environment "hm-clj-move" "00f95a" "w2"
                          "--session" "s" "--pane-id" "p" "--terminal-id" "t"
                          "--name" "Mind Sol 00f95a" "--agent" "codex"
                          "--native-thread" native-thread "--process-pid" "123")]
        (is (zero? (:exit moved)) (:err moved))
        (is (= "m" (:pane_id (store/route-for root "00f95a")))))
      (let [deregistered (invoke environment "hm-clj-deregister" "00f95a"
                                  "--session" "s" "--pane-id" "m" "--terminal-id" "t"
                                  "--name" "Mind Sol 00f95a")]
        (is (zero? (:exit deregistered)) (:err deregistered)))
      (let [held (invoke (assoc environment "FAKE_HERDR_AGENT_LIST" "empty")
                         "hm-clj-send" "00f95a" "isolated-held" "--hold-seconds" "0")]
        (is (= 1 (:exit held)))
        (is (str/includes? (:err held) "Held.{ 00f95a NotRegistered"))
        (is (= "isolated-held" (:message (first (store/pending-for root "00f95a"))))))

      (let [registered (invoke environment "hm-clj-register" "00f95a" "Mind Sol 00f95a"
                               "--session" "s" "--native-thread" native-thread)]
        (is (zero? (:exit registered)) (:err registered)))
      (let [digest (hm/sha256 evidence)
            retired (invoke environment "hm-clj-retire" "00f95a"
                            "--session" "s" "--pane-id" "m" "--terminal-id" "t"
                            "--name" "Mind Sol 00f95a" "--agent" "codex"
                            "--native-thread" native-thread
                            "--evidence" (str evidence) "--evidence-sha256" digest)]
        (is (zero? (:exit retired)) (:err retired))
        (is (= "sender" (:retired_by (store/retirement-for root "00f95a"))))
        (is (nil? (store/route-for root "00f95a")))
        (is (empty? (fs/glob root "**/*.edn"))
            "operational commands create no EDN authority files"))
      (finally
        (fs/delete-tree root)
        (fs/delete-tree tools)
        (fs/delete-if-exists evidence)))))
