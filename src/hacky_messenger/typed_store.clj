(ns hacky-messenger.typed-store
  "Experimental, unused typed Datalevin authority API. Core commands remain on
  EDN until their readers and writers switch together in a later cutover."
  (:require [malli.core :as m]))

(def schema
  {:flow/id {:db/unique :db.unique/identity}
   :route/flow {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   :attempt/flow {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   :pending/attempt {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   :retirement/flow {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   :route/session {:db/index true} :route/pane {:db/index true}
   :route/terminal {:db/index true} :route/agent {:db/index true}
   :route/thread {:db/index true} :route/hold {:db/index true} :route/state {:db/index true}
   :attempt/at {:db/index true} :attempt/grade {:db/index true} :attempt/reason {:db/index true}
   :retirement/evidence {:db/index true} :retirement/at {:db/index true}
   :retirement/retired-by {:db/index true}})

(def Route [:map {:closed true} [:flow/id :string] [:route/session :string] [:route/pane :string]
            [:route/terminal :string] [:route/agent :string] [:route/thread {:optional true} :string]
            [:route/hold {:optional true} :string] [:route/state :string]])
(def Attempt [:map {:closed true} [:attempt/id :string] [:attempt/flow :string] [:attempt/at :string]
              [:attempt/grade :keyword] [:attempt/reason :keyword] [:attempt/body {:optional true} :string]])
(def Retirement [:map {:closed true} [:retirement/flow :string] [:retirement/evidence :string]
                 [:retirement/at :string] [:retirement/retired-by :string]])
(defn valid! [schema value] (if (m/validate schema value) value (throw (ex-info "Invalid typed store entity" {:value value}))))
(defn route! [route]
  (valid! Route (assoc route :route/state (or (:route/state route) (if (:route/thread route) "Bound" "NeedsBinding")))))
(defn attempt! [attempt] (valid! Attempt attempt))
(defn retirement! [retirement] (valid! Retirement retirement))
(defn route-tx [route]
  (let [route (route! route) flow (:flow/id route)]
    [{:flow/id flow} (assoc (dissoc route :flow/id) :route/flow [:flow/id flow])]))
(defn attempt-tx [attempt]
  (let [attempt (attempt! attempt)] [(assoc (dissoc attempt :attempt/flow) :attempt/flow [:flow/id (:attempt/flow attempt)])]))
(defn retirement-tx [retirement]
  (let [retirement (retirement! retirement)] [(assoc (dissoc retirement :retirement/flow) :retirement/flow [:flow/id (:retirement/flow retirement)])]))
(defn export-edn [entities] (pr-str entities))
(defn import-edn [value] (when-not (sequential? value) (throw (ex-info "Typed import must be sequential EDN" {}))) value)
