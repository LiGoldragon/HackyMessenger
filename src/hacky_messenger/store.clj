(ns hacky-messenger.store
  "The EDN files are the operational records. Datalevin is their durable
  Datalog index for registry, attempts, and pending-intent lookup."
  (:require [babashka.fs :as fs]
            [babashka.pods :as pods]))

(pods/load-pod 'huahaiy/datalevin "0.8.25")
(require 'pod.huahaiy.datalevin)

(defn- call [symbol & args]
  (apply (or (resolve symbol) (throw (ex-info (str "Datalevin pod lacks " symbol) {}))) args))

(def schema
  {:hm/identity {:db/unique :db.unique/identity}
   :hm/kind     {:db/index true}
   :hm/flow     {:db/index true}
   :hm/reason   {:db/index true}})

(defn database-path [root] (str (fs/path root "datalevin")))

(defn with-connection [root f]
  (let [conn (call 'pod.huahaiy.datalevin/get-conn (database-path root) schema)]
    (try (f conn)
         (finally (call 'pod.huahaiy.datalevin/close conn)))))

(defn index-route! [root flow route]
  (with-connection root
    #(call 'pod.huahaiy.datalevin/transact! %
           [(assoc route :hm/identity (str "route/" flow) :hm/kind :route :hm/flow flow)])))

(defn index-attempt! [root attempt]
  (with-connection root
    #(call 'pod.huahaiy.datalevin/transact! %
           [(assoc attempt :hm/identity (str "attempt/" (:id attempt))
                   :hm/kind :attempt :hm/flow (:flow attempt) :hm/reason (:reason attempt))])))

(defn index-pending! [root attempt message]
  (with-connection root
    #(call 'pod.huahaiy.datalevin/transact! %
           [{:hm/identity (str "pending/" (:id attempt)) :hm/kind :pending
             :hm/flow (:flow attempt) :hm/reason (:reason attempt) :hm/message message}])))

(defn attempts-for [root flow]
  (with-connection root
    #(call 'pod.huahaiy.datalevin/q
           '[:find ?identity ?reason
             :in $ ?flow
             :where [?e :hm/kind :attempt]
             [?e :hm/flow ?flow]
             [?e :hm/identity ?identity]
             [?e :hm/reason ?reason]]
           (call 'pod.huahaiy.datalevin/db %) flow)))

(defn pending-for [root flow]
  (with-connection root
    #(call 'pod.huahaiy.datalevin/q
           '[:find ?identity ?reason
             :in $ ?flow
             :where [?e :hm/kind :pending]
             [?e :hm/flow ?flow]
             [?e :hm/identity ?identity]
             [?e :hm/reason ?reason]]
           (call 'pod.huahaiy.datalevin/db %) flow)))

(defn routes-for [root]
  (with-connection root
    #(call 'pod.huahaiy.datalevin/q
           '[:find ?flow
             :where [?e :hm/kind :route]
             [?e :hm/flow ?flow]]
           (call 'pod.huahaiy.datalevin/db %))))
