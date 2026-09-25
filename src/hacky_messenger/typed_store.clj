(ns hacky-messenger.typed-store
  "Experimental, unused typed Datalevin authority API. Core commands remain on
  EDN until their readers and writers switch together in a later cutover."
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

(def schema
  {:flow/id {:db/unique :db.unique/identity}
   :attempt/id {:db/unique :db.unique/identity}
   :pending/id {:db/unique :db.unique/identity}
   :route/flow {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one
                :db/unique :db.unique/identity}
   :attempt/flow {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   :pending/attempt {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   :retirement/flow {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one
                     :db/unique :db.unique/identity}
   :route/session {:db/index true}
   :route/pane {:db/index true}
   :route/terminal {:db/index true}
   :route/agent {:db/index true}
   :route/thread {:db/index true}
   :route/hold {:db/index true}
   :route/state {:db/index true}
   :attempt/at {:db/index true}
   :attempt/grade {:db/index true}
   :attempt/reason {:db/index true}
   :pending/state {:db/index true}
   :retirement/evidence {:db/index true}
   :retirement/at {:db/index true}
   :retirement/retired-by {:db/index true}})

(def Route
  [:map {:closed true}
   [:flow/id :string]
   [:route/session :string]
   [:route/pane :string]
   [:route/terminal :string]
   [:route/agent :string]
   [:route/thread {:optional true} :string]
   [:route/hold {:optional true} :string]
   [:route/state :string]])

(def Attempt
  [:map {:closed true}
   [:attempt/id :string]
   [:attempt/flow :string]
   [:attempt/at :string]
   [:attempt/grade :keyword]
   [:attempt/reason :keyword]
   [:attempt/body {:optional true} :string]])

(def Pending
  [:map {:closed true}
   [:pending/id :string]
   [:pending/attempt Attempt]
   [:pending/state :string]])

(def Retirement
  [:map {:closed true}
   [:retirement/flow :string]
   [:retirement/evidence :string]
   [:retirement/at :string]
   [:retirement/retired-by :string]])

(defn valid! [entity-schema value]
  (if (m/validate entity-schema value)
    value
    (throw (ex-info "Invalid typed store entity"
                    {:value value :explanation (m/explain entity-schema value)}))))

(defn route! [route]
  (valid! Route (assoc route :route/state
                       (or (:route/state route)
                           (if (:route/thread route) "Bound" "NeedsBinding")))))
(defn attempt! [attempt] (valid! Attempt attempt))
(defn pending! [pending] (valid! Pending pending))
(defn retirement! [retirement] (valid! Retirement retirement))

(defn- entity! [entity]
  (cond
    (contains? entity :flow/id) (route! entity)
    (contains? entity :attempt/id) (attempt! entity)
    (contains? entity :pending/id) (pending! entity)
    (contains? entity :retirement/flow) (retirement! entity)
    :else (throw (ex-info "Unknown typed store entity" {:value entity}))))

(defn route-tx [route]
  (let [route (route! route)
        flow (:flow/id route)]
    [{:flow/id flow}
     (assoc (dissoc route :flow/id) :route/flow [:flow/id flow])]))

(defn attempt-tx [attempt]
  (let [attempt (attempt! attempt)]
    [{:flow/id (:attempt/flow attempt)}
     (assoc (dissoc attempt :attempt/flow)
            :attempt/flow [:flow/id (:attempt/flow attempt)])]))

(defn retirement-tx [retirement]
  (let [retirement (retirement! retirement)]
    [{:flow/id (:retirement/flow retirement)}
     (assoc (dissoc retirement :retirement/flow)
            :retirement/flow [:flow/id (:retirement/flow retirement)])]))

(defn export-edn [entities]
  (when-not (sequential? entities)
    (throw (ex-info "Typed export must be a sequence" {:value entities})))
  (pr-str (mapv entity! entities)))

(defn import-edn [text]
  (when-not (string? text)
    (throw (ex-info "Typed import must be EDN text" {:value text})))
  (let [entities (edn/read-string text)]
    (when-not (sequential? entities)
      (throw (ex-info "Typed import must contain a sequential EDN value" {:value entities})))
    (mapv entity! entities)))

(defn checked-rows! [rows width]
  (when-not (and (or (set? rows) (sequential? rows))
                 (every? #(and (vector? %) (= width (count %))) rows))
    (throw (ex-info "Malformed typed Datalevin query row"
                    {:rows rows :width width})))
  rows)

(defn retirement-precedes? [retirements flow]
  (boolean (some #(= flow (:retirement/flow %)) retirements)))

(defn database-path [root]
  (str (fs/path root "typed-datalevin")))

(defn with-db [root f]
  (let [conn (call 'pod.huahaiy.datalevin/get-conn (database-path root) schema)]
    (try
      (f conn)
      (finally (call 'pod.huahaiy.datalevin/close conn)))))

(defn transact! [root tx]
  (with-db root #(call 'pod.huahaiy.datalevin/transact! % tx)))

(defn put-route! [root route]
  (let [route (route! route)]
    (transact! root (route-tx route))
    route))

(defn put-attempt! [root attempt]
  (let [attempt (attempt! attempt)]
    (transact! root (attempt-tx attempt))
    attempt))

(defn put-pending! [root attempt-id state]
  (when-not (and (string? attempt-id) (string? state))
    (throw (ex-info "Invalid pending link" {:attempt-id attempt-id :state state})))
  (transact! root [{:pending/id attempt-id
                    :pending/attempt [:attempt/id attempt-id]
                    :pending/state state}]))

(defn put-retirement! [root retirement]
  (let [retirement (retirement! retirement)]
    (transact! root (retirement-tx retirement))
    retirement))

(defn query [root form & inputs]
  (with-db root
    #(apply call 'pod.huahaiy.datalevin/q
            form (call 'pod.huahaiy.datalevin/db %) inputs)))

(def route-pull
  [:route/session :route/pane :route/terminal :route/agent
   :route/thread :route/hold :route/state {:route/flow [:flow/id]}])
(def attempt-pull
  [:attempt/id :attempt/at :attempt/grade :attempt/reason :attempt/body
   {:attempt/flow [:flow/id]}])
(def pending-pull
  [:pending/id :pending/state {:pending/attempt attempt-pull}])
(def retirement-pull
  [:retirement/evidence :retirement/at :retirement/retired-by
   {:retirement/flow [:flow/id]}])

(defn- exactly-one-or-nil! [rows kind identity]
  (let [rows (checked-rows! rows 1)]
    (when (> (count rows) 1)
      (throw (ex-info (str "Typed " kind " is not unique")
                      {:identity identity :rows rows})))
    (ffirst rows)))

(defn- resolved-flow! [entity ref-key]
  (let [flow (get-in entity [ref-key :flow/id])]
    (when-not (string? flow)
      (throw (ex-info "Malformed typed Datalevin reference"
                      {:entity entity :reference ref-key})))
    [flow (dissoc entity ref-key)]))

(defn- resolve-route! [entity]
  (let [[flow route] (resolved-flow! entity :route/flow)]
    (route! (assoc route :flow/id flow))))

(defn- resolve-attempt! [entity]
  (let [[flow attempt] (resolved-flow! entity :attempt/flow)]
    (attempt! (assoc attempt :attempt/flow flow))))

(defn- resolve-pending! [entity]
  (when-not (map? (:pending/attempt entity))
    (throw (ex-info "Malformed typed Datalevin reference"
                    {:entity entity :reference :pending/attempt})))
  (pending! (update entity :pending/attempt resolve-attempt!)))

(defn- resolve-retirement! [entity]
  (let [[flow retirement] (resolved-flow! entity :retirement/flow)]
    (retirement! (assoc retirement :retirement/flow flow))))

(defn retirement-for [root flow]
  (some-> (query root '[:find (pull ?retirement ?pattern)
                        :in $ ?flow ?pattern
                        :where [?f :flow/id ?flow]
                               [?retirement :retirement/flow ?f]]
                 flow retirement-pull)
          (exactly-one-or-nil! "retirement" flow)
          resolve-retirement!))

(defn route-for [root flow]
  ;; A retirement is authoritative even if an older route entity remains.
  (when-not (retirement-for root flow)
    (some-> (query root '[:find (pull ?route ?pattern)
                          :in $ ?flow ?pattern
                          :where [?f :flow/id ?flow]
                                 [?route :route/flow ?f]]
                   flow route-pull)
            (exactly-one-or-nil! "route" flow)
            resolve-route!)))

(defn attempt-by-id [root attempt-id]
  (some-> (query root '[:find (pull ?attempt ?pattern)
                        :in $ ?attempt-id ?pattern
                        :where [?attempt :attempt/id ?attempt-id]]
                 attempt-id attempt-pull)
          (exactly-one-or-nil! "attempt" attempt-id)
          resolve-attempt!))

(defn attempts-for [root flow]
  (let [rows (checked-rows!
              (query root '[:find (pull ?attempt ?pattern)
                            :in $ ?flow ?pattern
                            :where [?f :flow/id ?flow]
                                   [?attempt :attempt/flow ?f]]
                     flow attempt-pull)
              1)]
    (->> rows (map (comp resolve-attempt! first)) (sort-by :attempt/id) vec)))

(defn pending-by-id [root pending-id]
  (some-> (query root '[:find (pull ?pending ?pattern)
                        :in $ ?pending-id ?pattern
                        :where [?pending :pending/id ?pending-id]]
                 pending-id pending-pull)
          (exactly-one-or-nil! "pending intent" pending-id)
          resolve-pending!))

(defn pending-for [root flow]
  (let [rows (checked-rows!
              (query root '[:find (pull ?pending ?pattern)
                            :in $ ?flow ?pattern
                            :where [?f :flow/id ?flow]
                                   [?attempt :attempt/flow ?f]
                                   [?pending :pending/attempt ?attempt]]
                     flow pending-pull)
              1)]
    (->> rows (map (comp resolve-pending! first)) (sort-by :pending/id) vec)))

(defn retirements-for [root flow]
  (if-let [retirement (retirement-for root flow)] [retirement] []))
