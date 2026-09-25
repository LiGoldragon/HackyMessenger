(ns hacky-messenger.core-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [malli.core]
            [hacky-messenger.core :as hm]
            [hacky-messenger.store :as store]
            [babashka.fs :as fs]))

(def route {:session "s" :name "Mind Sol 00f95a" :pane_id "p" :terminal_id "t"
            :agent "codex" :native_thread "00000000-0000-0000-0000-000000000000"})

(deftest identifiers-and-title-fallback-are-strict
  (is (false? (malli.core/validate hm/FlowId "../../etc/x")))
  (is (thrown? Exception (hm/path "../../etc/x")))
  (is (thrown? Exception (hm/path "a b!")))
  (with-redefs [hm/live-agents (constantly [(assoc route :name "Mind Sol 00f95a")])]
    (is (= "p" (:pane_id (first (hm/resolve-send-route "00f95a" nil nil))))))
  (with-redefs [hm/live-agents (constantly [(assoc route :name "A 00f95a") (assoc route :name "B 00f95a")])]
    (is (thrown? Exception (hm/resolve-send-route "00f95a" nil nil)))))

(deftest relay-is-a-bounded-edn-round-trip
  (let [line (hm/relay "00f95a" "e51411" "receipt")
        value (edn/read-string line)]
    (is (<= (count line) 800))
    (is (not (.contains line "\n")))
    (is (= "machine" (first (:machine/relay value))))
    (is (= "00f95a" (second (:machine/relay value))))
    (is (= ["e51411"] (nth (:machine/relay value) 4)))))

(deftest datalevin-pod-indexes-and-queries-attempts
  (let [root (str (fs/create-temp-dir {:prefix "hm-datalevin-"}))
        attempt {:id "attempt-1" :at "2026-09-25T00:00:00Z" :flow "00f95a"
                 :reason :NotRegistered :grade :Held}]
    (store/index-attempt! root attempt)
    (is (= #{["attempt/attempt-1" :NotRegistered]}
           (store/attempts-for root "00f95a")))))

(deftest held-unregistered-writes-edn-and-datalog-pending-without-prompt
  (let [root (str (fs/create-temp-dir {:prefix "hm-held-"}))
        prompted (atom false)]
    (binding [hm/*root* root hm/*flow-id* "sender"]
      (with-redefs [hm/live-agents (constantly [])
                    hm/verify-target! (fn [_] route)
                    hm/direct-prompt! (fn [& _] (reset! prompted true))]
        (let [message (try (hm/send! "00f95a" "held body" false nil)
                           (catch Exception error (.getMessage error)))]
          (is (re-find #"Held\.\{ 00f95a NotRegistered" message))
          (is (false? @prompted))
          (is (= 1 (count (fs/glob (fs/path root "pending") "*.edn"))))
          (is (= #{["pending/" :NotRegistered]}
                 (set (map (fn [[identity reason]] [(subs identity 0 8) reason])
                           (store/pending-for root "00f95a"))))))))))

(deftest fallback-order-and-grades-are-explicit
  (with-redefs [hm/live-agents (constantly [route])]
    (is (= [route false] (hm/resolve-send-route "00f95a" route nil))))
  (with-redefs [hm/live-agents (constantly [(assoc route :pane_id "stale")])]
    (is (= "stale" (:pane_id (first (hm/resolve-send-route "00f95a" route nil)))))
    (is (true? (second (hm/resolve-send-route "00f95a" route nil)))))
  (with-redefs [hm/live-agents (constantly [(assoc route :session "override" :pane_id "chosen")])]
    (is (= "chosen" (:pane_id (first (hm/resolve-send-route "00f95a" nil "override:chosen")))))))

(deftest stale-route-is-fallback-presented-and-prompt-failure-is-not-retried
  (let [root-path (str (fs/create-temp-dir {:prefix "hm-send-"}))]
    (binding [hm/*root* root-path hm/*flow-id* "sender"]
      (hm/atomic-edn! (hm/path "00f95a") route)
      (with-redefs [hm/live-agents (constantly [(assoc route :pane_id "moved")])
                    hm/verify-target! (fn [_] route)
                    hm/direct-prompt! (fn [_ _ _] nil)]
        (is (= "Fallback-Presented.{ 00f95a unknown }"
               (hm/send! "00f95a" "fallback" false nil))))
      (let [calls (atom 0)]
        (with-redefs [hm/live-agents (constantly [route])
                      hm/verify-target! (fn [_] route)
                      hm/direct-prompt! (fn [& _] (swap! calls inc) (hm/fail "connection lost"))]
          (is (re-find #"Uncertain\.\{ 00f95a"
                       (try (hm/send! "00f95a" "once" false nil)
                            (catch Exception error (.getMessage error)))))
          (is (= 1 @calls)))))))

(deftest ledger-failure-prevents-the-live-send-prompt
  (let [root-path (str (fs/create-temp-dir {:prefix "hm-ledger-"}))
        prompts (atom 0)
        failing (reify hm/Ledger
                  (record-attempt! [_ _] (hm/fail "ledger unavailable"))
                  (record-pending! [_ _ _] nil))]
    (binding [hm/*root* root-path hm/*flow-id* "sender" hm/*ledger* failing]
      (hm/atomic-edn! (hm/path "00f95a") route)
      (with-redefs [hm/live-agents (constantly [route])
                    hm/verify-target! (fn [_] route)
                    hm/direct-prompt! (fn [& _] (swap! prompts inc))]
        (is (re-find #"ledger unavailable"
                     (try (hm/send! "00f95a" "body" false nil)
                          (catch Exception error (.getMessage error)))))
        (is (zero? @prompts))))))
