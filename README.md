# Hacky Messenger

Hacky Messenger (HM) is the live compensation layer for messaging between
flows in Herdr. The installed commands resolve a Flow ID to its current pane,
check the route and native harness identity, and prompt that pane. Flow Nexus
and Message Nexus are intended to replace it.

## What the recipient sees

HM types exactly one line of tagged EDN into the target pane:

```clojure
#msg ["FLOW_ID" "text"]
```

The vector contains only the sender's Flow ID and the message text. Time,
recipient, harness kind, route identity, delivery attempts, pending messages,
and retirements belong to the typed Datalevin ledger and do not appear in the
pane message.

HM constructs this envelope. Pass only the body to `hm-send`; never place one
complete tagged message inside another. A body that parses as exactly one
complete `#msg` form is rejected. Ordinary prose may mention `#msg`.

The pane line is at most 800 characters and contains no newline. HM collapses
up to three short body lines. Longer content is written under the sender's
`flows/FLOW_ID/messages/` directory and the tagged message carries a short
instruction to read that file.

## Commands

Nine unprefixed commands on `PATH` run the typed Clojure implementation:

```text
hm-send          hm-send-abrupt
hm-list          hm-register
hm-deregister    hm-rebind
hm-move          hm-retire
hm-heartbeat-state
```

`FLOW_ID=<self> hm-send TARGET 'text'` is the ordinary send. `hm-send-abrupt`
interrupts the current turn before prompting. The registry commands require
the exact live or stale route identity shown by Herdr; use their `--help`
output and the `compensation-hacky-messenger` Curriculum skill for the full
arguments and receipt meanings.

`hm-heartbeat-state` takes no message body or other arguments. It reads the
typed store and prints a read-only view of current routes and retirements for
the heartbeat consumer; it does not send or mutate a message.

The Clojure-only maintenance commands are exposed with the `hm-clj-` prefix:
`hm-clj-import-json` and `hm-clj-import-retirement`.

## Authority and storage

The live Clojure state root defaults to
`~/.local/state/hacky-messenger-clojure` and may be set with `HM_REGISTRY`.
Datalevin is authoritative for routes, attempts, pending messages, and
retirements. The legacy Python and JSON files remain only for migration and
historical checks; the installed unprefixed commands do not target them.

The installed launchers live under
`~/.local/libexec/hacky-messenger-clojure/`. The source is this repository's
`clojure` branch. Every HM behavior change must update the authored
`skills/compensation-hacky-messenger.md` in Curriculum in the same landing.

## Checks

```sh
bb --config bb.edn -e \
  '(require '\''hacky-messenger.core-test '\''hacky-messenger.cli-test '\''hacky-messenger.typed-store-test '\''hacky-messenger.legacy-import-test) (apply clojure.test/run-tests ['\''hacky-messenger.core-test '\''hacky-messenger.cli-test '\''hacky-messenger.typed-store-test '\''hacky-messenger.legacy-import-test])'
```

The test suite uses fake Herdr boundaries and temporary Datalevin stores.
