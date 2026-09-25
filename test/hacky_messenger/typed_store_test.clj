(ns hacky-messenger.typed-store-test
  (:require [clojure.test :refer [deftest is]] [hacky-messenger.typed-store :as typed] [babashka.fs :as fs]))
(deftest typed-routes-keep-needs-binding-and-ref-links
  (let [route (typed/route! {:flow/id "00f95a" :route/session "s" :route/pane "p" :route/terminal "t" :route/agent "codex"})]
    (is (= "NeedsBinding" (:route/state route)))
    (is (= [:flow/id "00f95a"] (get-in (typed/route-tx route) [1 :route/flow])))
    (is (thrown? Exception (typed/route! (assoc route :unknown true))))))
(deftest attempts-preserve-absent-historical-body-and-retirements-link-flow
  (is (= [:flow/id "00f95a"] (get-in (typed/attempt-tx {:attempt/id "a" :attempt/flow "00f95a" :attempt/at "now" :attempt/grade :Held :attempt/reason :RouteHold}) [0 :attempt/flow])))
  (is (= [:flow/id "00f95a"] (get-in (typed/retirement-tx {:retirement/flow "00f95a" :retirement/evidence "e" :retirement/at "now" :retirement/retired-by "Mind"}) [0 :retirement/flow]))))
(deftest isolated-pod-accepts-idempotent-route-transactions
  (let [root (str (fs/create-temp-dir {:prefix "hm-typed-"}))
        route {:flow/id "00f95a" :route/session "s" :route/pane "p" :route/terminal "t" :route/agent "codex" :route/thread "00000000-0000-0000-0000-000000000000" :route/hold "none" :route/state "Bound"}]
    (typed/put-route! root route)
    (typed/put-route! root route)
    (is (= route (typed/route-for root "00f95a")))))
