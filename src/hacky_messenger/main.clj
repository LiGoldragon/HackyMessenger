(ns hacky-messenger.main
  (:require [hacky-messenger.core :as hm]))
(defn usage [] (str "Usage: hm-clj <send|register|list> ...\n" hm/skill-note))
(defn arg [xs option] (second (drop-while #(not= option %) xs)))
(defn -main [& argv]
  (try
    (let [[op & xs] argv]
      (if (or (nil? op) (= op "--help") (= op "-h") (some #{"--help" "-h"} xs))
        (println (usage))
        (case op
          "send" (let [[flow body & rest] xs] (println (hm/send! flow body (boolean (some #{"--wait-presented"} rest)) (arg rest "--pane"))))
          "register" (let [[flow name & rest] xs session (arg rest "--session") thread (arg rest "--native-thread")]
                       (when-not (and session thread) (hm/fail "register requires --session and --native-thread"))
                       (println (hm/register! flow name session thread)))
          "list" (println (hm/listing!))
          (hm/fail (usage)))))
    (catch Exception e (binding [*out* *err*] (println "hm:" (.getMessage e))) (System/exit 1))))
