(ns hacky-messenger.core-test
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [cheshire.core :as json]
            [malli.core]
            [hacky-messenger.core :as hm]
            [hacky-messenger.typed-store :as store]
            [babashka.fs :as fs]))

(def route {:session "s" :name "Mind Sol 00f95a" :pane_id "p" :terminal_id "t"
            :agent "codex" :native_thread "00000000-0000-0000-0000-000000000000"})

(defn pass-reservation [_ f] (f))
(defn persist-route! [root-path value]
  (store/put-route! root-path "00f95a" value))
(defn fake-transport [live-agent target-agent processes prompts]
  (reify hm/HerdrTransport
    (live-agents* [_] [live-agent])
    (target-agent* [_ _] {:agent target-agent})
    (process-info* [_ _] {:process_info {:foreground_processes processes}})
    (pane* [_ route] {:pane (select-keys route [:pane_id :terminal_id :agent])})
    (move-pane* [_ route _ _] {:move_result {:previous_pane_id (:pane_id route) :previous_workspace_id "w1"
                                             :pane (assoc (select-keys route [:terminal_id :agent]) :pane_id "moved" :workspace_id "w2")}})
    (send-keys* [_ _ _] {:ok true})
    (prompt!* [_ _ _ _] (swap! prompts inc) {:ok true})))

(defn move-transport [route mode moves]
  (reify hm/HerdrTransport
    (live-agents* [_]
      (let [pane @moves]
        (if (or (and (= mode :ambiguous) (not= pane "p"))
                (and (= mode :compensate) (= pane "m")))
          []
          [(assoc route :pane_id pane)])))
    (target-agent* [_ route] {:agent route})
    ;; Codex remote panes do not include the stored UUID in argv; PID plus the
    ;; verified terminal and harness identity is Python's accepted evidence.
    (process-info* [_ _] {:process_info {:foreground_processes [{:pid 123 :argv ["codex" "remote"]}]}})
    (pane* [_ requested] {:pane {:pane_id (:pane_id requested) :terminal_id (:terminal_id route) :agent (:agent route)
                                 :workspace_id (if (= "p" (:pane_id requested)) "w1" (if (= "m" (:pane_id requested)) "w2" "w1"))}})
    (move-pane* [_ requested workspace _]
      (let [next-pane (if (= "p" (:pane_id requested)) "m" "r")
            previous-workspace (if (= "p" (:pane_id requested)) "w1" "w2")]
        (reset! moves next-pane)
        {:move_result {:previous_pane_id (:pane_id requested) :previous_workspace_id previous-workspace
                       :pane {:pane_id next-pane :terminal_id (:terminal_id route) :agent (:agent route) :workspace_id workspace}}}))
    (send-keys* [_ _ _] {:ok true})
    (prompt!* [_ _ _ _] {:ok true})))

(deftest identifiers-and-title-fallback-are-strict
  (is (false? (malli.core/validate hm/FlowId "../../etc/x")))
  (is (thrown? Exception (hm/flow-id! "../../etc/x")))
  (is (thrown? Exception (hm/flow-id! "a b!")))
  (is (= {:session "test" :pane_id "w1:p2"} (hm/parse-pane "test:w1:p2")))
  (with-redefs [hm/live-agents (constantly [(assoc route :name "Mind Sol 00f95a")])]
    (is (= "p" (:pane_id (first (hm/resolve-send-route "00f95a" nil nil))))))
  (with-redefs [hm/live-agents (constantly [(assoc route :name "A 00f95a") (assoc route :name "B 00f95a")])]
    (is (thrown? Exception (hm/resolve-send-route "00f95a" nil nil)))))

(deftest relay-is-a-bounded-edn-round-trip
  (let [line (hm/relay "00f95a" "e51411" "receipt")
        value (edn/read-string line)]
    (is (<= (count line) 800))
    (is (not (.contains line "\n")))
    (is (.startsWith line "{:machine/relay"))
    (is (not (.startsWith line "#:machine")))
    (is (= "machine" (first (:machine/relay value))))
    (is (= "00f95a" (second (:machine/relay value))))
    (is (= ["e51411"] (nth (:machine/relay value) 4)))))

