(ns hacky-messenger.main
  (:require [hacky-messenger.core :as hm]))
(defn usage [] (str "Usage: hm-clj <send|register|list> ...\n" hm/skill-note))
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
          "register" (let [[flow name & rest] xs session (arg rest "--session") thread (arg rest "--native-thread")]
                       (when-not (and flow name) (parse-error "the following arguments are required: flow, name"))
                       (unknown-flags! rest #{"--session" "--native-thread" "--readiness-probe" "--rollout"})
                       (when-not (and session thread) (hm/fail "register requires --session and --native-thread in the Clojure proof"))
                       (println (hm/register! flow name session thread)))
          "list" (println (hm/listing!))
          (parse-error (str "invalid choice: " op)))))
    (catch clojure.lang.ExceptionInfo e
      (binding [*out* *err*] (println (if (:hm/parse (ex-data e)) (str "usage: " (usage) "hm-clj: error: " (.getMessage e)) (str "hm: " (.getMessage e))))
               (System/exit (if (:hm/parse (ex-data e)) 2 1)))
      (catch Exception e (binding [*out* *err*] (println "hm:" (.getMessage e))) (System/exit 1)))))
