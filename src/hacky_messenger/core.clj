(ns hacky-messenger.core
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [babashka.process :refer [shell]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [hacky-messenger.store :as store]
            [malli.core :as m]))

(def skill-note "Documented by the compensation-hacky-messenger skill (Curriculum skills/compensation-hacky-messenger.md). Update that skill with any change to this tool.")
(def failure-reasons #{:NotRegistered :InTransition :RouteHold :IdentityChanged :PaneMissing :NotReady :Blocked :ProcessMismatch :Stalled :Uncertain :Submitting :sent})
(def delivery-grades #{:Transported :Presented :Fallback-Presented :Held :Uncertain})
(def FlowId [:and [:string {:min 1 :max 96}] [:re #"^[A-Za-z0-9][A-Za-z0-9_-]*$"]])
(def NativeThread [:and [:string {:min 16 :max 96}] [:re #"^[A-Za-z0-9-]+$"]])
(def MessageBody [:string {:min 1 :max 65536}])
(def ReadinessProof [:map [:thread_id NativeThread] [:rollout :string] [:marker :string]])
(def RouteBinding [:map [:session :string] [:name :string] [:pane_id :string] [:terminal_id :string] [:agent :string] [:native_thread NativeThread] [:readiness_proof {:optional true} ReadinessProof]])
(def DeliveryAttempt [:map [:id :string] [:at :string] [:flow FlowId] [:reason :keyword] [:grade {:optional true} :keyword] [:binding {:optional true} RouteBinding]])
(def PendingIntent [:map [:attempt DeliveryAttempt] [:message MessageBody] [:state [:= "held"]]])
(def RetirementMarker [:map [:flow FlowId] [:record RouteBinding] [:native_thread NativeThread]])
(def Reservation [:map [:id :int] [:flow FlowId] [:root :string]])
(def MachineRelay [:tuple :string FlowId :string :string [:vector FlowId] MessageBody :string])
(doseq [schema [FlowId NativeThread MessageBody ReadinessProof RouteBinding DeliveryAttempt PendingIntent RetirementMarker Reservation MachineRelay]] (m/validator schema))

(defn fail [s] (throw (ex-info s {:hm/failure true})))
(defn valid! [schema value label] (if (m/validate schema value) value (fail (str "Invalid " label ": " (pr-str (m/explain schema value))))))
(declare atomic-edn! ->EdnLedger record-attempt! record-pending!)
(defn flow-id! [value]
  (when-not (and (string? value) (re-matches #"[A-Za-z0-9][A-Za-z0-9_-]{0,95}" value)) (fail "Invalid FlowId"))
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
(def ^:dynamic *transport* nil)
;; A test seam around the Orchestrate boundary.  Production always uses
;; `with-reservation` below; tests supply a short-lived in-memory lease.
(def ^:dynamic *with-reservation* nil)
(defn root [] (fs/absolutize (or *root* (System/getenv "HM_REGISTRY") (str (fs/path (System/getProperty "user.home") ".local/state/hacky-messenger")))))
(defn path [flow] (fs/path (root) (str (flow-id! flow) ".edn")))
(defn retired-path [flow] (fs/path (root) "retired" (str (flow-id! flow) ".edn")))
(defn now [] (current-time (->SystemClock)))
(defn quote-datom [s] (str "«" (str/replace (str s) #"[\\»]" {\\ "\\\\" \» "\\»"}) "»"))
(defn relay [sender recipient body]
  ;; Canonical seven positions: ingress, sender, heard, seat, recipients, body, context.
  (let [heard (now)
        seat (or (System/getenv "MESSAGING_SEAT") "unknown")
        positions (valid! MachineRelay ["machine" sender heard seat [recipient] body ""] "Machine.Relay")
        line (pr-str {:machine/relay positions})]
    (when (or (str/includes? line "\n") (> (count line) 800))
      (fail "Machine.Relay EDN must be one line of at most 800 characters; message held"))
    (when-not (= {:machine/relay positions} (edn/read-string line))
      (fail "Machine.Relay EDN round trip failed; message held"))
    line))
(defn atomic-edn! [destination value]
  (fs/create-dirs (fs/parent destination))
  (let [tmp (fs/path (str destination ".tmp"))]
    (spit (str tmp) (str (pr-str value) "\n"))
    (fs/move tmp destination {:replace-existing true})))
(defn read-route [flow]
  (let [p (path flow)]
    (when-not (fs/exists? p) (fail (str "No valid registration for " flow)))
    (try (route-binding! (load-route (->EdnRegistry (root)) flow))
         (catch Exception e (fail (str "No valid registration for " flow ": " (.getMessage e)))))))
(defn herdr! [& args]
  (let [{:keys [exit out err]} (apply shell {:out :string :err :string :timeout 15000} "herdr" args)]
    (when-not (zero? exit) (fail (or (not-empty (str/trim err)) (str "herdr failed: " exit))))
    (try (let [reply (json/parse-string out true)] (if (:error reply) (fail (str "Herdr: " (:error reply))) (or (:result reply) reply)))
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
  (prompt!* [_ route envelope wait?] (direct-prompt! route envelope wait?)))
(defn transport [] (or *transport* (->ShellHerdr)))
(defn assert-not-retired! [flow]
  (let [marker (retired-path flow)]
    (when (fs/exists? marker)
      (try
        (valid! RetirementMarker (edn/read-string (slurp (str marker))) "RetirementMarker")
        (catch Exception _ (fail (str "Retirement marker for " flow " is unavailable or malformed"))))
      (fail (str "Retired: " flow)))))
(defn process-matches! [route]
  (let [reply (process-info* (transport) route)
        info (or (:process_info reply) reply)
        processes (:foreground_processes info)
        native (native-thread! (:native_thread route))]
    (when-not (some #(str/includes? (str/join " " (map str (or (:argv %) []))) native) processes)
      (fail "ProcessMismatch"))))
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
    (process-matches! route)
    agent))
(defn shell-live-agents []
  (mapcat (fn [session]
            (map #(assoc % :session (:name session))
                 (:agents (herdr! "--session" (:name session) "agent" "list"))))
          (filter :running (:sessions (herdr! "session" "list" "--json")))))
(defn live-agents [] (live-agents* (transport)))
(defn parse-pane [value]
  (let [[session pane & extra] (str/split (or value "") #":" 3)]
    (when (or (str/blank? session) (str/blank? pane) (seq extra)) (fail "--pane requires <session>:<pane>"))
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
(defn append-attempt! [flow reason grade route]
  (let [attempt (cond-> {:id (str (java.util.UUID/randomUUID)) :at (now) :flow flow :reason reason :grade grade}
                  route (assoc :binding route))]
    (delivery-attempt! attempt)
    (fs/create-dirs (root))
    (record-attempt! (or *ledger* (->EdnLedger (root))) attempt)))
(defn reserve! [flow]
  (let [owner (flow-id! (or *flow-id* (System/getenv "FLOW_ID") flow))
        reply (shell {:out :string :err :string :timeout 15000}
                     "orchestrate"
                     (str "Lock.{ HackyMessengerDelivery " owner " [ " (quote-datom (root)) " ] «Register or submit through Herdr» }"))
        match (re-find #"Locked\.\{\s+(\d+)\b" (:out reply))]
    (when-not (and (zero? (:exit reply)) match)
      (fail (str "Reservation refused: " (str/trim (or (not-empty (:out reply)) (:err reply) "")))))
    (valid! Reservation {:id (parse-long (second match)) :flow flow :root (str (root))} "Reservation")))
(defn release! [reservation]
  (let [reply (shell {:out :string :err :string :timeout 15000} "orchestrate" (str "Release." (:id reservation)))]
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
    (fail (str "Held.{ " flow " " (name reason) " attempt-" (subs (:id attempt) 0 12) " }"))))
(defn register! [flow name session native-thread]
  (valid! FlowId flow "FlowId") (valid! NativeThread native-thread "NativeThread")
  (let [agents (:agents (herdr! "--session" session "agent" "list"))
        found (filter #(= name (:name %)) agents)]
    (when-not (= 1 (count found)) (fail (str "Expected one live agent named " name "; found " (count found) ". Use --session.")))
    (let [a (first found) route (valid! RouteBinding (assoc (select-keys a [:session :name :pane_id :terminal_id :agent]) :session session :native_thread native-thread) "RouteBinding")]
      (save-route! (->EdnRegistry (root)) flow route)
      (store/index-route! (root) flow route)
      (str "Registered " flow ": " name " (" session ")"))))
(defn send! [flow body wait-presented pane]
  (valid! FlowId flow "FlowId") (valid! MessageBody body "MessageBody")
  (when (or (str/blank? body) (re-find #"[\p{Cc}&&[^\n\t]]" body)) (fail "Message must be nonempty and contain no terminal control characters"))
  (let [sender (or *flow-id* (System/getenv "FLOW_ID") (fail "Set FLOW_ID to your own flow ID before sending"))]
    (with-reservation flow
      (fn []
        ;; The lifecycle check and route resolution share the delivery lease.
        (assert-not-retired! flow)
        (let [stored (try (read-route flow) (catch Exception _ nil))]
          (let [[route fallback?] (try (resolve-send-route flow stored pane)
                                       (catch Exception _
                                         (held! flow (if stored :PaneMissing :NotRegistered) body stored)))]
            (let [live (try (verify-target! route)
                            (catch Exception error
                              (held! flow (keyword (or (.getMessage error) "IdentityChanged")) body route)))
                  envelope (relay sender flow body)
                  submission (append-attempt! flow :Submitting :Uncertain route)]
              (try
                (let [wait? (or fallback? wait-presented)
                      reply (prompt!* (transport) route envelope wait?)]
                  (when fallback? (presented! reply)))
                (let [grade (if fallback? :Fallback-Presented (if wait-presented :Presented :Transported))]
                  (append-attempt! flow :sent grade route)
                  (str (name grade) ".{ " flow " " (or (:agent_status live) "unknown") " }"))
                (catch Exception error
                  (fail (str "Uncertain.{ " flow " attempt-" (subs (:id submission) 0 12) " } prompt failed or is uncertain: " (.getMessage error))))))))))))
(defn listing! []
  (let [records (for [p (fs/glob (root) "*.edn")
                      :when (not= "attempts.edn" (str (fs/file-name p)))]
                  [(fs/strip-ext (fs/file-name p)) (read-route (fs/strip-ext (fs/file-name p)))])]
    (str/join "\n" (concat ["FLOW\tAGENT\tSESSION\tSTATE"] (map (fn [[f r]] (str f "\t" (:name r) "\t" (:session r) "\tREGISTERED")) records)))))