(deftest framed-text-is-one-line-edn-and-durably-points-to-overflow
  (let [primary (str (fs/create-temp-dir {:prefix "hm-primary-"}))
        fixed-clock (reify hm/Clock (current-time [_] "2026-09-25T00:00:00Z"))]
    (binding [hm/*clock* fixed-clock]
      (with-redefs [hm/primary-root (constantly primary)]
        (let [short-line (hm/framed-text "sender" "00f95a" "one\ntwo\nλ")
              short-value (edn/read-string short-line)]
          (is (<= (count short-line) 800))
          (is (not (str/includes? short-line "\n")))
          (is (= "one two λ" (nth (:machine/relay short-value) 5))))
        (let [overhead (dec (count (hm/relay-line "sender" "00f95a" "x")))
              exact-body (apply str (repeat (- 800 overhead) "x"))
              exact-line (hm/framed-text "sender" "00f95a" exact-body)]
          (is (= 800 (count exact-line)))
          (is (= exact-body (nth (:machine/relay (edn/read-string exact-line)) 5))))
        (doseq [body [(apply str (repeat 900 "λ"))
                      "one\ntwo\nthree\nfour"
                      "literal <pasted_content> wrapper"]]
          (let [line (hm/framed-text "sender" "00f95a" body)
                pointer (nth (:machine/relay (edn/read-string line)) 5)
                path (second (re-find #"read (.+) in full\." pointer))]
            (is (<= (count line) 800))
            (is (not (str/includes? line "\n")))
            (is (not (str/includes? line "<pasted_content")))
            (is (not (str/starts-with? line "Machine.Relay.{")))
            (is (str/starts-with? path (str (fs/path primary "flows" "sender" "messages"))))
            (is (= (if (str/ends-with? body "\n") body (str body "\n"))
                   (slurp path)))))))))

(deftest nested-machine-relay-is-rejected-before-send
  (is (re-find #"Nested Machine\.Relay"
               (try (hm/send! "00f95a" (pr-str {:machine/relay ["machine" "sender" "heard" "seat" ["00f95a"] "body" ""]}) false nil)
                    (catch Exception error (.getMessage error))))))

(deftest closed-core-records-and-deep-relays-are-rejected
  (is (false? (malli.core/validate hm/RouteBinding (assoc route :unexpected true))))
  (is (hm/nested-relay? (pr-str [{:machine/relay ["machine" "sender" "heard" "seat" ["00f95a"] "body" ""]}])))
  (is (hm/nested-relay? "Machine.Relay.{ relayed }")))

(deftest abrupt-send-is-durable-gated-and-agent-specific
  (let [root-path (str (fs/create-temp-dir {:prefix "hm-abrupt-"}))
        process [{:argv ["codex" "--thread" (:native_thread route)]}]
        run (fn [record reply target]
              (let [events (atom [])
                    transport (reify hm/HerdrTransport
                                (live-agents* [_] [(assoc record :interactive_ready true :agent_status "working")])
                                (target-agent* [_ _] {:agent (assoc target :interactive_ready true :agent_status "working")})
                                (process-info* [_ _] {:process_info {:foreground_processes process}})
                                (pane* [_ _] {:pane {}})
                                (move-pane* [_ _ _ _] {:move_result {}})
                                (send-keys* [_ _ key] (swap! events conj [:key key]) {:ok true})
                                (prompt!* [_ _ _ wait?] (swap! events conj [:prompt wait?]) reply))]
                (binding [hm/*root* root-path hm/*flow-id* "sender" hm/*with-reservation* pass-reservation hm/*transport* transport]
                  (persist-route! root-path record)
                  [(try (hm/send-abrupt! "00f95a" "receipt" true)
                        (catch Exception error (.getMessage error))) @events])))]
    (let [[result events] (run route {:presented true} route)]
      (is (= "Presented.{ 00f95a working }" result))
      (is (= [[:key "esc"] [:prompt true]] events)))
    (let [[result events] (run (assoc route :agent "claude") {:presented true} (assoc route :agent "claude"))]
      (is (= "Presented.{ 00f95a working }" result))
      (is (= [[:key "esc"] [:key "esc"] [:prompt true] [:key "enter"]] events)))
    (let [[result events] (run route {:ok true} route)]
      (is (re-find #"Uncertain\.\{ 00f95a attempt-.*Escape was sent" result))
      (is (= [[:key "esc"] [:prompt true]] events)))
    (let [[result events] (run route {:presented true} (assoc route :terminal_id "other"))]
      (is (re-find #"Held\.\{ 00f95a" result))
      (is (empty? events)))))

(deftest datalevin-pod-stores-and-queries-typed-attempts
  (let [root (str (fs/create-temp-dir {:prefix "hm-datalevin-"}))
        attempt {:id "attempt-1" :at "2026-09-25T00:00:00Z" :flow "00f95a"
                 :reason :NotRegistered :grade :Held}]
    (store/put-attempt! root attempt)
    (is (= [attempt] (store/attempts-for root "00f95a")))))

(deftest held-unregistered-writes-linked-pending-without-prompt
  (let [root (str (fs/create-temp-dir {:prefix "hm-held-"}))
        prompted (atom false)]
    (binding [hm/*root* root hm/*flow-id* "sender" hm/*with-reservation* pass-reservation]
      (with-redefs [hm/live-agents (constantly [])
                    hm/verify-target! (fn [_] route)
                    hm/direct-prompt! (fn [& _] (reset! prompted true))]
        (let [message (try (hm/send! "00f95a" "held body" false nil)
                           (catch Exception error (.getMessage error)))]
          (is (re-find #"Held\.\{ 00f95a NotRegistered" message))
          (is (false? @prompted))
          (is (= 1 (count (store/pending-for root "00f95a"))))
          (is (= :NotRegistered
                 (get-in (first (store/pending-for root "00f95a"))
                         [:attempt :reason]))))))))

(deftest needs-binding-route-is-held-before-herdr
  (let [root-path (str (fs/create-temp-dir {:prefix "hm-needs-binding-"}))
        prompted (atom false)]
    (binding [hm/*root* root-path hm/*flow-id* "sender"
              hm/*with-reservation* pass-reservation]
      (persist-route! root-path (dissoc route :native_thread))
      (with-redefs [hm/live-agents (constantly [route])
                    hm/direct-prompt! (fn [& _] (reset! prompted true))]
        (let [message (try (hm/send! "00f95a" "held body" false nil 0)
                           (catch Exception error (.getMessage error)))]
          (is (re-find #"Held\.\{ 00f95a NeedsBinding" message))
          (is (false? @prompted))
          (is (= :NeedsBinding
                 (get-in (first (store/pending-for root-path "00f95a"))
                         [:attempt :reason]))))))))

(deftest fallback-order-and-grades-are-explicit
  (with-redefs [hm/live-agents (constantly [route])]
    (is (= [route false] (hm/resolve-send-route "00f95a" route nil))))
  (with-redefs [hm/live-agents (constantly [(assoc route :pane_id "stale")])]
    (is (= "stale" (:pane_id (first (hm/resolve-send-route "00f95a" route nil)))))
    (is (true? (second (hm/resolve-send-route "00f95a" route nil)))))
  (with-redefs [hm/live-agents (constantly [(assoc route :session "override" :pane_id "chosen")])]
    (is (= "chosen" (:pane_id (first (hm/resolve-send-route "00f95a" nil "override:chosen")))))))

(deftest fallback-presentation-requires-an-observed-wait-without-retry
  (let [root-path (str (fs/create-temp-dir {:prefix "hm-send-"}))]
    (binding [hm/*root* root-path hm/*flow-id* "sender" hm/*with-reservation* pass-reservation]
      (persist-route! root-path route)
      (let [calls (atom [])]
        (with-redefs [hm/live-agents (constantly [(assoc route :pane_id "moved")])
                      hm/verify-target! (fn [_] route)
                      hm/direct-prompt! (fn [& args] (swap! calls conj args) {:presented true})]
          (is (= "Fallback-Presented.{ 00f95a unknown }"
                 (hm/send! "00f95a" "fallback" false nil)))
          (is (= 1 (count @calls)))
          (is (true? (nth (first @calls) 2)))))
      (let [calls (atom 0)]
        (with-redefs [hm/live-agents (constantly [(assoc route :pane_id "moved")])
                      hm/verify-target! (fn [_] route)
                      hm/direct-prompt! (fn [& _] (swap! calls inc) {:ok true})]
          (is (re-find #"Uncertain\.\{ 00f95a"
                       (try (hm/send! "00f95a" "submission-only" false nil)
                            (catch Exception error (.getMessage error)))))
          (is (= 1 @calls))))
      (let [calls (atom 0)]
        (with-redefs [hm/live-agents (constantly [(assoc route :pane_id "moved")])
                      hm/verify-target! (fn [_] route)
                      hm/direct-prompt! (fn [& _] (swap! calls inc) (hm/fail "wait timeout"))]
          (is (re-find #"Uncertain\.\{ 00f95a"
                       (try (hm/send! "00f95a" "timeout" false nil)
                            (catch Exception error (.getMessage error)))))
          (is (= 1 @calls)))))))

(deftest ledger-failure-prevents-the-live-send-prompt
  (let [root-path (str (fs/create-temp-dir {:prefix "hm-ledger-"}))
        prompts (atom 0)
        failing (reify hm/Ledger
                  (record-attempt! [_ _] (hm/fail "ledger unavailable"))
                  (record-pending! [_ _ _] nil))]
    (binding [hm/*root* root-path hm/*flow-id* "sender" hm/*ledger* failing hm/*with-reservation* pass-reservation]
      (persist-route! root-path route)
      (with-redefs [hm/live-agents (constantly [route])
                    hm/verify-target! (fn [_] route)
                    hm/direct-prompt! (fn [& _] (swap! prompts inc))]
        (is (re-find #"ledger unavailable"
                     (try (hm/send! "00f95a" "body" false nil)
                          (catch Exception error (.getMessage error)))))
        (is (zero? @prompts))))))

(deftest route-gates-precede-the-prompt
  (let [root-path (str (fs/create-temp-dir {:prefix "hm-route-gates-"}))
        prompts (atom 0)
        good-agent (assoc route :interactive_ready true :agent_status "working")
        good-process [{:argv ["codex" "--thread" (:native_thread route)]}]
        send-result (fn [target-agent processes]
                      (binding [hm/*root* root-path hm/*flow-id* "sender"
                                hm/*with-reservation* pass-reservation
                                hm/*transport* (fake-transport good-agent target-agent processes prompts)]
                        (persist-route! root-path route)
                        (try (hm/send! "00f95a" "body" false nil)
                             (catch Exception error (.getMessage error)))))]
    (is (re-find #"IdentityChanged" (send-result (assoc good-agent :agent "other") good-process)))
    (is (re-find #"NotReady" (send-result (assoc good-agent :interactive_ready false) good-process)))
    (is (re-find #"ProcessMismatch" (send-result good-agent [{:argv ["codex" "--thread" "different"]}])))
    (binding [hm/*root* root-path hm/*flow-id* "sender" hm/*with-reservation* pass-reservation]
      (with-redefs [store/retirement-for (fn [_ flow] (when (= flow "00f95a") {:malformed true}))]
        (is (re-find #"Retirement marker" (try (hm/send! "00f95a" "body" false nil)
                                               (catch Exception error (.getMessage error)))))))
    (binding [hm/*root* root-path hm/*flow-id* "sender"
              hm/*with-reservation* (fn [_ _] (hm/fail "Reservation refused"))
              hm/*transport* (fake-transport good-agent good-agent good-process prompts)]
      (is (re-find #"Reservation refused" (try (hm/send! "00f95a" "body" false nil)
                                               (catch Exception error (.getMessage error))))))
    (is (zero? @prompts))
    (is (= "Transported.{ 00f95a working }" (send-result good-agent good-process)))
    (is (= 1 @prompts))))

(deftest held-routes-overflow-write-errors-and-post-prompt-ledger-failure-are-honest
  (let [root-path (str (fs/create-temp-dir {:prefix "hm-p0-"}))
        primary (str (fs/create-temp-dir {:prefix "hm-primary-p0-"}))
        prompts (atom 0)
        overflow-body (apply str (repeat 790 "x"))
        good-agent (assoc route :interactive_ready true :agent_status "working")
        process [{:argv ["codex" "--thread" (:native_thread route)]}]
        transport (fake-transport good-agent good-agent process prompts)]
    (binding [hm/*root* root-path hm/*flow-id* "sender" hm/*with-reservation* pass-reservation hm/*transport* transport]
      (persist-route! root-path (assoc route :route_hold "pane_move_in_progress"))
      (is (re-find #"RouteHold" (try (hm/send! "00f95a" "body" false nil) (catch Exception error (.getMessage error)))))
      (is (re-find #"RouteHold" (try (hm/send-abrupt! "00f95a" "body" false) (catch Exception error (.getMessage error)))))
      (is (zero? @prompts))
      (persist-route! root-path route)
      (with-redefs [hm/durable-write! (fn [& _] (hm/fail "disk unavailable"))]
        (is (re-find #"RelayOverflow" (try (hm/send! "00f95a" overflow-body false nil) (catch Exception error (.getMessage error))))))
      (let [pending (first (filter #(= overflow-body (:message %))
                                   (store/pending-for root-path "00f95a")))]
        (is (= overflow-body (:message pending)))
        (is (= overflow-body (get-in pending [:attempt :body]))))
      (with-redefs [hm/primary-root (constantly primary)]
        (is (= "Transported.{ 00f95a working }" (hm/send! "00f95a" overflow-body false nil))))
      (is (= #{overflow-body}
             (set (keep :body (filter #(contains? #{:Submitting :sent} (:reason %))
                                      (store/attempts-for root-path "00f95a"))))))
      (let [writes (atom 0)
            ledger (reify hm/Ledger
                     (record-attempt! [_ _] (if (= 2 (swap! writes inc)) (hm/fail "sent ledger unavailable") :ok))
                     (record-pending! [_ _ _] :ok))]
        (binding [hm/*ledger* ledger]
          (is (re-find #"prompt was delivered but ledger confirmation failed; do not retry"
                       (try (hm/send! "00f95a" "body" false nil) (catch Exception error (.getMessage error)))))
          (is (= 2 @prompts)))))))

(deftest listing-joins-live-agents-from-typed-routes
  (let [root-path (str (fs/create-temp-dir {:prefix "hm-list-"}))
        second-agent {:session "s" :name "Other 123" :pane_id "x" :terminal_id "u" :agent "codex" :agent_status "idle"}]
    (binding [hm/*root* root-path]
      (persist-route! root-path route)
      (with-redefs [hm/live-agents (constantly [(assoc route :agent_status "working") second-agent])]
        (is (= (str "FLOW\tAGENT\tSESSION\tSTATE\n"
                    "00f95a\tMind Sol 00f95a\ts\tworking\n"
                    "-\tOther 123\ts\tidle")
               (hm/listing!)))
        (let [queried (atom [])]
          (with-redefs [store/routes (fn [_] (swap! queried conj :routes) {"00f95a" route})]
            (hm/listing!)
            (is (= [:routes] @queried)))))
      (with-redefs [hm/live-agents (constantly [])]
        (is (= (str "FLOW\tAGENT\tSESSION\tSTATE\n"
                    "00f95a\tMind Sol 00f95a\ts\tSTALE")
               (hm/listing!)))))))

(deftest readiness-probe-requires-exact-native-turns
  (let [marker "HM_READY_12345678"
        transcript (fs/create-temp-file {:prefix "hm-rollout-" :suffix ".jsonl"})
        prompts (atom 0)
        agent (assoc route :interactive_ready false)
        transport (reify hm/HerdrTransport
                    (live-agents* [_] [agent])
                    (target-agent* [_ _] {:agent agent})
                    (process-info* [_ _] {:process_info {:foreground_processes []}})
                    (pane* [_ _] {:pane {}})
                    (move-pane* [_ _ _ _] {:move_result {}})
                    (send-keys* [_ _ _] {:ok true})
                    (prompt!* [_ _ _ _] (swap! prompts inc) {:ok true}))
        user {:type "event_msg" :payload {:thread_id (:native_thread route)
                                          :item {:type "UserMessage" :content [{:text (str "Reply " marker)}]}}}
        reply {:type "event_msg" :payload {:thread_id (:native_thread route)
                                           :item {:type "AgentMessage" :content [{:text marker}]}}}]
    (spit (str transcript) (str (json/generate-string user) "\n" (json/generate-string reply) "\n"))
    (binding [hm/*transport* transport hm/*readiness-attempts* 1]
      (is (= {:thread_id (:native_thread route) :rollout (str (fs/absolutize transcript)) :marker marker}
             (hm/readiness-probe! agent marker (:native_thread route) transcript)))
      (is (= 1 @prompts))
      (is (thrown? Exception (hm/readiness-probe! agent "not-a-marker" (:native_thread route) transcript)))
      (spit (str transcript) (str (json/generate-string {:type "user" :sessionId (:native_thread route) :message {:content marker}}) "\n"
                                  (json/generate-string {:type "assistant" :sessionId (:native_thread route) :message {:content marker}}) "\n"))
      (is (= "claude-transcript" (:evidence_kind (hm/readiness-probe! agent marker (:native_thread route) transcript))))
      (spit (str transcript) (str (json/generate-string (assoc-in reply [:payload :thread_id] "different")) "\n"))
      (is (thrown? Exception (hm/readiness-probe! agent marker (:native_thread route) transcript))))))

(deftest register-persists-proof-for-a-not-ready-agent
  (let [root-path (str (fs/create-temp-dir {:prefix "hm-register-proof-"}))
        marker "HM_READY_87654321"
        transcript (fs/create-temp-file {:prefix "hm-register-rollout-" :suffix ".jsonl"})
        agent (assoc route :interactive_ready false)
        rows [{:type "event_msg" :payload {:thread_id (:native_thread route)
                                           :item {:type "UserMessage" :content [{:text marker}]}}}
              {:type "event_msg" :payload {:thread_id (:native_thread route)
                                           :item {:type "AgentMessage" :content [{:text marker}]}}}]]
    (spit (str transcript) (str/join "\n" (map json/generate-string rows)))
    (binding [hm/*root* root-path hm/*readiness-attempts* 1]
      (with-redefs [hm/herdr! (fn [& args]
                                (if (some #{"list"} args) {:agents [agent]} {:ok true}))]
        (is (thrown? Exception (hm/register! "00f95a" (:name route) "s" (:native_thread route) nil nil)))
        (is (= "Registered 00f95a: Mind Sol 00f95a (s)"
               (hm/register! "00f95a" (:name route) "s" (:native_thread route) marker transcript)))
        (is (= marker (get-in (hm/read-route "00f95a") [:readiness_proof :marker])))))))

(deftest deregister-and-rebind-keep-an-exact-live-binding
  (let [root-path (str (fs/create-temp-dir {:prefix "hm-lifecycle-"}))
        process [{:argv ["codex" "--thread" (:native_thread route)]}]
        rebound (assoc route :name "Mind Sol renamed" :interactive_ready true :agent_status "working")]
    (binding [hm/*root* root-path hm/*with-reservation* pass-reservation]
      (persist-route! root-path route)
      (is (= "Deregistered stale 00f95a: Mind Sol 00f95a (s/p/t)"
             (hm/deregister! "00f95a" "s" "p" "t" "Mind Sol 00f95a")))
      (is (nil? (store/route-for root-path "00f95a")))
      (persist-route! root-path route)
      (binding [hm/*transport* (fake-transport rebound rebound process (atom 0))]
        (is (= "Rebound 00f95a: Mind Sol 00f95a -> Mind Sol renamed (s/p/t)"
               (hm/rebind! "00f95a" "Mind Sol 00f95a" "Mind Sol renamed" "s" "p" "t" "codex" (:native_thread route))))
        (is (= "Mind Sol renamed" (:name (hm/read-route "00f95a"))))
        (is (thrown? Exception
                     (hm/rebind! "00f95a" "Mind Sol renamed" "again" "s" "p" "t" "codex" "different-native-thread")))))))

(deftest guarded-move-persists-hold-before-mutation-and-compensates
  (let [root-path (str (fs/create-temp-dir {:prefix "hm-move-"}))
        invoke (fn [mode]
                 (let [moves (atom "p")]
                   (binding [hm/*root* root-path hm/*with-reservation* pass-reservation
                             hm/*transport* (move-transport route mode moves)]
                     (persist-route! root-path route)
                     [(try (hm/move! "00f95a" "s" "p" "t" "Mind Sol 00f95a" "codex" (:native_thread route) 123 "w2")
                           (catch Exception error (.getMessage error)))
                      (hm/read-route "00f95a")])))]
    (let [[result record] (invoke :success)]
      (is (= "Moved 00f95a: w1/p -> w2/m (t)" result))
      (is (= "m" (:pane_id record)))
      (is (nil? (:route_hold record))))
    (let [[result record] (invoke :compensate)]
      (is (re-find #"returned to original workspace" result))
      (is (= "r" (:pane_id record)))
      (is (nil? (:route_hold record))))
    (let [[result record] (invoke :ambiguous)]
      (is (re-find #"delivery held for manual route repair" result))
      (is (= "pane_move_in_progress" (:route_hold record))))
    (let [moves (atom "p")]
      (binding [hm/*root* root-path hm/*with-reservation* pass-reservation
                hm/*transport* (move-transport route :success moves)]
        (persist-route! root-path (assoc route :route_hold "pane_move_in_progress"))
        (is (thrown? Exception (hm/move! "00f95a" "s" "p" "t" "Mind Sol 00f95a" "codex" (:native_thread route) 123 "w2")))
        (is (= "p" @moves))))))

(deftest retirement-is-evidence-bound-idempotent-and-blocks-reuse
  (let [root-path (str (fs/create-temp-dir {:prefix "hm-retire-"}))
        evidence (fs/create-temp-file {:prefix "hm-evidence-"})]
    (spit (str evidence) "witness")
    (binding [hm/*root* root-path hm/*with-reservation* pass-reservation]
      (persist-route! root-path route)
      (let [digest (hm/sha256 evidence)]
        (is (= "Retired 00f95a: delivery is blocked before Herdr routing"
               (hm/retire! "00f95a" "s" "p" "t" "Mind Sol 00f95a" "codex" (:native_thread route) evidence digest false)))
        (is (= "Already retired 00f95a: marker retained"
               (hm/retire! "00f95a" "s" "p" "t" "Mind Sol 00f95a" "codex" (:native_thread route) evidence digest false)))
        (is (thrown? Exception (hm/assert-not-retired! "00f95a")))
        (is (thrown? Exception (hm/assert-native-not-retired! (:native_thread route) "other-flow")))
        (is (thrown? Exception (hm/retire! "00f95a" "s" "other" "t" "Mind Sol 00f95a" "codex" (:native_thread route) evidence digest false)))
        (with-redefs [store/retirement-for (fn [_ flow] (when (= flow "broken") {:bad true}))]
          (is (thrown? Exception (hm/assert-not-retired! "broken"))))
        (is (= "Retired imported: delivery is blocked before Herdr routing"
               (hm/retire! "imported" "s" "p" "t" "Mind Sol 00f95a" "codex" (:native_thread route) evidence digest true)))))))
