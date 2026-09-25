(ns hacky-messenger.cli-test
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [cheshire.core :as json]
            [hacky-messenger.core :as hm]
            [hacky-messenger.legacy-import-test :as legacy-test]
            [hacky-messenger.typed-store :as store]))

(def native-thread "00000000-0000-0000-0000-000000000000")

(defn invoke [environment wrapper & arguments]
  (apply shell {:out :string :err :string :continue true :extra-env environment}
         (str (fs/absolutize (fs/path "bin" wrapper))) arguments))

(defn fake-herdr-environment []
  (let [root (str (fs/create-temp-dir {:prefix "hm-cli-presented-"}))
        tools (fs/create-temp-dir {:prefix "hm-cli-presented-tools-"})
        prompt-log (str (fs/path root "prompts.edn"))
        herdr (fs/path tools "herdr")
        orchestrate (fs/path tools "orchestrate")]
    (fs/copy "test/fake-herdr" herdr)
    (spit (str orchestrate)
          "#!/usr/bin/env bash\ncase \"$1\" in Lock.*) echo 'Locked.{ 1 Test sender [ /tmp ] test }';; Release.*) echo 'Released.{ 1 Test sender [ /tmp ] test }';; esac\n")
    (.setExecutable (java.io.File. (str herdr)) true)
    (.setExecutable (java.io.File. (str orchestrate)) true)
    {:root root :tools tools :prompt-log prompt-log
     :environment {"PATH" (str tools ":" (System/getenv "PATH"))
                   "HM_REGISTRY" root "HM_PRIMARY_ROOT" root "FLOW_ID" "sender"
                   "FAKE_HERDR_PROMPT_LOG" prompt-log}}))

(deftest wait-presented-cli-uses-one-fake-herdr-prompt-and-durable-grades
  (doseq [[wait-result expected-grade expected-exit]
          [["presented" :Presented 0]
           ["submitted" :Uncertain 1]
           ["timeout" :Uncertain 1]]]
    (let [{:keys [root tools prompt-log environment]} (fake-herdr-environment)
          env (assoc environment "FAKE_HERDR_WAIT" wait-result)]
      (try
        (is (zero? (:exit (invoke env "hm-clj-register" "00f95a" "Mind Sol 00f95a"
                                  "--session" "s" "--native-thread" native-thread))))
        (let [sent (invoke env "hm-clj-send" "00f95a" (str "wait-" wait-result) "--wait-presented")
              attempts (store/attempts-for root "00f95a")]
          (is (= expected-exit (:exit sent)) (str wait-result ": " (:err sent)))
          (is (str/includes? (str (:out sent) (:err sent)) (name expected-grade)))
          (is (= 1 (count (str/split-lines (slurp prompt-log)))))
          (is (= (str "#msg [\"sender\" \"wait-" wait-result "\"]")
                 (first (str/split-lines (slurp prompt-log)))))
          (is (= 1 (count (filter #(= :Submitting (:reason %)) attempts))))
          (is (= expected-grade (:grade (last attempts)))))
        (finally
          (fs/delete-tree root)
          (fs/delete-tree tools))))))

(deftest public-json-import-wrapper-is-dry-run-by-default-and-requires-apply
  (let [{:keys [source]} (legacy-test/fixture!)
        target (str (fs/path (fs/create-temp-dir {:prefix "hm-cli-import-target-"}) "target"))
        receipt (str (fs/path (fs/create-temp-dir {:prefix "hm-cli-import-receipt-"}) "receipt.edn"))
        dry-run (invoke {} "hm-clj-import-json" source "--target" target "--receipt" receipt)]
    (is (zero? (:exit dry-run)) (:err dry-run))
    (is (= :dry-run (:mode (edn/read-string (str/trim (:out dry-run))))))
    (is (not (fs/exists? (store/database-path target))))
    (let [applied (invoke {} "hm-clj-import-json" source "--target" target "--receipt" receipt "--apply")]
      (is (zero? (:exit applied)) (:err applied))
      (is (= :applied (:mode (edn/read-string (str/trim (:out applied))))))
      (is (= "NeedsBinding" (:state (store/route-for target "beta")))))))

(deftest public-wrappers-use-one-isolated-typed-database
  (let [root (str (fs/create-temp-dir {:prefix "hm-cli-store-"}))
        tools (fs/create-temp-dir {:prefix "hm-cli-tools-"})
        state (str (fs/path root "fake-herdr-pane"))
        prompt-log (str (fs/path root "fake-herdr-prompts.edn"))
        evidence (fs/create-temp-file {:prefix "hm-cli-evidence-"})
        herdr (fs/path tools "herdr")
        orchestrate (fs/path tools "orchestrate")
        environment {"PATH" (str tools ":" (System/getenv "PATH"))
                     "HM_REGISTRY" root
                     "HM_PRIMARY_ROOT" root
                     "FLOW_ID" "sender"
                     "FAKE_HERDR_STATE" state
                     "FAKE_HERDR_PROMPT_LOG" prompt-log}]
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
      (let [snapshot (invoke environment "hm-clj-heartbeat-state")
            value (json/parse-string (:out snapshot) true)]
        (is (zero? (:exit snapshot)) (:err snapshot))
        (is (= 1 (:version value)))
        (is (= [{:flow "00f95a"
                 :route {:session "s" :name "Mind Sol 00f95a" :pane_id "p" :terminal_id "t"
                         :agent "codex" :native_thread native-thread :state "Bound"}}]
               (:routes value)))
        (is (= [] (:retirements value))))
      (let [sent (invoke environment "hm-clj-send" "00f95a" "isolated-success")]
        (is (zero? (:exit sent)) (:err sent))
        (is (str/includes? (:out sent) "Transported.{ 00f95a working }"))
        (is (some #(= :Submitting (:reason %)) (store/attempts-for root "00f95a"))))
      (let [short-body "one\ntwo\nλ"
            long-body (apply str (repeat 900 "λ"))
            short-send (invoke environment "hm-clj-send" "00f95a" short-body)
            long-send (invoke environment "hm-clj-send" "00f95a" long-body)
            lines (str/split-lines (slurp prompt-log))
            values (mapv read-string lines)
            pointer (second (last values))
            path (second (re-find #"read (.+) in full\." pointer))]
        (is (zero? (:exit short-send)) (:err short-send))
        (is (zero? (:exit long-send)) (:err long-send))
        (is (= 3 (count lines)))
        (is (every? #(and (<= (count %) 800) (not (str/includes? % "\n"))) lines))
        (is (every? #(str/starts-with? % "#msg [") lines))
        (is (every? #(and (= 2 (count %)) (string? (first %)) (string? (second %))) values))
        (is (= ["sender" "one two λ"] (second values)))
        (is (= (str long-body "\n") (slurp path)))
        (is (str/starts-with? path (str (fs/path root "flows" "sender" "messages"))))
        (is (= #{short-body long-body}
               (set (keep :body (filter #(and (contains? #{:Submitting :sent} (:reason %))
                                              (not= "isolated-success" (:body %)))
                                        (store/attempts-for root "00f95a")))))))
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
        (let [snapshot (invoke environment "hm-clj-heartbeat-state")
              value (json/parse-string (:out snapshot) true)]
          (is (zero? (:exit snapshot)) (:err snapshot))
          (is (= [] (:routes value)))
          (is (= ["00f95a"] (mapv :flow (:retirements value)))))
        (is (empty? (fs/glob root "**/*.edn"))
            "operational commands create no EDN authority files"))
      (finally
        (fs/delete-tree root)
        (fs/delete-tree tools)
        (fs/delete-if-exists evidence)))))
