#!/usr/bin/env bash
set -euo pipefail
export MESSENGER_CLJ_DATALEVIN_POD="@datalevinPod@/dtlv"

case "$(basename "$0")" in
  messenger-clj) ;;
  hm-send) set -- send "$@" ;;
  hm-send-abrupt) set -- send-abrupt "$@" ;;
  hm-list) set -- list "$@" ;;
  hm-register) set -- register "$@" ;;
  hm-deregister) set -- deregister "$@" ;;
  hm-rebind) set -- rebind "$@" ;;
  hm-move) set -- move "$@" ;;
  hm-retire) set -- retire "$@" ;;
  hm-heartbeat-state) set -- heartbeat-state "$@" ;;
  *) printf 'messenger-clj: unsupported command name %s\n' "$(basename "$0")" >&2; exit 2 ;;
esac

exec "@babashka@/bin/bb" --config "@out@/share/messenger-clj/bb.edn" \
  "@out@/share/messenger-clj/messenger-clj.clj" "$@"
