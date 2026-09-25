(ns hacky-messenger.main
  (:require [hacky-messenger.core :as hm]))
(defn usage [] (str "Usage: hm-clj <send|send-abrupt|register|deregister|rebind|move|retire|import-retirement|list> ...\n" hm/skill-note))
(defn arg [xs option] (second (drop-while #(not= option %) xs)))
(defn parse-error [message] (throw (ex-info message {:hm/parse true})))
(def value-options #{"--session" "--native-thread" "--readiness-probe" "--rollout" "--old-name" "--pane-id" "--terminal-id" "--name" "--agent" "--process-pid" "--evidence" "--evidence-sha256" "--hold-seconds" "--pane"})
(defn expand-equals [xs]
  (mapcat #(if-let [[_ option value] (re-matches #"(--[^=]+)=(.*)" %)] [option value] [%]) xs))
(defn normalize-options [xs]
  (let [xs (vec (expand-equals xs))]
    (loop [remaining xs positional [] options []]
      (if-let [value (first remaining)]
        (cond
          (= value "--wait-presented") (recur (next remaining) positional (conj options value))
          (contains? value-options value) (if-let [argument (second remaining)]
                                            (recur (nnext remaining) positional (into options [value argument]))
                                            (parse-error (str "argument " value ": expected one argument")))
          (.startsWith value "--") (recur (next remaining) positional (conj options value))
          :else (recur (next remaining) (conj positional value) options))
        (into positional options)))))
(defn extra-values! [xs allowed]
  (loop [remaining xs]
    (when-let [value (first remaining)]
      (cond
        (= value "--wait-presented") (recur (next remaining))
        (contains? value-options value) (recur (nnext remaining))
        (contains? allowed value) (recur (next remaining))
        :else (parse-error (str "unrecognized arguments: " value))))))
(defn unknown-flags! [xs allowed]
  (doseq [value xs :when (and (.startsWith value "--") (not (contains? allowed value)))]
    (parse-error (str "unrecognized arguments: " value))))
(defn -main [& argv]
  (try
    (let [[op & raw-xs] argv
          xs (normalize-options raw-xs)]
      (if (or (= op "--help") (= op "-h") (some #{"--help" "-h"} xs))
        (println (usage))
        (case op
          "send" (let [[flow body & rest] xs]
                   (when-not (and flow body) (parse-error "the following arguments are required: flow, message"))
                   (unknown-flags! rest #{"--wait-presented" "--hold-seconds" "--pane"})
                   (extra-values! rest #{"--wait-presented" "--hold-seconds" "--pane"})
                   (let [hold (or (arg rest "--hold-seconds") "10")]
                     (when-not (try (<= 0 (Double/parseDouble hold) 60) (catch Exception _ false))
                       (parse-error "argument --hold-seconds: invalid float value")))
                   (println (hm/send! flow body (boolean (some #{"--wait-presented"} rest)) (arg rest "--pane"))))
          "send-abrupt" (let [[flow body & rest] xs]
                          (when-not (and flow body) (parse-error "the following arguments are required: flow, message"))
                          (unknown-flags! rest #{"--wait-presented" "--hold-seconds"})
                          (extra-values! rest #{"--wait-presented" "--hold-seconds"})
                          (let [hold (or (arg rest "--hold-seconds") "10")]
                            (when-not (try (<= 0 (Double/parseDouble hold) 60) (catch Exception _ false))
                              (parse-error "argument --hold-seconds: invalid float value")))
                          (println (hm/send-abrupt! flow body (boolean (some #{"--wait-presented"} rest)))))
          "register" (let [[flow name & rest] xs session (arg rest "--session") thread (arg rest "--native-thread")
                           marker (arg rest "--readiness-probe") rollout (arg rest "--rollout")]
                       (when-not (and flow name) (parse-error "the following arguments are required: flow, name"))
                       (unknown-flags! rest #{"--session" "--native-thread" "--readiness-probe" "--rollout"})
                       (extra-values! rest #{"--session" "--native-thread" "--readiness-probe" "--rollout"})
                       (println (hm/register! flow name session thread marker rollout)))
          "deregister" (let [[flow & rest] xs]
                         (when-not flow (parse-error "the following arguments are required: flow"))
                         (unknown-flags! rest #{"--session" "--pane-id" "--terminal-id" "--name"})
                         (extra-values! rest #{"--session" "--pane-id" "--terminal-id" "--name"})
                         (let [session (arg rest "--session") pane-id (arg rest "--pane-id") terminal-id (arg rest "--terminal-id") name (arg rest "--name")]
                           (when-not (every? some? [session pane-id terminal-id name])
                             (parse-error "the following arguments are required: --session, --pane-id, --terminal-id, --name"))
                           (println (hm/deregister! flow session pane-id terminal-id name))))
          "rebind" (let [[flow new-name & rest] xs]
                     (when-not (and flow new-name) (parse-error "the following arguments are required: flow, new_name"))
                     (unknown-flags! rest #{"--old-name" "--session" "--pane-id" "--terminal-id" "--agent" "--native-thread"})
                     (extra-values! rest #{"--old-name" "--session" "--pane-id" "--terminal-id" "--agent" "--native-thread"})
                     (let [old-name (arg rest "--old-name") session (arg rest "--session") pane-id (arg rest "--pane-id")
                           terminal-id (arg rest "--terminal-id") agent (arg rest "--agent") native-thread (arg rest "--native-thread")]
                       (when-not (every? some? [old-name session pane-id terminal-id agent native-thread])
                         (parse-error "the following arguments are required: --old-name, --session, --pane-id, --terminal-id, --agent, --native-thread"))
                       (println (hm/rebind! flow old-name new-name session pane-id terminal-id agent native-thread))))
          "move" (let [[flow workspace & rest] xs]
                   (when-not (and flow workspace) (parse-error "the following arguments are required: flow, workspace"))
                   (unknown-flags! rest #{"--session" "--pane-id" "--terminal-id" "--name" "--agent" "--native-thread" "--process-pid"})
                   (extra-values! rest #{"--session" "--pane-id" "--terminal-id" "--name" "--agent" "--native-thread" "--process-pid"})
                   (let [session (arg rest "--session") pane-id (arg rest "--pane-id") terminal-id (arg rest "--terminal-id")
                         name (arg rest "--name") agent (arg rest "--agent") native-thread (arg rest "--native-thread") pid-text (arg rest "--process-pid")]
                     (when-not (every? some? [session pane-id terminal-id name agent native-thread pid-text])
                       (parse-error "the following arguments are required: --session, --pane-id, --terminal-id, --name, --agent, --native-thread, --process-pid"))
                     (let [pid (or (try (parse-long pid-text) (catch Exception _ nil))
                                   (parse-error "argument --process-pid: invalid int value"))]
                       (println (hm/move! flow session pane-id terminal-id name agent native-thread pid workspace)))))
          ("retire" "import-retirement") (let [[flow & rest] xs]
                                           (when-not flow (parse-error "the following arguments are required: flow"))
                                           (unknown-flags! rest #{"--session" "--pane-id" "--terminal-id" "--name" "--agent" "--native-thread" "--evidence" "--evidence-sha256"})
                                           (extra-values! rest #{"--session" "--pane-id" "--terminal-id" "--name" "--agent" "--native-thread" "--evidence" "--evidence-sha256"})
                                           (let [session (arg rest "--session") pane-id (arg rest "--pane-id") terminal-id (arg rest "--terminal-id") name (arg rest "--name")
                                                 agent (arg rest "--agent") native-thread (arg rest "--native-thread") evidence (arg rest "--evidence") digest (arg rest "--evidence-sha256")]
                                             (when-not (every? some? [session pane-id terminal-id name agent native-thread evidence digest])
                                               (parse-error "the following arguments are required: --session, --pane-id, --terminal-id, --name, --agent, --native-thread, --evidence, --evidence-sha256"))
                                             (println (hm/retire! flow session pane-id terminal-id name agent native-thread evidence digest (= op "import-retirement")))))
          "list" (do (when (seq xs) (parse-error "unrecognized arguments")) (println (hm/listing!)))
          (parse-error (str "invalid choice: " op)))))
    (catch clojure.lang.ExceptionInfo e
      (binding [*out* *err*]
        (println (if (:hm/parse (ex-data e))
                   (str "usage: " (usage) "hm-clj: error: " (.getMessage e))
                   (str (when-not (:hm/held (ex-data e)) "hm: ") (.getMessage e))))
        (System/exit (if (:hm/parse (ex-data e)) 2 1))))
    (catch Exception e
      (binding [*out* *err*]
        (println "hm:" (.getMessage e))
        (System/exit 1)))))
