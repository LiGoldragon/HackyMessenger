(ns hacky-messenger.typed-store
  "Typed Datalevin authority for the Clojure messenger. EDN is exposed only as
  an explicit import/export representation."
  (:require [babashka.fs :as fs]
            [babashka.pods :as pods]
            [clojure.edn :as edn]
            [malli.core :as m]))

(def pod-version "0.8.25")
(pods/load-pod 'huahaiy/datalevin pod-version)
(require 'pod.huahaiy.datalevin)

(defn- call [symbol & args]
  (apply (or (resolve symbol)
             (throw (ex-info (str "Datalevin pod lacks " symbol) {})))
         args))

(def ReadinessProof
  [:map {:closed true}
   [:thread_id :string] [:rollout :string] [:marker :string]
   [:evidence_kind {:optional true} :string]])
(def Route
  [:map {:closed true}
   [:session :string] [:name :string] [:pane_id :string] [:terminal_id :string]
   [:agent :string] [:native_thread {:optional true} :string]
   [:readiness_proof {:optional true} ReadinessProof]
   [:route_hold {:optional true} :string]
   [:transition {:optional true} :boolean]
   [:state :string]])
(def AttemptBinding
  [:map {:closed true}
   [:session {:optional true} :string] [:name {:optional true} :string]
   [:pane_id {:optional true} :string] [:terminal_id {:optional true} :string]
   [:agent {:optional true} :string] [:native_thread {:optional true} :string]
   [:readiness_proof {:optional true} ReadinessProof]
   [:route_hold {:optional true} :string] [:transition {:optional true} :boolean]
   [:state {:optional true} :string]])
(def Attempt
  [:map {:closed true}
   [:id :string] [:flow :string] [:at :string] [:grade :keyword] [:reason :keyword]
   [:body {:optional true} :string] [:binding {:optional true} AttemptBinding]])
(def Pending
  [:map {:closed true}
   [:attempt Attempt] [:message :string] [:state [:= "held"]]])
(def RouteIdentity
  [:map {:closed true}
   [:session :string] [:name :string] [:pane_id :string] [:terminal_id :string] [:agent :string]])
(def Evidence
  [:map {:closed true} [:path :string] [:sha256 :string]])
(def Retirement
  [:map {:closed true}
   [:version [:= 1]] [:state [:= "retired"]] [:flow :string]
   [:record RouteIdentity] [:native_thread :string] [:evidence Evidence]
   [:retired_by :string] [:retired_at :string]])

(def schema
  {:flow/id {:db/unique :db.unique/identity}
   :route/flow {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one
                :db/unique :db.unique/identity}
   :route/session {} :route/name {} :route/pane {} :route/terminal {}
   :route/agent {} :route/thread {} :route/hold {} :route/transition {} :route/state {}
   :route/readiness-thread {} :route/readiness-rollout {} :route/readiness-marker {}
   :route/readiness-kind {}
   :attempt/id {:db/unique :db.unique/identity}
   :attempt/flow {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   :attempt/at {} :attempt/grade {} :attempt/reason {} :attempt/body {}
   :attempt.binding/session {} :attempt.binding/name {} :attempt.binding/pane {}
   :attempt.binding/terminal {} :attempt.binding/agent {} :attempt.binding/thread {}
   :attempt.binding/hold {} :attempt.binding/transition {} :attempt.binding/state {}
   :attempt.binding/readiness-thread {} :attempt.binding/readiness-rollout {}
   :attempt.binding/readiness-marker {} :attempt.binding/readiness-kind {}
   :pending/id {:db/unique :db.unique/identity}
   :pending/attempt {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   :pending/message {} :pending/state {}
   :retirement/flow {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one
                     :db/unique :db.unique/identity}
   :retirement/version {} :retirement/state {} :retirement/session {}
   :retirement/name {} :retirement/pane {} :retirement/terminal {} :retirement/agent {}
   :retirement/thread {} :retirement/evidence-path {} :retirement/evidence-sha256 {}
   :retirement/retired-by {} :retirement/at {}})

(defn valid! [entity-schema value]
  (if (m/validate entity-schema value)
    value
    (throw (ex-info "Invalid typed store entity"
                    {:value value :explanation (m/explain entity-schema value)}))))

(defn route! [route]
  (valid! Route
          (assoc route :state
                 (or (:state route)
                     (if (:native_thread route) "Bound" "NeedsBinding")))))
(defn attempt! [attempt] (valid! Attempt attempt))
(defn pending! [pending] (valid! Pending (update pending :attempt attempt!)))
(defn retirement! [retirement] (valid! Retirement retirement))

(defn database-path [root] (str (fs/path root "typed-datalevin")))
(defn with-db [root f]
  (let [conn (call 'pod.huahaiy.datalevin/get-conn (database-path root) schema)]
    (try (f conn) (finally (call 'pod.huahaiy.datalevin/close conn)))))
(defn transact! [root tx] (with-db root #(call 'pod.huahaiy.datalevin/transact! % tx)))
(defn query [root form & inputs]
  (with-db root
    #(apply call 'pod.huahaiy.datalevin/q
            form (call 'pod.huahaiy.datalevin/db %) inputs)))

(defn checked-rows! [rows width]
  (when-not (and (or (set? rows) (sequential? rows))
                 (every? #(and (vector? %) (= width (count %))) rows))
    (throw (ex-info "Malformed typed Datalevin query row" {:rows rows :width width})))
  rows)
(defn- one! [rows kind identity]
  (let [rows (checked-rows! rows 1)]
    (when (> (count rows) 1)
      (throw (ex-info (str "Typed " kind " is not unique") {:identity identity :rows rows})))
    (ffirst rows)))
(defn- assoc-present [m k v] (if (nil? v) m (assoc m k v)))

(defn- route-attrs [prefix route]
  (let [route (route! route)
        proof (:readiness_proof route)]
    (-> {}
        (assoc (keyword prefix "session") (:session route)
               (keyword prefix "name") (:name route)
               (keyword prefix "pane") (:pane_id route)
               (keyword prefix "terminal") (:terminal_id route)
               (keyword prefix "agent") (:agent route)
               (keyword prefix "state") (:state route))
        (assoc-present (keyword prefix "thread") (:native_thread route))
        (assoc-present (keyword prefix "hold") (:route_hold route))
        (assoc-present (keyword prefix "transition") (:transition route))
        (assoc-present (keyword prefix "readiness-thread") (:thread_id proof))
        (assoc-present (keyword prefix "readiness-rollout") (:rollout proof))
        (assoc-present (keyword prefix "readiness-marker") (:marker proof))
        (assoc-present (keyword prefix "readiness-kind") (:evidence_kind proof)))))

(defn- attrs-route! [prefix entity]
  (let [getv #(get entity (keyword prefix %))
        proof (when-let [thread (getv "readiness-thread")]
                (cond-> {:thread_id thread
                         :rollout (getv "readiness-rollout")
                         :marker (getv "readiness-marker")}
                  (getv "readiness-kind") (assoc :evidence_kind (getv "readiness-kind"))))]
    (route!
     (cond-> {:session (getv "session") :name (getv "name")
              :pane_id (getv "pane") :terminal_id (getv "terminal")
              :agent (getv "agent") :state (getv "state")}
       (getv "thread") (assoc :native_thread (getv "thread"))
       (getv "hold") (assoc :route_hold (getv "hold"))
       (some? (getv "transition")) (assoc :transition (getv "transition"))
       proof (assoc :readiness_proof proof)))))

(defn- binding-attrs [binding]
  (let [proof (:readiness_proof binding)]
    (-> {}
        (assoc-present :attempt.binding/session (:session binding))
        (assoc-present :attempt.binding/name (:name binding))
        (assoc-present :attempt.binding/pane (:pane_id binding))
        (assoc-present :attempt.binding/terminal (:terminal_id binding))
        (assoc-present :attempt.binding/agent (:agent binding))
        (assoc-present :attempt.binding/thread (:native_thread binding))
        (assoc-present :attempt.binding/hold (:route_hold binding))
        (assoc-present :attempt.binding/transition (:transition binding))
        (assoc-present :attempt.binding/state (:state binding))
        (assoc-present :attempt.binding/readiness-thread (:thread_id proof))
        (assoc-present :attempt.binding/readiness-rollout (:rollout proof))
        (assoc-present :attempt.binding/readiness-marker (:marker proof))
        (assoc-present :attempt.binding/readiness-kind (:evidence_kind proof)))))
(defn- attrs-binding! [entity]
  (let [proof (when-let [thread (:attempt.binding/readiness-thread entity)]
                (cond-> {:thread_id thread
                         :rollout (:attempt.binding/readiness-rollout entity)
                         :marker (:attempt.binding/readiness-marker entity)}
                  (:attempt.binding/readiness-kind entity)
                  (assoc :evidence_kind (:attempt.binding/readiness-kind entity))))]
    (valid! AttemptBinding
            (cond-> {}
              (:attempt.binding/session entity) (assoc :session (:attempt.binding/session entity))
              (:attempt.binding/name entity) (assoc :name (:attempt.binding/name entity))
              (:attempt.binding/pane entity) (assoc :pane_id (:attempt.binding/pane entity))
              (:attempt.binding/terminal entity) (assoc :terminal_id (:attempt.binding/terminal entity))
              (:attempt.binding/agent entity) (assoc :agent (:attempt.binding/agent entity))
              (:attempt.binding/thread entity) (assoc :native_thread (:attempt.binding/thread entity))
              (:attempt.binding/hold entity) (assoc :route_hold (:attempt.binding/hold entity))
              (some? (:attempt.binding/transition entity))
              (assoc :transition (:attempt.binding/transition entity))
              (:attempt.binding/state entity) (assoc :state (:attempt.binding/state entity))
              proof (assoc :readiness_proof proof)))))

(def route-pull
  [:route/session :route/name :route/pane :route/terminal :route/agent :route/thread
   :route/hold :route/transition :route/state :route/readiness-thread
   :route/readiness-rollout :route/readiness-marker :route/readiness-kind
   {:route/flow [:flow/id]}])
(def attempt-pull
  [:attempt/id :attempt/at :attempt/grade :attempt/reason :attempt/body
   :attempt.binding/session :attempt.binding/name :attempt.binding/pane
   :attempt.binding/terminal :attempt.binding/agent :attempt.binding/thread
   :attempt.binding/hold :attempt.binding/transition :attempt.binding/state
   :attempt.binding/readiness-thread :attempt.binding/readiness-rollout
   :attempt.binding/readiness-marker :attempt.binding/readiness-kind
   {:attempt/flow [:flow/id]}])
(def pending-pull
  [:pending/message :pending/state {:pending/attempt attempt-pull}])
(def retirement-pull
  [:retirement/version :retirement/state :retirement/session :retirement/name
   :retirement/pane :retirement/terminal :retirement/agent :retirement/thread
   :retirement/evidence-path :retirement/evidence-sha256
   :retirement/retired-by :retirement/at {:retirement/flow [:flow/id]}])

(defn put-route! [root flow route]
  (let [route (route! route)
        current (some-> (query root '[:find (pull ?route [*])
                                      :in $ ?flow
                                      :where [?f :flow/id ?flow] [?route :route/flow ?f]]
                               flow)
                        (one! "route" flow))
        entity-id (:db/id current)
        replacement (cond-> (assoc (route-attrs "route" route)
                                   :route/flow [:flow/id flow])
                      entity-id (assoc :db/id entity-id))
        retractions (for [[attribute value] current
                          :when (and (keyword? attribute)
                                     (= "route" (namespace attribute))
                                     (not= attribute :route/flow)
                                     (not (contains? replacement attribute)))]
                      [:db/retract entity-id attribute value])]
    (transact! root (vec (concat retractions [{:flow/id flow} replacement])))
    route))
(defn- pulled-route! [entity]
  (when-not (string? (get-in entity [:route/flow :flow/id]))
    (throw (ex-info "Malformed route flow reference" {:entity entity})))
  (attrs-route! "route" entity))
(defn stored-route-for [root flow]
  (some-> (query root '[:find (pull ?route ?pattern)
                        :in $ ?flow ?pattern
                        :where [?f :flow/id ?flow] [?route :route/flow ?f]]
                 flow route-pull)
          (one! "route" flow) pulled-route!))

(declare retirement-for)
(defn route-for [root flow]
  (when-not (retirement-for root flow) (stored-route-for root flow)))
(defn routes [root]
  (let [rows (checked-rows!
              (query root '[:find ?flow (pull ?route ?pattern)
                            :in $ ?pattern
                            :where [?f :flow/id ?flow] [?route :route/flow ?f]]
                     route-pull)
              2)]
    (into {} (map (fn [[flow entity]] [flow (pulled-route! entity)]) rows))))
(defn remove-route! [root flow]
  (when-let [entity-id (query root '[:find ?route . :in $ ?flow
                                     :where [?f :flow/id ?flow] [?route :route/flow ?f]]
                              flow)]
    (transact! root [[:db/retractEntity entity-id]]))
  nil)

(defn- attempt-tx [attempt]
  (let [attempt (attempt! attempt)]
    [{:flow/id (:flow attempt)}
     (cond-> {:attempt/id (:id attempt) :attempt/flow [:flow/id (:flow attempt)]
              :attempt/at (:at attempt) :attempt/grade (:grade attempt)
              :attempt/reason (:reason attempt)}
       (:body attempt) (assoc :attempt/body (:body attempt))
       (:binding attempt) (merge (binding-attrs (:binding attempt))))]))
(defn put-attempt! [root attempt]
  (let [attempt (attempt! attempt)] (transact! root (attempt-tx attempt)) attempt))
(defn- pulled-attempt! [entity]
  (let [flow (get-in entity [:attempt/flow :flow/id])]
    (when-not (string? flow)
      (throw (ex-info "Malformed attempt flow reference" {:entity entity})))
    (attempt!
     (cond-> {:id (:attempt/id entity) :flow flow :at (:attempt/at entity)
              :grade (:attempt/grade entity) :reason (:attempt/reason entity)}
       (:attempt/body entity) (assoc :body (:attempt/body entity))
       (some #(contains? entity %)
             [:attempt.binding/session :attempt.binding/name :attempt.binding/pane
              :attempt.binding/terminal :attempt.binding/agent :attempt.binding/thread])
       (assoc :binding (attrs-binding! entity))))))
(defn attempt-by-id [root id]
  (some-> (query root '[:find (pull ?attempt ?pattern)
                        :in $ ?id ?pattern :where [?attempt :attempt/id ?id]]
                 id attempt-pull)
          (one! "attempt" id) pulled-attempt!))
(defn attempts-for [root flow]
  (->> (checked-rows!
        (query root '[:find (pull ?attempt ?pattern)
                      :in $ ?flow ?pattern
                      :where [?f :flow/id ?flow] [?attempt :attempt/flow ?f]]
               flow attempt-pull)
        1)
       (map (comp pulled-attempt! first)) (sort-by :id) vec))

(defn put-pending! [root pending]
  (let [pending (pending! pending) attempt (:attempt pending)]
    (when-not (attempt-by-id root (:id attempt))
      (throw (ex-info "Pending intent requires a persisted attempt" {:attempt (:id attempt)})))
    (transact! root [{:pending/id (:id attempt)
                      :pending/attempt [:attempt/id (:id attempt)]
                      :pending/message (:message pending) :pending/state (:state pending)}])
    pending))
(defn- pulled-pending! [entity]
  (when-not (map? (:pending/attempt entity))
    (throw (ex-info "Malformed pending attempt reference" {:entity entity})))
  (pending! {:attempt (pulled-attempt! (:pending/attempt entity))
             :message (:pending/message entity) :state (:pending/state entity)}))
(defn pending-by-id [root id]
  (some-> (query root '[:find (pull ?pending ?pattern)
                        :in $ ?id ?pattern :where [?pending :pending/id ?id]]
                 id pending-pull)
          (one! "pending intent" id) pulled-pending!))
(defn pending-for [root flow]
  (->> (checked-rows!
        (query root '[:find (pull ?pending ?pattern)
                      :in $ ?flow ?pattern
                      :where [?f :flow/id ?flow] [?attempt :attempt/flow ?f]
                             [?pending :pending/attempt ?attempt]]
               flow pending-pull)
        1)
       (map (comp pulled-pending! first)) (sort-by #(get-in % [:attempt :id])) vec))

(defn put-retirement! [root retirement]
  (let [retirement (retirement! retirement) record (:record retirement)
        evidence (:evidence retirement)]
    (transact! root
               [{:flow/id (:flow retirement)}
                {:retirement/flow [:flow/id (:flow retirement)]
                 :retirement/version (:version retirement) :retirement/state (:state retirement)
                 :retirement/session (:session record) :retirement/name (:name record)
                 :retirement/pane (:pane_id record) :retirement/terminal (:terminal_id record)
                 :retirement/agent (:agent record) :retirement/thread (:native_thread retirement)
                 :retirement/evidence-path (:path evidence)
                 :retirement/evidence-sha256 (:sha256 evidence)
                 :retirement/retired-by (:retired_by retirement)
                 :retirement/at (:retired_at retirement)}])
    retirement))
(defn- pulled-retirement! [entity]
  (let [flow (get-in entity [:retirement/flow :flow/id])]
    (when-not (string? flow)
      (throw (ex-info "Malformed retirement flow reference" {:entity entity})))
    (retirement!
     {:version (:retirement/version entity) :state (:retirement/state entity) :flow flow
      :record {:session (:retirement/session entity) :name (:retirement/name entity)
               :pane_id (:retirement/pane entity) :terminal_id (:retirement/terminal entity)
               :agent (:retirement/agent entity)}
      :native_thread (:retirement/thread entity)
      :evidence {:path (:retirement/evidence-path entity)
                 :sha256 (:retirement/evidence-sha256 entity)}
      :retired_by (:retirement/retired-by entity) :retired_at (:retirement/at entity)})))
(defn retirement-for [root flow]
  (some-> (query root '[:find (pull ?retirement ?pattern)
                        :in $ ?flow ?pattern
                        :where [?f :flow/id ?flow] [?retirement :retirement/flow ?f]]
                 flow retirement-pull)
          (one! "retirement" flow) pulled-retirement!))
(defn retirements [root]
  (->> (checked-rows!
        (query root '[:find (pull ?retirement ?pattern)
                      :in $ ?pattern :where [?retirement :retirement/flow]]
               retirement-pull)
        1)
       (map (comp pulled-retirement! first)) (sort-by :flow) vec))

(defn- boundary-entity! [entity]
  (when-not (map? entity)
    (throw (ex-info "Typed boundary entity must be a map" {:value entity})))
  (case (:entity/type entity)
    :route (do
             (when-not (string? (:entity/flow entity))
               (throw (ex-info "Route export requires its Flow ID" {:value entity})))
             (assoc entity :entity/value (route! (:entity/value entity))))
    :attempt (assoc entity :entity/value (attempt! (:entity/value entity)))
    :pending (assoc entity :entity/value (pending! (:entity/value entity)))
    :retirement (assoc entity :entity/value (retirement! (:entity/value entity)))
    (throw (ex-info "Unknown typed boundary entity" {:value entity}))))
(defn export-edn [entities]
  (when-not (sequential? entities)
    (throw (ex-info "Typed export must be a sequence" {:value entities})))
  (pr-str (mapv boundary-entity! entities)))
(defn import-edn [text]
  (when-not (string? text)
    (throw (ex-info "Typed import must be EDN text" {:value text})))
  (let [entities (edn/read-string text)]
    (when-not (sequential? entities)
      (throw (ex-info "Typed import must contain a sequential EDN value" {:value entities})))
    (mapv boundary-entity! entities)))
