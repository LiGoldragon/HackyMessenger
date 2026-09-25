(ns hacky-messenger.core
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [babashka.process :refer [shell]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [hacky-messenger.store :as store]
            [malli.core :as m]))

(def skill-note "Documented by the compensation-hacky-messenger skill (Curriculum skills/compensation-hacky-messenger.md). Update that skill with any change to this tool.")
(def failure-reasons #{:NotRegistered :InTransition :RouteHold :IdentityChanged :PaneMissing :NotReady :Blocked :ProcessMismatch :Stalled :Uncertain :RelayOverflow :Submitting :sent})
(def delivery-grades #{:Transported :Presented :Fallback-Presented :Held :Uncertain})
(def FlowId [:and [:string {:min 1 :max 96}] [:re #"^[A-Za-z0-9][A-Za-z0-9_-]*$"]])
(def NativeThread [:and [:string {:min 16 :max 96}] [:re #"^[A-Za-z0-9-]+$"]])
(def MessageBody [:string {:min 1 :max 65536}])
(def ReadinessProof [:map {:closed true} [:thread_id NativeThread] [:rollout :string] [:marker :string] [:evidence_kind {:optional true} :string]])
(def RouteBinding [:map {:closed true} [:session :string] [:name :string] [:pane_id :string] [:terminal_id :string] [:agent :string] [:native_thread NativeThread] [:readiness_proof {:optional true} ReadinessProof] [:route_hold {:optional true} :string] [:transition {:optional true} :boolean] [:state {:optional true} :string]])
(def DeliveryAttempt [:map {:closed true} [:id :string] [:at :string] [:flow FlowId] [:reason :keyword] [:grade {:optional true} :keyword] [:binding {:optional true} RouteBinding]])
(def PendingIntent [:map {:closed true} [:attempt DeliveryAttempt] [:message MessageBody] [:state [:= "held"]]])
(def RouteIdentity [:map {:closed true} [:session :string] [:name :string] [:pane_id :string] [:terminal_id :string] [:agent :string]])
(def RetirementEvidence [:map {:closed true} [:path :string] [:sha256 [:re #"^[0-9a-f]{64}$"]]])
(def RetirementMarker [:map {:closed true} [:version [:= 1]] [:state [:= "retired"]] [:flow FlowId] [:record RouteIdentity] [:native_thread NativeThread] [:evidence RetirementEvidence]])
(def Reservation [:map {:closed true} [:id :int] [:flow FlowId] [:root :string]])
(def MachineRelay [:tuple :string FlowId :string :string [:vector FlowId] MessageBody :string])
(doseq [schema [FlowId NativeThread MessageBody ReadinessProof RouteBinding DeliveryAttempt PendingIntent RetirementMarker Reservation MachineRelay]] (m/validator schema))

(defn fail [s] (throw (ex-info s {:hm/failure true})))
(defn valid! [schema value label] (if (m/validate schema value) value (fail (str "Invalid " label ": " (pr-str (m/explain schema value))))))
(declare atomic-edn! ->EdnLedger record-attempt! record-pending! route-records nonempty-strings!)
(defn flow-id! [value]
  (when-not (and (string? value) (re-matches #"[A-Za-z0-9][A-Za-z0-9_-]{0,95}" value))
    (fail "Flow ID must contain only letters, digits, underscores, or hyphens"))
  (valid! FlowId value "FlowId"))
(defn native-thread! [value]
  (when-not (and (string? value) (re-matches #"[A-Za-z0-9-]{16,96}" value)) (fail "Invalid NativeThread"))
  (valid! NativeThread value "NativeThread"))
(defn route-binding! [value] (valid! RouteBinding value "RouteBinding"))
(defn delivery-attempt! [value]
  (when-not (and (contains? failure-reasons (:reason value)) (contains? delivery-grades (:grade value)))
    (fail "Invalid DeliveryAttempt grade or reason"))
  (valid! DeliveryAttempt value "DeliveryAttempt"))
(defprotocol Registry (load-route [this flow]) (save-route! [this flow route]))
(defprotocol HerdrTransport
  (live-agents* [this])
  (target-agent* [this route])
  (process-info* [this route])
  (pane* [this route])
  (move-pane* [this route workspace label])
  (send-keys* [this route key])
  (prompt!* [this route envelope wait?]))
(defprotocol Ledger (record-attempt! [this attempt]) (record-pending! [this attempt body]))
(defprotocol Clock (current-time [this]))
(defrecord SystemClock [] Clock (current-time [_] (.toString (java.time.Instant/now))))
(defrecord EdnRegistry [state-root]
  Registry
  (load-route [_ flow] (edn/read-string (slurp (str (fs/path state-root (str (flow-id! flow) ".edn"))))))
  (save-route! [_ flow route] (atomic-edn! (fs/path state-root (str (flow-id! flow) ".edn")) (route-binding! route))))
(def ^:dynamic *root* nil)
(def ^:dynamic *flow-id* nil)
(def ^:dynamic *ledger* nil)
(def ^:dynamic *registry* nil)
(def ^:dynamic *clock* nil)
(def ^:dynamic *transport* nil)
(def ^:dynamic *readiness-attempts* 50)
;; A test seam around the Orchestrate boundary.  Production always uses
;; `with-reservation` below; tests supply a short-lived in-memory lease.
(def ^:dynamic *with-reservation* nil)
(defn root []
  ;; Clojure EDN records must never share Python's JSON registry by default.
  (fs/absolutize (or *root* (System/getenv "HM_REGISTRY")
                     (str (fs/path (System/getProperty "user.home") ".local/state/hacky-messenger-clojure")))))
(defn path [flow] (fs/path (root) (str (flow-id! flow) ".edn")))
(defn retired-path [flow] (fs/path (root) "retired" (str (flow-id! flow) ".edn")))
(defn registry [] (or *registry* (->EdnRegistry (root))))
(defn now [] (current-time (or *clock* (->SystemClock))))
(defn quote-datom [s] (str "«" (str/replace (str s) #"[\\»]" {\\ "\\\\" \» "\\»"}) "»"))
(defn relay [sender recipient body]
  ;; Canonical seven positions: ingress, sender, heard, seat, recipients, body, context.
  (let [heard (now)
        seat (or (System/getenv "MESSAGING_SEAT") "unknown")
        positions (valid! MachineRelay ["machine" sender heard seat [recipient] body ""] "Machine.Relay")
        line (binding [*print-namespace-maps* false] (pr-str {:machine/relay positions}))]
    (when (or (str/includes? line "\n") (> (count line) 800))
      (fail "Machine.Relay EDN must be one line of at most 800 characters; message held"))
    (when-not (= {:machine/relay positions} (edn/read-string line))
      (fail "Machine.Relay EDN round trip failed; message held"))
    line))
(defn nested-relay? [body]
  (or (str/includes? body "Machine.Relay.{")
      (try
        (let [value (edn/read-string body)]
          (boolean (some #(and (map? %) (contains? % :machine/relay))
                         (tree-seq coll? seq value))))
        (catch Exception _ false))))
(defn atomic-edn! [destination value]
  (fs/create-dirs (fs/parent destination))
  (let [tmp (fs/path (str destination ".tmp"))]
    (spit (str tmp) (str (pr-str value) "\n"))
    (fs/move tmp destination {:replace-existing true})))
(defn read-route [flow]
  (let [p (path flow)]
    (when-not (fs/exists? p) (fail (str "No valid registration for " flow)))
    (try (route-binding! (load-route (registry) flow))
         (catch Exception e (fail (str "No valid registration for " flow ": " (.getMessage e)))))))
(defn herdr! [& args]
  (let [{:keys [exit out err]} (apply shell {:out :string :err :string :continue true :timeout 15000} "herdr" args)]
    (when-not (zero? exit) (fail (or (not-empty (str/trim err)) (str "herdr failed: " exit))))
    (try (let [reply (json/parse-string out true)]
           (when-not (map? reply) (fail "Herdr returned malformed JSON object; do not blindly retry a send"))
           (when (:error reply) (fail (str "Herdr: " (:error reply))))
           (let [result (or (:result reply) reply)]
             (when-not (map? result) (fail "Herdr returned malformed result object; do not blindly retry a send"))
             result))
         (catch Exception _ (fail "Herdr returned invalid JSON; do not blindly retry a send")))))
(declare shell-live-agents)
(defn direct-prompt! [route envelope wait-presented]
  (let [args (cond-> ["--session" (:session route) "agent" "prompt" (:pane_id route) envelope]
               wait-presented (into ["--wait" "--timeout" "5000"]))]
    (apply herdr! args)))
(defn presented! [reply]
  ;; Herdr's supported `--wait` response is the observation boundary.  A
  ;; successful submission reply alone cannot establish pane presentation.
  (when-not (true? (:presented reply))
    (fail "Presentation was not observed; do not retry blindly"))
  reply)
(defrecord ShellHerdr []
  HerdrTransport
  (live-agents* [_] (shell-live-agents))
  (target-agent* [_ route] (herdr! "--session" (:session route) "agent" "get" (:pane_id route)))
  (process-info* [_ route] (herdr! "--session" (:session route) "pane" "process-info" "--pane" (:pane_id route)))
  (pane* [_ route] (herdr! "--session" (:session route) "pane" "get" (:pane_id route)))
  (move-pane* [_ route workspace label] (herdr! "--session" (:session route) "pane" "move" (:pane_id route) "--new-tab" "--workspace" workspace "--label" label "--no-focus"))
  (send-keys* [_ route key] (herdr! "--session" (:session route) "agent" "send-keys" (:pane_id route) key))
  (prompt!* [_ route envelope wait?] (direct-prompt! route envelope wait?)))
(defn transport [] (or *transport* (->ShellHerdr)))
(defn sha256 [file]
  (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest (doto (java.security.MessageDigest/getInstance "SHA-256") (.update (fs/read-all-bytes file)))))))
(defn retirement! [flow]
  (let [marker (retired-path flow)]
    (when (fs/exists? marker)
      (try
        (let [value (valid! RetirementMarker (edn/read-string (slurp (str marker))) "RetirementMarker")
              evidence (:evidence value)]
          (when-not (and (= flow (:flow value)) (fs/absolute? (:path evidence)) (fs/regular-file? (:path evidence))
                         (= (:sha256 evidence) (sha256 (:path evidence))))
            (fail "bad marker"))
          value)
        (catch Exception _ (fail (str "Retirement marker for " flow " is unavailable or malformed")))))))
(defn assert-not-retired! [flow]
  (when-let [marker (retirement! flow)]
    (fail (str "Retired: " flow " by " (get-in marker [:evidence :path])))))
(defn assert-native-not-retired! [native-thread flow]
  (doseq [marker (fs/glob (fs/path (root) "retired") "*.edn")]
    (let [other (fs/strip-ext (fs/file-name marker))]
      (when (and (not= flow other) (= native-thread (:native_thread (retirement! other))))
        (fail (str "Native thread " native-thread " is retired as Flow " other "; use a fresh native session"))))))
(defn claude-session-matches? [process native]
  (and (int? (:pid process))
       (try
         (let [file (fs/path (System/getProperty "user.home") ".claude" "sessions" (str (:pid process) ".json"))
               value (json/parse-string (slurp (str file)) true)]
           (= native (:sessionId value)))
         (catch Exception _ false))))
(defn process-matches! [route]
  (let [reply (process-info* (transport) route)
        info (or (:process_info reply) reply)
        processes (:foreground_processes info)
        native (native-thread! (:native_thread route))]
    (when-not (some #(or (str/includes? (str/join " " (map str (or (:argv %) []))) native)
                         (and (= "claude" (:agent route)) (claude-session-matches? % native)))
                    processes)
      (fail "ProcessMismatch"))))
(defn content-text [content]
  (cond
    (string? content) content
    (sequential? content) (str/join "" (keep #(when (map? %) (:text %)) content))
    :else ""))
(defn readiness-witness [rows marker native-thread rollout]
  (let [user-index (first (keep-indexed (fn [index row]
                                          (let [payload (:payload row) item (:item payload)]
                                            (when (and (= "event_msg" (:type row))
                                                       (= native-thread (:thread_id payload))
                                                       (= "UserMessage" (:type item))
                                                       (str/includes? (content-text (:content item)) marker))
                                              index))) rows))]
    (when user-index
      (some (fn [row]
              (let [payload (:payload row) item (:item payload)]
                (when (and (= native-thread (:thread_id payload))
                           (= "AgentMessage" (:type item))
                           (= marker (str/trim (content-text (:content item)))))
                  {:thread_id native-thread :rollout (str (fs/absolutize rollout)) :marker marker})))
            (drop (inc user-index) rows)))))
(defn claude-readiness-witness [rows marker native-thread rollout]
  (let [session-id #(or (:sessionId %) (:session_id %))
        user-index (first (keep-indexed (fn [index row]
                                          (when (and (= "user" (:type row))
                                                     (= native-thread (session-id row))
                                                     (str/includes? (content-text (get-in row [:message :content])) marker))
                                            index)) rows))]
    (when user-index
      (some (fn [row]
              (when (and (= "assistant" (:type row))
                         (= native-thread (session-id row))
                         (= marker (str/trim (content-text (get-in row [:message :content])))))
                {:thread_id native-thread :rollout (str (fs/absolutize rollout)) :marker marker
                 :evidence_kind "claude-transcript"}))
            (drop (inc user-index) rows)))))
(defn readiness-probe! [agent marker native-thread rollout]
  (when-not (and (string? marker) (re-matches #"HM_READY_[A-Za-z0-9_-]{8,96}" marker))
    (fail "Readiness probe marker must be a unique HM_READY token"))
  (native-thread! native-thread)
  (try
    (prompt!* (transport) agent (str "Reply exactly " marker " to confirm this explicit HM readiness probe.") false)
    (catch Exception error
      ;; Herdr can report this after injecting into a resumed Codex pane.  Only
      ;; the exact native transcript witness below can turn it into readiness.
      (when-not (str/includes? (or (.getMessage error) "") "agent_prompt_stalled")
        (throw error))))
  (when-not rollout (fail "Readiness probe requires a native Codex rollout or Claude transcript"))
  (loop [remaining *readiness-attempts*]
    (let [rows (try (mapv #(json/parse-string % true)
                          (remove str/blank? (str/split-lines (slurp (str rollout)))))
                    (catch Exception _ (fail "Readiness probe rollout is unavailable or invalid")))]
      (or (readiness-witness rows marker native-thread rollout)
          (claude-readiness-witness rows marker native-thread rollout)
          (if (pos? (dec remaining))
            (do (Thread/sleep 100) (recur (dec remaining)))
            (fail "Readiness probe marker was not observed in an exact native assistant reply"))))))
(defn verify-target! [route]
  (let [reply (target-agent* (transport) route)
        agent (or (:agent reply) reply)]
    (when-not (and (= (:name route) (:name agent))
                   (= (:pane_id route) (:pane_id agent))
                   (= (:terminal_id route) (:terminal_id agent))
                   (= (:agent route) (:agent agent)))
      (fail "IdentityChanged"))
    (when (and (not (:interactive_ready agent))
               (not= (:native_thread route) (get-in route [:readiness_proof :thread_id])))
      (fail "NotReady"))
    (when (= "blocked" (:agent_status agent)) (fail "Blocked"))
    (when-not (contains? #{"idle" "working" "done"} (:agent_status agent)) (fail "Uncertain"))
    (process-matches! route)
    agent))
(defn shell-live-agents []
  (mapcat (fn [session]
            (map #(assoc % :session (:name session))
                 (:agents (herdr! "--session" (:name session) "agent" "list"))))
          (filter :running (:sessions (herdr! "session" "list" "--json")))))
(defn live-agents [] (live-agents* (transport)))
(defn parse-pane [value]
  (let [[session pane] (str/split (or value "") #":" 2)]
    (when (or (str/blank? session) (str/blank? pane)) (fail "--pane requires <session>:<pane>"))
    {:session session :pane_id pane}))
(defn fallback-route [flow stored pane]
  (cond
    pane (let [{:keys [session pane_id]} (parse-pane pane)
               hits (filter #(and (= session (:session %)) (= pane_id (:pane_id %))) (live-agents))]
           (when-not (= 1 (count hits)) (fail "Held: --pane does not name exactly one live Herdr agent"))
           (route-binding! (assoc (select-keys (first hits) [:name :pane_id :terminal_id :agent]) :session session :native_thread (or (:native_thread stored) "00000000-0000-0000-0000-000000000000"))))
    stored (let [hits (filter #(and (= (:session stored) (:session %)) (= (:name stored) (:name %))) (live-agents))]
             (when-not (= 1 (count hits)) (fail "Held: stored route has no unique live Herdr agent"))
             (assoc (first hits) :native_thread (:native_thread stored)))
    :else (let [suffix (re-pattern (str "\\b" (java.util.regex.Pattern/quote flow) "$"))
                hits (filter #(re-find suffix (or (:name %) "")) (live-agents))]
            (when-not (= 1 (count hits)) (fail "Held: Flow title has no unique live Herdr agent"))
            (let [a (first hits)] (route-binding! (assoc (select-keys a [:session :name :pane_id :terminal_id :agent]) :native_thread "00000000-0000-0000-0000-000000000000"))))))
(defn exact-live-route? [stored]
  (some #(and (= (:session stored) (:session %))
              (= (:name stored) (:name %))
              (= (:pane_id stored) (:pane_id %))
              (= (:terminal_id stored) (:terminal_id %))
              (= (:agent stored) (:agent %)))
        (live-agents)))
(defn resolve-send-route [flow stored pane]
  (cond
    pane [(fallback-route flow stored pane) true]
    (and stored (exact-live-route? stored)) [stored false]
    stored [(fallback-route flow stored nil) true]
    :else [(fallback-route flow nil nil) true]))
(defn in-transition? [route] (or (:transition route) (= "transition" (:state route))))
(defn append-attempt! [flow reason grade route]
  (let [attempt (cond-> {:id (str (java.util.UUID/randomUUID)) :at (now) :flow flow :reason reason :grade grade}
                  route (assoc :binding route))]
    (delivery-attempt! attempt)
    (fs/create-dirs (root))
    (record-attempt! (or *ledger* (->EdnLedger (root))) attempt)
    ;; The production ledger must be queryable before a prompt may rely on its
    ;; pre-prompt attempt.  Injected test ledgers own their own persistence.
    (when-not *ledger*
      (when-not (some #(= (first %) (str "attempt/" (:id attempt))) (store/attempts-for (root) flow))
        (fail "Attempt ledger index did not confirm persistence")))
    attempt))
(defn held-reason [error]
  (let [message (.getMessage error)
        candidate (keyword (or message ""))]
    (if (contains? failure-reasons candidate) candidate :PaneMissing)))
(defn record-uncertain! [flow route]
  ;; The pre-prompt record is already durable.  Keep the original uncertainty
  ;; if storage is unavailable while recording this post-submit observation.
  (try (append-attempt! flow :Uncertain :Uncertain route) (catch Exception _ nil)))
(defn reserve! [flow]
  (let [owner (flow-id! (or *flow-id* (System/getenv "FLOW_ID") flow))
        reply (shell {:out :string :err :string :continue true :timeout 15000}
                     "orchestrate"
                     (str "Lock.{ HackyMessengerDelivery " owner " [ " (quote-datom (root)) " ] «Register or submit through Herdr» }"))
        match (re-find #"Locked\.\{\s+(\d+)\b" (:out reply))]
    (when-not (and (zero? (:exit reply)) match)
      (fail (str "Reservation refused: " (str/trim (or (not-empty (:out reply)) (:err reply) "")))))
    (valid! Reservation {:id (parse-long (second match)) :flow flow :root (str (root))} "Reservation")))
(defn release! [reservation]
  (let [reply (shell {:out :string :err :string :continue true :timeout 15000} "orchestrate" (str "Release." (:id reservation)))]
    (when-not (and (zero? (:exit reply)) (str/starts-with? (:out reply) "Released."))
      (fail (str "Reservation release failed: " (str/trim (or (not-empty (:out reply)) (:err reply) "")))))))
(defn with-reservation [flow f]
  (if *with-reservation*
    (*with-reservation* flow f)
    (let [reservation (reserve! flow)]
      (try (f) (finally (release! reservation))))))
(defrecord EdnLedger [state-root]
  Ledger
  (record-attempt! [_ attempt]
    (spit (str (fs/path state-root "attempts.edn")) (str (pr-str attempt) "\n") :append true)
    (store/index-attempt! state-root attempt)
    attempt)
  (record-pending! [_ attempt body]
    (store/index-pending! state-root attempt body)
    attempt))
(defn held! [flow reason body route]
  (let [attempt (append-attempt! flow reason :Held route)
        pending (valid! PendingIntent {:attempt attempt :message body :state "held"} "PendingIntent")
        destination (fs/path (root) "pending" (str (:id attempt) ".edn"))]
    (atomic-edn! destination pending)
    (record-pending! (or *ledger* (->EdnLedger (root))) attempt body)
    (when-not *ledger*
      (when-not (some #(= (first %) (str "pending/" (:id attempt))) (store/pending-for (root) flow))
        (fail "Pending ledger index did not confirm persistence")))
    (throw (ex-info (str "Held.{ " flow " " (name reason) " attempt-" (subs (:id attempt) 0 12) " }")
                    {:hm/failure true :hm/held true}))))
(defn register! [flow name session native-thread readiness-marker rollout]
  (flow-id! flow)
  (with-reservation flow
    (fn []
      (assert-not-retired! flow)
      (let [existing (when (fs/exists? (path flow)) (read-route flow))]
        (when (:route_hold existing) (fail "Registration is held for route repair"))
        (let [agents (if session (:agents (herdr! "--session" session "agent" "list")) (live-agents))
              found (filter #(= name (:name %)) agents)]
          (when-not (= 1 (count found)) (fail (str "Expected one live agent named " name "; found " (count found) ". Use --session.")))
          (let [a (assoc (first found) :session (or session (:session (first found))))
                session (:session a)
                observed (herdr! "--session" session "pane" "process-info" "--pane" (:pane_id a))
                native-thread (or native-thread (:native_thread existing)
                                  (second (re-find #"([A-Za-z0-9-]{16,96})" (pr-str observed))))
                _ (native-thread! native-thread)
                _ (assert-native-not-retired! native-thread flow)
                _ (nonempty-strings! "Herdr registration has no agent kind" [(:agent a)])
                proof (when-not (:interactive_ready a)
                        (if readiness-marker
                          (readiness-probe! a readiness-marker native-thread rollout)
                          (fail "Agent is not interactively ready")))
                route (valid! RouteBinding (cond-> (assoc (select-keys a [:session :name :pane_id :terminal_id :agent]) :native_thread native-thread)
                                             proof (assoc :readiness_proof proof)) "RouteBinding")]
            (when (and existing (not= (:terminal_id existing) (:terminal_id route)))
              (fail "Flow is already registered to a different terminal"))
            (save-route! (registry) flow route)
            (store/index-route! (root) flow route)
            (str "Registered " flow ": " name " (" session ")")))))))
(defn nonempty-strings! [label fields]
  (when-not (every? #(and (string? %) (not (str/blank? %))) fields)
    (fail label)))
(defn deregister! [flow session pane-id terminal-id name]
  (flow-id! flow)
  (nonempty-strings! "Deregister requires every exact stale route identity field" [session pane-id terminal-id name])
  (with-reservation flow
    (fn []
      (let [actual (read-route flow)]
        (when-not (= {:session session :pane_id pane-id :terminal_id terminal-id :name name}
                     (select-keys actual [:session :pane_id :terminal_id :name]))
          (fail "Registration differs from the explicitly revalidated stale route"))
        (fs/delete (path flow))
        (store/remove-route! (root) flow)
        (str "Deregistered stale " flow ": " name " (" session "/" pane-id "/" terminal-id ")")))))
(defn retirement-evidence! [evidence-path evidence-sha256]
  (let [path (fs/absolutize evidence-path)]
    (when-not (and (fs/absolute? path) (fs/regular-file? path) (string? evidence-sha256) (re-matches #"[0-9a-f]{64}" evidence-sha256))
      (fail "Retirement evidence requires an existing absolute file and SHA-256 digest"))
    (let [actual (sha256 path)]
      (when-not (= evidence-sha256 actual) (fail "Retirement evidence SHA-256 does not match; nothing changed"))
      {:path (str path) :sha256 actual})))
(defn retire! [flow session pane-id terminal-id name agent native-thread evidence-path evidence-sha256 allow-absent?]
  (flow-id! flow)
  (nonempty-strings! "Retirement requires every exact route identity field" [session pane-id terminal-id name agent])
  (native-thread! native-thread)
  (let [evidence (retirement-evidence! evidence-path evidence-sha256)
        expected {:session session :pane_id pane-id :terminal_id terminal-id :name name :agent agent}]
    (with-reservation flow
      (fn []
        (if-let [existing (retirement! flow)]
          (if (and (= expected (:record existing)) (= native-thread (:native_thread existing)))
            (str "Already retired " flow ": marker retained")
            (fail (str "Flow " flow " already has a different retirement marker")))
          (do
            (if (fs/exists? (path flow))
              (when-not (= expected (select-keys (read-route flow) (keys expected)))
                (fail "Registration differs from the explicitly revalidated retirement route"))
              (when-not allow-absent?
                (fail "No current registration; use import-retirement only with retained exact evidence")))
            (atomic-edn! (retired-path flow)
                         (valid! RetirementMarker {:version 1 :state "retired" :flow flow :record expected
                                                   :native_thread native-thread :evidence evidence} "RetirementMarker"))
            (str "Retired " flow ": delivery is blocked before Herdr routing")))))))
(defn rebind! [flow old-name new-name session pane-id terminal-id agent native-thread]
  (flow-id! flow)
  (nonempty-strings! "Rebind requires every exact old route identity field" [old-name session pane-id terminal-id agent])
  (when (or (not (string? new-name)) (str/blank? new-name) (= old-name new-name))
    (fail "Rebind requires a distinct nonempty new agent name"))
  (native-thread! native-thread)
  (with-reservation flow
    (fn []
      (assert-not-retired! flow)
      (let [actual (read-route flow)
            expected {:session session :name old-name :pane_id pane-id :terminal_id terminal-id :agent agent}]
        (when (:route_hold actual) (fail "Registration is held for route repair"))
        (when-not (= expected (select-keys actual (keys expected)))
          (fail "Registration differs from the explicitly revalidated old binding"))
        (when-not (= native-thread (:native_thread actual))
          (fail "Registration differs from the explicitly revalidated native thread"))
        (when (some #(and (not= flow (key %)) (= session (get-in % [1 :session])) (= new-name (get-in % [1 :name])))
                    (route-records))
          (fail (str "New agent name " new-name " is already registered in " session)))
        (let [replacement (route-binding! (assoc actual :name new-name))]
          ;; `verify-target!` checks exact live identity, readiness, blocked
          ;; state, and native process before the registry name is changed.
          (verify-target! replacement)
          (atomic-edn! (path flow) replacement)
          (store/index-route! (root) flow replacement)
          (str "Rebound " flow ": " old-name " -> " new-name " (" session "/" pane-id "/" terminal-id ")"))))))
(defn move-route! [flow record pane-id hold?]
  (let [replacement (cond-> (assoc record :pane_id pane-id)
                      hold? (assoc :route_hold "pane_move_in_progress")
                      (not hold?) (dissoc :route_hold))]
    (atomic-edn! (path flow) replacement)
    (store/index-route! (root) flow replacement)
    replacement))
(defn pane-value [reply] (or (:pane reply) reply))
(defn move-result-value [reply] (or (:move_result reply) reply))
(defn verify-move-target! [expected pane process-pid native-thread]
  (let [route (assoc expected :pane_id (:pane_id pane))
        live-pane (pane-value (pane* (transport) route))
        process-reply (process-info* (transport) route)
        info (or (:process_info process-reply) process-reply)
        processes (:foreground_processes info)
        matches (filter #(and (= (:session expected) (:session %))
                              (= (:name expected) (:name %))
                              (= (:pane_id route) (:pane_id %))
                              (= (:terminal_id expected) (:terminal_id %))
                              (= (:agent expected) (:agent %)))
                        (live-agents))]
    (when-not (and (= (:terminal_id expected) (:terminal_id pane))
                   (= (:terminal_id expected) (:terminal_id live-pane))
                   (= (:agent expected) (:agent live-pane)))
      (fail "Moved pane terminal or harness identity changed"))
    (when-not (some #(= process-pid (:pid %)) processes)
      (fail "Moved pane foreground process identity changed"))
    (when-not (or (= "codex" (:agent expected))
                  (some #(str/includes? (str/join " " (map str (or (:argv %) []))) native-thread) processes))
      (fail "Moved pane native session identity changed"))
    (when-not (= 1 (count matches))
      (fail "Moved pane has no unique matching Herdr agent"))
    route))
(defn move! [flow session pane-id terminal-id name agent native-thread process-pid workspace]
  (flow-id! flow)
  (nonempty-strings! "Move requires the complete old route" [session pane-id terminal-id name agent])
  (native-thread! native-thread)
  (when-not (and (integer? process-pid) (pos? process-pid))
    (fail "Move requires a witnessed positive foreground process PID"))
  (when-not (and (string? workspace) (re-matches #"w[A-Za-z0-9]+" workspace))
    (fail "Move requires an exact Herdr workspace ID"))
  (with-reservation flow
    (fn []
      (assert-not-retired! flow)
      (let [record (read-route flow)
            expected {:session session :name name :pane_id pane-id :terminal_id terminal-id :agent agent}]
        (when (:route_hold record) (fail "Registration is held for route repair; move refused"))
        (when-not (and (= expected (select-keys record (keys expected))) (= native-thread (:native_thread record)))
          (fail "Move old route or native thread differs from registration"))
        (let [source (pane-value (pane* (transport) record))
              old-workspace (:workspace_id source)]
          (verify-move-target! expected source process-pid native-thread)
          (when (some #(and (not= flow (key %)) (= terminal-id (get-in % [1 :terminal_id]))) (route-records))
            (fail "Terminal is registered to another Flow"))
          ;; The persisted hold is the boundary before an irreversible pane mutation.
          (move-route! flow record pane-id true)
          (let [moved (atom nil)]
            (try
              (let [result (move-result-value (move-pane* (transport) record workspace (or (:label source) name)))
                    pane (:pane result)]
                (reset! moved pane)
                (when-not (and (= pane-id (:previous_pane_id result))
                               (= old-workspace (:previous_workspace_id result))
                               (= workspace (:workspace_id pane)))
                  (fail "Herdr move result differs from requested route"))
                (let [verified (verify-move-target! expected pane process-pid native-thread)]
                  (move-route! flow record (:pane_id verified) false)
                  (str "Moved " flow ": " old-workspace "/" pane-id " -> " workspace "/" (:pane_id verified) " (" terminal-id ")")))
              (catch Exception error
                (if-not @moved
                  (fail (str "Move failed or is uncertain; inspect exact terminal before routing: " (.getMessage error)))
                  (let [rollback-error (try
                                         (let [reverse-result (move-result-value (move-pane* (transport) (assoc record :pane_id (:pane_id @moved)) old-workspace (or (:label source) name)))
                                               reverse (:pane reverse-result)
                                               verified (verify-move-target! expected reverse process-pid native-thread)]
                                           (move-route! flow record (:pane_id verified) false)
                                           nil)
                                         (catch Exception rollback-error rollback-error))]
                    (if rollback-error
                      (fail (str "Move and compensation failed; delivery held for manual route repair: " (.getMessage rollback-error)))
                      (fail (str "Move failed; terminal was returned to original workspace with new pane ID: " (.getMessage error))))))))))))))
(def abrupt-keys {"codex" {:interrupt ["esc"] :submit []}
                  "claude" {:interrupt ["esc" "esc"] :submit ["enter"]}})
(defn send-abrupt! [flow body wait-presented]
  (flow-id! flow) (valid! MessageBody body "MessageBody")
  (when (or (str/blank? body) (re-find #"[\p{Cc}&&[^\n\t]]" body)) (fail "Message must be nonempty and contain no terminal control characters"))
  (when (nested-relay? body) (fail "Nested Machine.Relay is not a message body"))
  (let [sender (or *flow-id* (System/getenv "FLOW_ID") (fail "Set FLOW_ID to your own flow ID before sending"))]
    (with-reservation flow
      (fn []
        (assert-not-retired! flow)
        (let [route (read-route flow)]
          (when (in-transition? route) (held! flow :InTransition body route))
          (when (:route_hold route) (held! flow :RouteHold body route))
          (let [live (try (verify-target! route) (catch Exception error (held! flow (held-reason error) body route)))
                keys (get abrupt-keys (:agent route))]
            (when-not keys (fail (str "Hard-abrupt is not supported for " (:agent route) "; nothing sent")))
            (let [envelope (try (relay sender flow body)
                                (catch Exception _ (held! flow :RelayOverflow body route)))
                  submission (append-attempt! flow :Submitting :Uncertain route)]
              (try
                (doseq [key (:interrupt keys)] (send-keys* (transport) route key))
                (let [reply (prompt!* (transport) route envelope wait-presented)]
                  (when wait-presented (presented! reply)))
                (doseq [key (:submit keys)] (send-keys* (transport) route key))
              ;; A successful prompt does not prove the terminal stayed bound.
              ;; Recheck before reporting any delivery grade.
                (verify-target! route)
                (try
                  (append-attempt! flow :sent (if wait-presented :Presented :Transported) route)
                  (str (if wait-presented "Presented" "Transported") ".{ " flow " " (or (:agent_status live) "unknown") " }")
                  (catch Exception error
                    (record-uncertain! flow route)
                    (throw (ex-info (str "Uncertain.{ " flow " attempt-" (subs (:id submission) 0 12) " } prompt was delivered but ledger confirmation failed; do not retry: " (.getMessage error))
                                    {:hm/failure true :hm/post-ledger true}))))
                (catch Exception error
                  (if (:hm/post-ledger (ex-data error))
                    (throw error)
                    (do (record-uncertain! flow route)
                        (fail (str "Uncertain.{ " flow " attempt-" (subs (:id submission) 0 12) " } Escape was sent; prompt failed or is uncertain: " (.getMessage error))))))))))))))
(defn record-sent! [flow grade route submission live]
  (try
    (append-attempt! flow :sent grade route)
    (str (name grade) ".{ " flow " " (or (:agent_status live) "unknown") " }")
    (catch Exception error
      (record-uncertain! flow route)
      (throw (ex-info (str "Uncertain.{ " flow " attempt-" (subs (:id submission) 0 12)
                           " } prompt was delivered but ledger confirmation failed; do not retry: " (.getMessage error))
                      {:hm/failure true :hm/post-ledger true})))))
(defn send!
  ([flow body wait-presented pane] (send! flow body wait-presented pane 10))
  ([flow body wait-presented pane hold-seconds]
   (flow-id! flow) (valid! MessageBody body "MessageBody")
   (when (or (str/blank? body) (re-find #"[\p{Cc}&&[^\n\t]]" body)) (fail "Message must be nonempty and contain no terminal control characters"))
   (when (nested-relay? body) (fail "Nested Machine.Relay is not a message body"))
   (let [sender (or *flow-id* (System/getenv "FLOW_ID") (fail "Set FLOW_ID to your own flow ID before sending"))]
     (with-reservation flow
       (fn []
         (assert-not-retired! flow)
         (let [stored (try (read-route flow) (catch Exception _ nil))
               stored (if (and (nil? stored) (pos? hold-seconds))
                        (do (Thread/sleep (long (* 1000 hold-seconds)))
                            (try (read-route flow) (catch Exception _ nil)))
                        stored)]
           (when (in-transition? stored) (held! flow :InTransition body stored))
           (when (:route_hold stored) (held! flow :RouteHold body stored))
           (let [[route fallback?] (try (resolve-send-route flow stored pane)
                                        (catch Exception _ (held! flow (if stored :PaneMissing :NotRegistered) body stored)))
                 live (try (verify-target! route) (catch Exception error (held! flow (held-reason error) body route)))
                 envelope (try (relay sender flow body) (catch Exception _ (held! flow :RelayOverflow body route)))
                 submission (append-attempt! flow :Submitting :Uncertain route)
                 grade (if fallback? :Fallback-Presented (if wait-presented :Presented :Transported))]
             (try
               (let [reply (prompt!* (transport) route envelope (or fallback? wait-presented))]
                 (when fallback? (presented! reply)))
               (verify-target! route)
               (record-sent! flow grade route submission live)
               (catch Exception error
                 (if (:hm/post-ledger (ex-data error))
                   (throw error)
                   (do (record-uncertain! flow route)
                       (fail (str "Uncertain.{ " flow " attempt-" (subs (:id submission) 0 12)
                                  " } prompt failed or is uncertain: " (.getMessage error))))))))))))))
(defn route-records []
  (let [indexed-flows (set (map first (store/routes-for (root))))]
    ;; The EDN binding remains the PoC's readable source record.  Datalevin
    ;; names the bindings and delivery history that belong in this view.
    (doseq [flow indexed-flows] (store/attempts-for (root) flow))
    (into {}
          (for [p (fs/glob (root) "*.edn")
                :let [flow (fs/strip-ext (fs/file-name p))]
                :when (and (contains? indexed-flows flow)
                           (string? flow)
                           (not= "attempts" flow)
                           (re-matches #"[A-Za-z0-9][A-Za-z0-9_-]{0,95}" flow))]
            [flow (read-route flow)]))))
(defn route-matches-agent? [route agent]
  (and (= (:session route) (:session agent))
       (= (:name route) (:name agent))
       (= (:pane_id route) (:pane_id agent))
       (= (:terminal_id route) (:terminal_id agent))
       (= (:agent route) (:agent agent))))
(defn listing! []
  (let [records (route-records)
        agents (live-agents)
        live-rows (for [agent agents
                        :let [flows (->> records
                                         (keep (fn [[flow route]] (when (route-matches-agent? route agent) flow)))
                                         sort)]]
                    (str (if (seq flows) (str/join "," flows) "-") "\t"
                         (or (:name agent) "-") "\t" (:session agent) "\t"
                         (or (:agent_status agent) "unknown")))
        matched (set (mapcat (fn [agent]
                               (keep (fn [[flow route]] (when (route-matches-agent? route agent) flow)) records))
                             agents))
        stale-rows (for [[flow route] (sort-by key (remove (fn [[flow _]] (contains? matched flow)) records))]
                     (str flow "\t" (:name route) "\t" (:session route) "\tSTALE"))]
    (str/join "\n" (concat ["FLOW\tAGENT\tSESSION\tSTATE"] live-rows stale-rows))))
