(ns messenger-clj.core
  (:import [java.io PushbackReader StringReader])
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [babashka.process :refer [shell]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [messenger-clj.typed-store :as store]
            [malli.core :as m]))

(def skill-note "Documented by the authored messaging skills in Curriculum. Update those sources with any change to this tool.")
(def failure-reasons #{:NotRegistered :NeedsBinding :InTransition :RouteHold :IdentityChanged :PaneMissing :NotReady :Blocked :ProcessMismatch :Stalled :Uncertain :RelayOverflow :Submitting :InvalidBinding :sent})
(def delivery-grades #{:Transported :Presented :Fallback-Presented :Held :Uncertain})
(def FlowId [:and [:string {:min 1 :max 96}] [:re #"^[A-Za-z0-9][A-Za-z0-9_-]*$"]])
(def NativeThread [:and [:string {:min 16 :max 96}] [:re #"^[A-Za-z0-9-]+$"]])
(def MessageBody [:string {:min 1}])
(def MessageVariant [:enum :msg :psyche])
(def ReadinessProof [:map {:closed true} [:thread_id NativeThread] [:rollout :string] [:marker :string] [:evidence_kind {:optional true} :string]])
(def RouteBinding [:map {:closed true} [:session :string] [:name :string] [:pane_id :string] [:terminal_id :string] [:agent :string] [:native_thread {:optional true} NativeThread] [:readiness_proof {:optional true} ReadinessProof] [:route_hold {:optional true} :string] [:transition {:optional true} :boolean] [:state {:optional true} :string]])
(def DeliveryAttempt [:map {:closed true} [:id :string] [:at :string] [:flow FlowId] [:reason :keyword] [:grade {:optional true} :keyword] [:variant {:optional true} MessageVariant] [:context {:optional true} [:maybe MessageBody]] [:body {:optional true} MessageBody] [:part_index {:optional true} pos-int?] [:part_count {:optional true} pos-int?] [:submitted {:optional true} :string] [:binding {:optional true} RouteBinding]])
(def PendingIntent [:map {:closed true} [:attempt DeliveryAttempt] [:message MessageBody] [:variant {:optional true} MessageVariant] [:context {:optional true} [:maybe MessageBody]] [:part_index {:optional true} pos-int?] [:part_count {:optional true} pos-int?] [:state [:= "held"]]])
(def RouteIdentity [:map {:closed true} [:session :string] [:name :string] [:pane_id :string] [:terminal_id :string] [:agent :string]])
(def RetirementEvidence [:map {:closed true} [:path :string] [:sha256 [:re #"^[0-9a-f]{64}$"]]])
(def RetirementMarker [:map {:closed true} [:version [:= 1]] [:state [:= "retired"]] [:flow FlowId] [:record RouteIdentity] [:native_thread NativeThread] [:evidence RetirementEvidence] [:retired_by :string] [:retired_at :string]])
(def Reservation [:map {:closed true} [:id :int] [:flow FlowId] [:root :string]])
(def PaneMessage [:tuple FlowId MessageBody])
(def PsycheMessage [:tuple FlowId [:maybe MessageBody] [:and :string [:re #"^[1-9][0-9]*/[1-9][0-9]*$"]] MessageBody])
(def MessageRequest [:map {:closed true} [:variant MessageVariant] [:body MessageBody] [:context {:optional true} [:maybe MessageBody]] [:part_index {:optional true} pos-int?] [:part_count {:optional true} pos-int?]])
(doseq [schema [FlowId NativeThread MessageBody MessageVariant ReadinessProof RouteBinding DeliveryAttempt PendingIntent RetirementMarker Reservation PaneMessage PsycheMessage MessageRequest]] (m/validator schema))

(defn fail [s] (throw (ex-info s {:hm/failure true})))
(defn valid! [schema value label]
  (if (m/validate schema value)
    value
    (throw (ex-info (str "Invalid " label ": " (pr-str (m/explain schema value)))
                    {:hm/failure true :hm/invalid true}))))
(declare ->DatalevinLedger record-attempt! record-pending! route-records nonempty-strings!)
(defn flow-id! [value]
  (when-not (and (string? value) (re-matches #"[A-Za-z0-9][A-Za-z0-9_-]{0,95}" value))
    (fail "Flow ID must contain only letters, digits, underscores, or hyphens"))
  (valid! FlowId value "FlowId"))
(defn native-thread! [value]
  (when-not (and (string? value) (re-matches #"[A-Za-z0-9-]{16,96}" value)) (fail "Invalid NativeThread"))
  (valid! NativeThread value "NativeThread"))
(defn route-binding! [value]
  (try
    (valid! RouteBinding (store/route! value) "RouteBinding")
    (catch Exception error
      (throw (ex-info (.getMessage error)
                      (assoc (ex-data error) :hm/failure true :hm/invalid true)
                      error)))))
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
(defrecord DatalevinRegistry [state-root]
  Registry
  (load-route [_ flow] (store/route-for state-root (flow-id! flow)))
  (save-route! [_ flow route] (store/put-route! state-root (flow-id! flow) (route-binding! route))))
(def ^:dynamic *root* nil)
(def ^:dynamic *flow-id* nil)
(def ^:dynamic *ledger* nil)
(def ^:dynamic *registry* nil)
(def ^:dynamic *clock* nil)
(def ^:dynamic *transport* nil)
(def ^:dynamic *shell* shell)
(def ^:dynamic *readiness-attempts* 50)
(def ^:dynamic *reservation-wait-ms* 15000)
(def ^:dynamic *reservation-retry-ms* 50)
;; A test seam around the Orchestrate boundary.  Production always uses
;; `with-reservation` below; tests supply a short-lived in-memory lease.
(def ^:dynamic *with-reservation* nil)
(defn root []
  ;; The deployment migrates the typed database here while holding both state
  ;; roots and replacing every launcher in the same activation.
  (fs/absolutize (or *root* (System/getenv "HM_REGISTRY")
                     (str (fs/path (System/getProperty "user.home") ".local/state/messenger-clj")))))
(defn registry [] (or *registry* (->DatalevinRegistry (root))))
(defn now [] (current-time (or *clock* (->SystemClock))))
(defn quote-datom [s] (str "«" (str/replace (str s) #"[\\»]" {\\ "\\\\" \» "\\»"}) "»"))
(defn read-msg [value]
  ;; `data_readers.clj` binds #msg to this function for Clojure readers.  The
  ;; tagged value is deliberately just the two pane-visible fields.
  (valid! PaneMessage value "#msg"))
(defn read-psyche [value]
  (let [[_ _ label :as message] (valid! PsycheMessage value "#psyche")
        [_ index total] (re-matches #"([1-9][0-9]*)/([1-9][0-9]*)" label)]
    (when (> (parse-long index) (parse-long total))
      (fail "#psyche part index exceeds its count"))
    message))
(defn- read-complete [readers line]
  (with-open [reader (PushbackReader. (StringReader. line))]
    (let [eof (Object.)
          value (edn/read {:readers readers :eof eof} reader)]
      (when (or (identical? eof value)
                (not (identical? eof (edn/read {:readers readers :eof eof} reader))))
        (fail "Expected exactly one complete EDN form"))
      value)))
(defn read-pane-message [line]
  (let [tagged ::tagged
        value (read-complete {'msg #(hash-map tagged (read-msg %))} line)]
    (or (get value tagged) (fail "Expected one complete #msg form"))))
(defn read-psyche-message [line]
  (let [tagged ::tagged
        value (read-complete {'psyche #(hash-map tagged (read-psyche %))} line)]
    (or (get value tagged) (fail "Expected one complete #psyche form"))))
(defn request! [request]
  (let [request (valid! MessageRequest request "MessageRequest")]
    (when (not= (contains? request :part_index) (contains? request :part_count))
      (fail "Message part metadata must be complete"))
    (when (and (:part_index request) (> (:part_index request) (:part_count request)))
      (fail "Message part index exceeds its count"))
    (when (and (= :psyche (:variant request)) (not (contains? request :context)))
      (fail "Psyche messages require context"))
    (when (and (= :msg (:variant request)) (contains? request :context))
      (fail "Machine messages do not carry psyche context"))
    (when (and (= :msg (:variant request)) (:part_index request))
      (fail "Machine messages do not carry part metadata"))
    (when (and (= :psyche (:variant request)) (:part_index request)
               (not= (nil? (:context request)) (> (:part_index request) 1)))
      (fail "Only the first psyche part carries context"))
    request))
(defn message-envelope [sender request]
  (flow-id! sender)
  (let [{:keys [variant context body part_index part_count]} (request! request)
        [tag value reader] (case variant
                             :msg ["#msg" (read-msg [sender body]) read-pane-message]
                             :psyche (do
                                       (when-not part_index
                                         (fail "Psyche envelope requires numbered part metadata"))
                                       ["#psyche" (read-psyche [sender context (str part_index "/" part_count) body]) read-psyche-message]))
        envelope (str tag " " (pr-str value))]
    (when-not (= value (reader envelope))
      (fail (str tag " EDN round trip failed; message held")))
    envelope))

(def max-psyche-envelope-characters 800)
(defn psyche-envelope [sender context index total piece]
  (message-envelope sender {:variant :psyche :context context :body piece
                            :part_index index :part_count total}))
(defn- provisional-psyche-envelope [sender context index total piece]
  ;; Packing must measure labels before the final part count is known. The
  ;; provisional numerator may temporarily exceed the denominator.
  (str "#psyche " (pr-str [sender context (str index "/" total) piece])))
(defn- psyche-tokens [verbatim]
  ;; Whitespace itself is a safe boundary and may be packed one code point at
  ;; a time. Non-whitespace runs are indivisible words.
  (vec (re-seq #"\s|\S+" verbatim)))
(defn- pack-psyche-with-total [sender context verbatim total]
  (let [tokens (psyche-tokens verbatim)]
    (loop [remaining tokens index 1 parts []]
      (if (empty? remaining)
        parts
        (let [part-context (when (= index 1) context)
              [piece rest-tokens]
              (loop [accepted [] candidates remaining]
                (if-let [token (first candidates)]
                  (let [candidate (apply str (conj accepted token))]
                    (if (<= (count (provisional-psyche-envelope sender part-context index total candidate))
                            max-psyche-envelope-characters)
                      (recur (conj accepted token) (next candidates))
                      [(apply str accepted) candidates]))
                  [(apply str accepted) []]))]
          (when (empty? piece)
            (fail "Psyche context or one verbatim token cannot fit in an 800-character envelope; message held"))
          (recur (vec rest-tokens) (inc index) (conj parts piece)))))))
(defn psyche-requests [sender context verbatim]
  (loop [total 1 seen #{}]
    (when (contains? seen total)
      (fail "Psyche part labels did not stabilize; message held"))
    (let [pieces (pack-psyche-with-total sender context verbatim total)
          actual (count pieces)]
      (if (= total actual)
        (mapv (fn [index piece]
                {:variant :psyche :context (when (= index 1) context) :body piece
                 :part_index index :part_count actual})
              (range 1 (inc actual)) pieces)
        (recur actual (conj seen total))))))
(defn relay-line [sender _recipient body] (message-envelope sender {:variant :msg :body body}))
(defn relay [sender recipient body] (relay-line sender recipient body))
(defn framed-text [sender _recipient body] (message-envelope sender {:variant :msg :body body}))
(defn nested-relay? [body]
  (or (try (read-pane-message body) true (catch Exception _ false))
      (try (read-psyche-message body) true (catch Exception _ false))))
(defn read-route [flow]
  (try
    (if-let [route (load-route (registry) flow)]
      (route-binding! route)
      (fail (str "No valid registration for " flow)))
    (catch Exception e (fail (str "No valid registration for " flow ": " (.getMessage e))))))
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
(def presented-wait-args ["--wait" "--until" "working" "--until" "idle" "--until" "done"
                          "--until" "blocked" "--timeout" "10000"])
(defn direct-prompt! [route envelope wait-presented]
  (let [args (cond-> ["--session" (:session route) "agent" "prompt" (:pane_id route) envelope]
               wait-presented (into presented-wait-args))]
    (apply herdr! args)))
(defn presented! [route reply]
  ;; A waited Herdr prompt returns agent_prompted after observing one of the
  ;; requested lifecycle states.  Bind that observation to the exact pane.
  (when-not (and (= "agent_prompted" (:type reply))
                 (= (:pane_id route) (get-in reply [:agent :pane_id])))
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
  (when-let [stored (store/retirement-for (root) flow)]
    (try
      (let [value (valid! RetirementMarker stored "RetirementMarker")
            evidence (:evidence value)]
        (when-not (and (= flow (:flow value)) (fs/absolute? (:path evidence))
                       (fs/regular-file? (:path evidence))
                       (= (:sha256 evidence) (sha256 (:path evidence))))
          (fail "bad marker"))
        value)
      (catch Exception _ (fail (str "Retirement marker for " flow " is unavailable or malformed"))))))
(defn assert-not-retired! [flow]
  (when-let [marker (retirement! flow)]
    (fail (str "Retired: " flow " by " (get-in marker [:evidence :path])))))
(defn assert-native-not-retired! [native-thread flow]
  (doseq [marker (store/retirements (root))
          :let [other (:flow marker)]]
    (when (and (not= flow other) (= native-thread (:native_thread (retirement! other))))
      (fail (str "Native thread " native-thread " is retired as Flow " other "; use a fresh native session")))))
(defn claude-session-matches? [process native]
  (and (int? (:pid process))
       (try
         (let [file (fs/path (System/getProperty "user.home") ".claude" "sessions" (str (:pid process) ".json"))
               value (json/parse-string (slurp (str file)) true)]
           (= native (:sessionId value)))
         (catch Exception _ false))))
(defn process-matches! [route]
  ;; A fallback route without a stored native thread has no native identity to
  ;; compare.  Exact Herdr pane identity and readiness remain mandatory.
  (when-let [native-value (:native_thread route)]
    (let [reply (process-info* (transport) route)
          info (or (:process_info reply) reply)
          processes (:foreground_processes info)
          native (native-thread! native-value)]
      (when-not (some #(or (str/includes? (str/join " " (map str (or (:argv %) []))) native)
                           (and (= "claude" (:agent route)) (claude-session-matches? % native)))
                      processes)
        (fail "ProcessMismatch")))))
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
(defn fallback-binding [agent native-thread]
  (route-binding!
   (cond-> (assoc (select-keys agent [:session :name :pane_id :terminal_id :agent])
                  :state "Fallback")
     native-thread (assoc :native_thread native-thread))))
(defn fallback-route [flow stored pane]
  (cond
    pane (let [{:keys [session pane_id]} (parse-pane pane)
               hits (filter #(and (= session (:session %)) (= pane_id (:pane_id %))) (live-agents))]
           (when-not (= 1 (count hits)) (fail "Held: --pane does not name exactly one live Herdr agent"))
           (fallback-binding (assoc (first hits) :session session) (:native_thread stored)))
    stored (let [hits (filter #(and (= (:session stored) (:session %)) (= (:name stored) (:name %))) (live-agents))]
             (when-not (= 1 (count hits)) (fail "Held: stored route has no unique live Herdr agent"))
             (fallback-binding (first hits) (:native_thread stored)))
    :else (let [suffix (re-pattern (str "\\b" (java.util.regex.Pattern/quote flow) "$"))
                hits (filter #(re-find suffix (or (:name %) "")) (live-agents))]
            (when-not (= 1 (count hits)) (fail "Held: Flow title has no unique live Herdr agent"))
            (fallback-binding (first hits) nil))))
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
(defn needs-binding? [route] (= "NeedsBinding" (:state route)))
(defn attempt-fields [request submitted]
  (cond-> (select-keys (request! request) [:variant :context :body :part_index :part_count])
    submitted (assoc :submitted submitted)))
(defn append-attempt! [flow reason grade route request submitted]
  (let [attempt (cond-> {:id (str (java.util.UUID/randomUUID)) :at (now) :flow flow :reason reason :grade grade}
                  request (merge (attempt-fields request submitted))
                  route (assoc :binding route))]
    (delivery-attempt! attempt)
    (record-attempt! (or *ledger* (->DatalevinLedger (root))) attempt)
    ;; The production ledger must be queryable before a prompt may rely on its
    ;; pre-prompt attempt.  Injected test ledgers own their own persistence.
    (when-not *ledger*
      (when-not (= attempt (store/attempt-by-id (root) (:id attempt)))
        (fail "Attempt ledger index did not confirm persistence")))
    attempt))
(defn held-reason [error]
  (let [message (.getMessage error)
        invalid? (:hm/invalid (ex-data error))
        candidate (keyword (or message ""))]
    (cond invalid? :InvalidBinding
          (contains? failure-reasons candidate) candidate
          :else :PaneMissing)))
(defn record-uncertain! [flow route request submitted]
  ;; The pre-prompt record is already durable.  Keep the original uncertainty
  ;; if storage is unavailable while recording this post-submit observation.
  (try (append-attempt! flow :Uncertain :Uncertain route request submitted) (catch Exception _ nil)))
(defn contention? [reply]
  (boolean (re-find #"LockRejected\.(?:DuplicateName|PathConflict|PathOverlap)|(?:DuplicateName|PathConflict|PathOverlap)"
                    (str (:out reply) "\n" (:err reply)))))
(defn reserve! [flow]
  (let [owner (flow-id! (or *flow-id* (System/getenv "FLOW_ID") flow))
        operation (str "MessengerCljDelivery-"
                       (str/replace (str (java.util.UUID/randomUUID)) "-" ""))
        query (str "Lock.{ " operation " " owner " [ " (quote-datom (root))
                   " ] «Register or submit through Herdr» }")
        deadline (+ (System/nanoTime) (* 1000000 (long *reservation-wait-ms*)))]
    (loop []
      (let [reply (*shell* {:out :string :err :string :continue true :timeout 15000}
                           "orchestrate" query)
            match (re-find #"Locked\.\{\s+(\d+)\b" (:out reply))]
        (cond
          (and (zero? (:exit reply)) match)
          (valid! Reservation {:id (parse-long (second match)) :flow flow :root (str (root))}
                  "Reservation")

          (and (contention? reply) (< (System/nanoTime) deadline))
          (do (Thread/sleep (long *reservation-retry-ms*)) (recur))

          (contention? reply)
          (fail (str "Reservation timed out after " *reservation-wait-ms* "ms: "
                     (str/trim (or (not-empty (:out reply)) (:err reply) ""))))

          :else
          (fail (str "Reservation refused: "
                     (str/trim (or (not-empty (:out reply)) (:err reply) "")))))))))
(defn release! [reservation]
  (let [reply (*shell* {:out :string :err :string :continue true :timeout 15000} "orchestrate" (str "Release." (:id reservation)))]
    (when-not (and (zero? (:exit reply)) (str/starts-with? (:out reply) "Released."))
      (fail (str "Reservation release failed: " (str/trim (or (not-empty (:out reply)) (:err reply) "")))))))
(defn with-reservation [flow f]
  (if *with-reservation*
    (*with-reservation* flow f)
    (let [reservation (reserve! flow)]
      (try (f) (finally (release! reservation))))))
(defrecord DatalevinLedger [state-root]
  Ledger
  (record-attempt! [_ attempt]
    (store/put-attempt! state-root attempt)
    attempt)
  (record-pending! [_ attempt request]
    (let [{:keys [variant context body part_index part_count]} (request! request)]
      (store/put-pending! state-root
                          (valid! PendingIntent
                                  (cond-> {:attempt attempt :message body :variant variant :state "held"}
                                    (contains? request :context) (assoc :context context)
                                    part_index (assoc :part_index part_index :part_count part_count))
                                  "PendingIntent")))
    attempt))
(defn held! [flow reason request route]
  (let [route (when (and route (m/validate RouteBinding route)) route)
        {:keys [variant context body part_index part_count]} (request! request)
        attempt (append-attempt! flow reason :Held route request nil)
        pending (valid! PendingIntent
                        (cond-> {:attempt attempt :message body :variant variant :state "held"}
                          (contains? request :context) (assoc :context context)
                          part_index (assoc :part_index part_index :part_count part_count))
                        "PendingIntent")]
    (record-pending! (or *ledger* (->DatalevinLedger (root))) attempt request)
    (when-not *ledger*
      (when-not (= pending (store/pending-by-id (root) (:id attempt)))
        (fail "Pending ledger index did not confirm persistence")))
    (throw (ex-info (str "Held.{ " flow " " (name reason) " attempt-" (subs (:id attempt) 0 12) " }")
                    {:hm/failure true :hm/held true}))))
(defn register! [flow name session native-thread readiness-marker rollout]
  (flow-id! flow)
  (with-reservation flow
    (fn []
      (assert-not-retired! flow)
      (let [existing (load-route (registry) flow)]
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
            (if-let [route (store/stored-route-for (root) flow)]
              (when-not (= expected (select-keys route (keys expected)))
                (fail "Registration differs from the explicitly revalidated retirement route"))
              (when-not allow-absent?
                (fail "No current registration; use import-retirement only with retained exact evidence")))
            (store/put-retirement!
             (root)
             (valid! RetirementMarker
                     {:version 1 :state "retired" :flow flow :record expected
                      :native_thread native-thread :evidence evidence
                      :retired_by (or *flow-id* (System/getenv "FLOW_ID") "")
                      :retired_at (now)}
                     "RetirementMarker"))
            (store/remove-route! (root) flow)
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
          (save-route! (registry) flow replacement)
          (str "Rebound " flow ": " old-name " -> " new-name " (" session "/" pane-id "/" terminal-id ")"))))))
(defn move-route! [flow record pane-id hold?]
  (let [replacement (cond-> (assoc record :pane_id pane-id)
                      hold? (assoc :route_hold "pane_move_in_progress")
                      (not hold?) (dissoc :route_hold))]
    (save-route! (registry) flow replacement)
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
(defn validate-request! [request]
  (let [{:keys [context body] :as request} (request! request)
        values (cond-> [body] (some? context) (conj context))]
    (when (some #(or (str/blank? %) (re-find #"[\p{Cc}&&[^\n\t]]" %)) values)
      (fail "Message fields must be nonempty and contain no terminal control characters"))
    (when (some nested-relay? values)
      (fail "Nested complete #msg or #psyche form is not a message field"))
    request))
(defn send-abrupt-request! [flow request wait-presented]
  (flow-id! flow)
  (let [request (validate-request! request)]
  (let [sender (or *flow-id* (System/getenv "FLOW_ID") (fail "Set FLOW_ID to your own flow ID before sending"))]
    (with-reservation flow
      (fn []
        (assert-not-retired! flow)
        (let [route (read-route flow)]
          (when (needs-binding? route) (held! flow :NeedsBinding request route))
          (when (in-transition? route) (held! flow :InTransition request route))
          (when (:route_hold route) (held! flow :RouteHold request route))
          (let [live (try (verify-target! route) (catch Exception error (held! flow (held-reason error) request route)))
                keys (get abrupt-keys (:agent route))]
            (when-not keys (fail (str "Hard-abrupt is not supported for " (:agent route) "; nothing sent")))
            (let [envelope (message-envelope sender request)
                  submission (append-attempt! flow :Submitting :Uncertain route request envelope)]
              (try
                (doseq [key (:interrupt keys)] (send-keys* (transport) route key))
                (let [reply (prompt!* (transport) route envelope wait-presented)]
                  (when wait-presented (presented! route reply)))
                (doseq [key (:submit keys)] (send-keys* (transport) route key))
              ;; A successful prompt does not prove the terminal stayed bound.
              ;; Recheck before reporting any delivery grade.
                (verify-target! route)
                (try
                  (append-attempt! flow :sent (if wait-presented :Presented :Transported) route request envelope)
                  (str (if wait-presented "Presented" "Transported") ".{ " flow " " (or (:agent_status live) "unknown") " }")
                  (catch Exception error
                    (record-uncertain! flow route request envelope)
                    (throw (ex-info (str "Uncertain.{ " flow " attempt-" (subs (:id submission) 0 12) " } prompt was delivered but ledger confirmation failed; do not retry: " (.getMessage error))
                                    {:hm/failure true :hm/post-ledger true}))))
                (catch Exception error
                  (if (:hm/post-ledger (ex-data error))
                    (throw error)
                    (do (record-uncertain! flow route request envelope)
                        (fail (str "Uncertain.{ " flow " attempt-" (subs (:id submission) 0 12) " } Escape was sent; prompt failed or is uncertain: " (.getMessage error)))))))))))))))
(defn send-abrupt! [flow body wait-presented]
  (send-abrupt-request! flow {:variant :msg :body body} wait-presented))
(defn send-abrupt-psyche! [flow context verbatim wait-presented]
  (flow-id! flow)
  (let [request (validate-request! {:variant :psyche :context context :body verbatim})
        sender (or *flow-id* (System/getenv "FLOW_ID")
                   (fail "Set FLOW_ID to your own flow ID before sending"))]
    (with-reservation flow
      (fn []
        (let [requests (try
                         (psyche-requests sender context verbatim)
                         (catch Exception error
                           (if (:hm/failure (ex-data error))
                             (held! flow :RelayOverflow request nil)
                             (throw error))))]
          (binding [*with-reservation* (fn [_ f] (f))]
            (last (mapv #(send-abrupt-request! flow % wait-presented) requests))))))))
(defn record-sent! [flow grade route submission live request envelope]
  (try
    (append-attempt! flow :sent grade route request envelope)
    (str (name grade) ".{ " flow " " (or (:agent_status live) "unknown") " }")
    (catch Exception error
      (record-uncertain! flow route request envelope)
      (throw (ex-info (str "Uncertain.{ " flow " attempt-" (subs (:id submission) 0 12)
                           " } prompt was delivered but ledger confirmation failed; do not retry: " (.getMessage error))
                      {:hm/failure true :hm/post-ledger true})))))
(defn send-request!
  ([flow request wait-presented pane] (send-request! flow request wait-presented pane 10))
  ([flow request wait-presented pane hold-seconds]
   (flow-id! flow)
   (let [request (validate-request! request)]
   (let [sender (or *flow-id* (System/getenv "FLOW_ID") (fail "Set FLOW_ID to your own flow ID before sending"))]
     (with-reservation flow
       (fn []
         (assert-not-retired! flow)
         (let [stored (try (read-route flow) (catch Exception _ nil))
               stored (if (and (nil? stored) (pos? hold-seconds))
                        (do (Thread/sleep (long (* 1000 hold-seconds)))
                            (try (read-route flow) (catch Exception _ nil)))
                        stored)]
           (when (in-transition? stored) (held! flow :InTransition request stored))
           (when (needs-binding? stored) (held! flow :NeedsBinding request stored))
           (when (:route_hold stored) (held! flow :RouteHold request stored))
           (let [[route fallback?] (try (resolve-send-route flow stored pane)
                                        (catch Exception error
                                          (held! flow (cond (:hm/invalid (ex-data error)) :InvalidBinding
                                                            stored :PaneMissing
                                                            :else :NotRegistered)
                                                 request stored)))
                 live (try (verify-target! route) (catch Exception error (held! flow (held-reason error) request route)))
                 envelope (message-envelope sender request)
                 submission (try (append-attempt! flow :Submitting :Uncertain route request envelope)
                                 (catch Exception error
                                   (if (:hm/invalid (ex-data error))
                                     (held! flow :InvalidBinding request route)
                                     (throw error))))
                 grade (if fallback? :Fallback-Presented (if wait-presented :Presented :Transported))]
             (try
               (let [waited? (or fallback? wait-presented)
                     reply (prompt!* (transport) route envelope waited?)]
                 (when waited? (presented! route reply)))
               (verify-target! route)
               (record-sent! flow grade route submission live request envelope)
               (catch Exception error
                 (if (:hm/post-ledger (ex-data error))
                   (throw error)
                   (do (record-uncertain! flow route request envelope)
                       (fail (str "Uncertain.{ " flow " attempt-" (subs (:id submission) 0 12)
                                  " } prompt failed or is uncertain: " (.getMessage error)))))))))))))))
(defn send!
  ([flow body wait-presented pane] (send-request! flow {:variant :msg :body body} wait-presented pane))
  ([flow body wait-presented pane hold-seconds]
   (send-request! flow {:variant :msg :body body} wait-presented pane hold-seconds)))
(defn send-psyche!
  ([flow context verbatim wait-presented pane]
   (send-psyche! flow context verbatim wait-presented pane 10))
  ([flow context verbatim wait-presented pane hold-seconds]
   (flow-id! flow)
   (let [request (validate-request! {:variant :psyche :context context :body verbatim})
         sender (or *flow-id* (System/getenv "FLOW_ID")
                    (fail "Set FLOW_ID to your own flow ID before sending"))]
     (with-reservation flow
       (fn []
         (let [requests (try
                          (psyche-requests sender context verbatim)
                          (catch Exception error
                            (if (:hm/failure (ex-data error))
                              (held! flow :RelayOverflow request nil)
                              (throw error))))]
           (binding [*with-reservation* (fn [_ f] (f))]
             (last (mapv #(send-request! flow % wait-presented pane hold-seconds) requests)))))))))
(defn route-records []
  (into {} (remove (fn [[flow _]] (store/retirement-for (root) flow))
                   (store/routes (root)))))
(defn heartbeat-state! []
  (json/generate-string
   {:version 1
    :routes (mapv (fn [[flow route]] {:flow flow :route route})
                  (sort-by key (route-records)))
    :retirements (store/retirements (root))}))
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
