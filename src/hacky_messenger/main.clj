(ns hacky-messenger.main
  (:require [hacky-messenger.core :as hm]))
(defn usage [] (str "Usage: hm-clj <send|register|deregister|rebind|move|retire|import-retirement|list> ...\n" hm/skill-note))
(defn arg [xs option] (second (drop-while #(not= option %) xs)))
(defn parse-error [message] (throw (ex-info message {:hm/parse true})))
(defn unknown-flags! [xs allowed]
  (doseq [value xs :when (and (.startsWith value "--") (not (contains? allowed value)))]
    (parse-error (str "unrecognized arguments: " value))))
(defn -main [& argv]
  (try
    (let [[op & xs] argv]
      (if (or (nil? op) (= op "--help") (= op "-h") (some #{"--help" "-h"} xs))
        (println (usage))
        (case op
          "send" (let [[flow body & rest] xs]
                   (when-not (and flow body) (parse-error "the following arguments are required: flow, message"))
                   (unknown-flags! rest #{"--wait-presented" "--hold-seconds" "--pane"})
                   (let [hold (or (arg rest "--hold-seconds") "10")]
                     (when-not (try (<= 0 (Double/parseDouble hold) 60) (catch Exception _ false))
                       (hm/fail "--hold-seconds must be between 0 and 60")))
                   (println (hm/send! flow body (boolean (some #{"--wait-presented"} rest)) (arg rest "--pane"))))
          "register" (let [[flow name & rest] xs session (arg rest "--session") thread (arg rest "--native-thread")
                           marker (arg rest "--readiness-probe") rollout (arg rest "--rollout")]
                       (when-not (and flow name) (parse-error "the following arguments are required: flow, name"))
                       (unknown-flags! rest #{"--session" "--native-thread" "--readiness-probe" "--rollout"})
                       (when-not (and session thread) (hm/fail "register requires --session and --native-thread in the Clojure proof"))
                       (println (hm/register! flow name session thread marker rollout)))
          "deregister" (let [[flow & rest] xs]
                         (when-not flow (parse-error "the following arguments are required: flow"))
                         (unknown-flags! rest #{"--session" "--pane-id" "--terminal-id" "--name"})
                         (let [session (arg rest "--session") pane-id (arg rest "--pane-id") terminal-id (arg rest "--terminal-id") name (arg rest "--name")]
                           (when-not (every? some? [session pane-id terminal-id name])
                             (parse-error "the following arguments are required: --session, --pane-id, --terminal-id, --name"))
                           (println (hm/deregister! flow session pane-id terminal-id name))))
          "rebind" (let [[flow new-name & rest] xs]
                     (when-not (and flow new-name) (parse-error "the following arguments are required: flow, new_name"))
                     (unknown-flags! rest #{"--old-name" "--session" "--pane-id" "--terminal-id" "--agent" "--native-thread"})
                     (let [old-name (arg rest "--old-name") session (arg rest "--session") pane-id (arg rest "--pane-id")
                           terminal-id (arg rest "--terminal-id") agent (arg rest "--agent") native-thread (arg rest "--native-thread")]
                       (when-not (every? some? [old-name session pane-id terminal-id agent native-thread])
                         (parse-error "the following arguments are required: --old-name, --session, --pane-id, --terminal-id, --agent, --native-thread"))
                       (println (hm/rebind! flow old-name new-name session pane-id terminal-id agent native-thread))))
          "move" (let [[flow workspace & rest] xs]
                   (when-not (and flow workspace) (parse-error "the following arguments are required: flow, workspace"))
                   (unknown-flags! rest #{"--session" "--pane-id" "--terminal-id" "--name" "--agent" "--native-thread" "--process-pid"})
                   (let [session (arg rest "--session") pane-id (arg rest "--pane-id") terminal-id (arg rest "--terminal-id")
                         name (arg rest "--name") agent (arg rest "--agent") native-thread (arg rest "--native-thread") pid-text (arg rest "--process-pid")]
                     (when-not (every? some? [session pane-id terminal-id name agent native-thread pid-text])
                       (parse-error "the following arguments are required: --session, --pane-id, --terminal-id, --name, --agent, --native-thread, --process-pid"))
                     (let [pid (try (parse-long pid-text) (catch Exception _ (parse-error "argument --process-pid: invalid int value")))]
                       (println (hm/move! flow session pane-id terminal-id name agent native-thread pid workspace)))))
          ("retire" "import-retirement") (let [[flow & rest] xs]
                                           (when-not flow (parse-error "the following arguments are required: flow"))
                                           (unknown-flags! rest #{"--session" "--pane-id" "--terminal-id" "--name" "--agent" "--native-thread" "--evidence" "--evidence-sha256"})
                                           (let [session (arg rest "--session") pane-id (arg rest "--pane-id") terminal-id (arg rest "--terminal-id") name (arg rest "--name")
                                                 agent (arg rest "--agent") native-thread (arg rest "--native-thread") evidence (arg rest "--evidence") digest (arg rest "--evidence-sha256")]
                                             (when-not (every? some? [session pane-id terminal-id name agent native-thread evidence digest])
                                               (parse-error "the following arguments are required: --session, --pane-id, --terminal-id, --name, --agent, --native-thread, --evidence, --evidence-sha256"))
                                             (println (hm/retire! flow session pane-id terminal-id name agent native-thread evidence digest (= op "import-retirement")))))
          "list" (println (hm/listing!))
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
