(ns hacky-messenger.typed-store-test
  (:require [babashka.fs :as fs]
            [clojure.test :refer [deftest is testing]]
            [hacky-messenger.typed-store :as typed]))

(def flow "00f95a")
(def attempt
  {:attempt/id "attempt-1"
   :attempt/flow flow
   :attempt/at "2026-09-25T00:00:00Z"
   :attempt/grade :Held
   :attempt/reason :NotRegistered})
(def retirement
  {:retirement/flow flow
   :retirement/evidence "sha256:evidence"
   :retirement/at "2026-09-25T01:00:00Z"
   :retirement/retired-by "Mind"})

(defn with-temp-store [f]
  (let [root (str (fs/create-temp-dir {:prefix "hm-typed-"}))]
    (try (f root) (finally (fs/delete-tree root)))))

(deftest typed-transactions-use-datalevin-references
  (let [route (typed/route! {:flow/id flow
                             :route/session "s"
                             :route/pane "p"
                             :route/terminal "t"
                             :route/agent "codex"})]
    (is (= "NeedsBinding" (:route/state route)))
    (is (= [:flow/id flow] (get-in (typed/route-tx route) [1 :route/flow])))
    (is (= [:flow/id flow] (get-in (typed/attempt-tx attempt) [1 :attempt/flow])))
    (is (= [:flow/id flow]
           (get-in (typed/retirement-tx retirement) [1 :retirement/flow])))
    (is (thrown? Exception (typed/route! (assoc route :unknown true))))))

(deftest real-pod-roundtrips-linked-values-and-optional-fields
  (with-temp-store
    (fn [root]
      (let [route {:flow/id flow
                   :route/session "s"
                   :route/pane "p"
                   :route/terminal "t"
                   :route/agent "codex"}]
        (typed/put-route! root route)
        (typed/put-attempt! root attempt)
        (typed/put-pending! root (:attempt/id attempt) "held")

        (is (= (typed/route! route) (typed/route-for root flow)))
        (is (= attempt (typed/attempt-by-id root (:attempt/id attempt))))
        (is (= [attempt] (typed/attempts-for root flow)))
        (is (= {:pending/id (:attempt/id attempt)
                :pending/attempt attempt
                :pending/state "held"}
               (typed/pending-by-id root (:attempt/id attempt))))
        (is (= [(typed/pending-by-id root (:attempt/id attempt))]
               (typed/pending-for root flow)))))))

(deftest historical-body-and-retirement-precedence-survive-real-pod-reads
  (with-temp-store
    (fn [root]
      (let [with-body (assoc attempt :attempt/id "attempt-2" :attempt/body "payload")
            route {:flow/id flow :route/session "s" :route/pane "p"
                   :route/terminal "t" :route/agent "codex"}]
        (typed/put-route! root route)
        (typed/put-attempt! root attempt)
        (typed/put-attempt! root with-body)
        (is (not (contains? (typed/attempt-by-id root "attempt-1") :attempt/body)))
        (is (= "payload" (:attempt/body (typed/attempt-by-id root "attempt-2"))))

        (typed/put-retirement! root retirement)
        (is (= retirement (typed/retirement-for root flow)))
        (is (= [retirement] (typed/retirements-for root flow)))
        (is (typed/retirement-precedes? (typed/retirements-for root flow) flow))
        (is (nil? (typed/route-for root flow)))))))

(deftest edn-is-only-an-import-export-boundary
  (let [entities [(typed/route! {:flow/id flow :route/session "s" :route/pane "p"
                                 :route/terminal "t" :route/agent "codex"})
                  attempt
                  {:pending/id "attempt-1" :pending/attempt attempt :pending/state "held"}
                  retirement]
        encoded (typed/export-edn entities)]
    (is (= entities (typed/import-edn encoded)))
    (is (string? encoded))
    (is (thrown? Exception (typed/import-edn "{:not-a-sequence true}")))
    (is (thrown? Exception (typed/import-edn "[{:unknown/entity true}]")))
    (is (thrown? Exception (typed/export-edn {:not "a sequence"})))))

(deftest malformed-query-results-and-references-are-rejected
  (testing "row containers and widths"
    (is (thrown? Exception (typed/checked-rows! {:not "rows"} 1)))
    (is (thrown? Exception (typed/checked-rows! #{["x"]} 2))))
  (testing "reader validates pulled maps and resolved refs"
    (with-redefs [typed/query (fn [& _] #{[{:attempt/id "bad"}]})]
      (is (thrown? Exception (typed/attempt-by-id "unused" "bad"))))
    (with-redefs [typed/query (fn [& _] #{[{:attempt/id "bad"
                                            :attempt/flow 42
                                            :attempt/at "now"
                                            :attempt/grade :Held
                                            :attempt/reason :Legacy}]})]
      (is (thrown? Exception (typed/attempt-by-id "unused" "bad")))))
  (testing "real pod rows missing required typed values"
    (with-temp-store
      (fn [root]
        (typed/transact! root [{:flow/id flow}
                               {:attempt/id "malformed"
                                :attempt/flow [:flow/id flow]
                                :attempt/at "now"}])
        (is (thrown? Exception (typed/attempts-for root flow)))))))

(deftest pod-version-is-explicitly-pinned
  (is (= "0.8.25" typed/pod-version)))
