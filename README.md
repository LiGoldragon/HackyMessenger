# messenger-clj

`messenger-clj` is the standalone Clojure command line messenger for live Flow
routes in Herdr. It resolves a Flow ID to its registered pane, checks the route
and native harness identity, and prompts that pane exactly once.

The public compatibility commands remain available:

```text
hm-send          hm-send-abrupt
hm-list          hm-register
hm-deregister    hm-rebind
hm-move          hm-retire
hm-heartbeat-state
```

They invoke the same `messenger-clj` program and the same typed Datalevin
ledger. There is no Python runtime fallback.

## Message shape

The recipient sees one line of tagged EDN:

```clojure
#msg ["FLOW_ID" "text"]
```

The vector contains the sender Flow ID and the message text. Time, recipient,
harness kind, route identity, delivery attempts, pending messages, and
retirements stay in the typed ledger.

Pass only the body to `hm-send` or `messenger-clj send`. A body that parses as
one complete `#msg` form is rejected. Ordinary prose may mention `#msg`.
Messages longer than the one line pane limit are written under the sender's
`flows/FLOW_ID/messages/` directory and replaced with a pointer.

## Use

During development, the Bash launcher runs Babashka against the source tree:

```sh
bin/messenger-clj --help
FLOW_ID=<self> bin/messenger-clj send TARGET 'text'
```

The `hm-*` scripts are development compatibility launchers. JSON migration is
available only through `messenger-clj import-json`; it is not an operational
authority or fallback.

The Nix package installs the compiled Babashka uberscript, a pinned Datalevin
pod, `messenger-clj`, and the nine `hm-*` command links:

```sh
nix build
nix run . -- --help
nix flake check
```

## State continuity

The typed state root stays at
`~/.local/state/hacky-messenger-clojure` so the repository rename does not
split the live registry or ledger. `HM_REGISTRY` may select another root for
tests and isolated operation. The old Python source and JSON state are retained
only as frozen migration inputs.

## Clojure tests

```sh
bb --config bb.edn -e \
  '(require '\''messenger-clj.core-test '\''messenger-clj.cli-test '\''messenger-clj.typed-store-test '\''messenger-clj.legacy-import-test) (apply clojure.test/run-tests ['\''messenger-clj.core-test '\''messenger-clj.cli-test '\''messenger-clj.typed-store-test '\''messenger-clj.legacy-import-test])'
```

The suite uses fake Herdr boundaries and temporary Datalevin stores. It never
sends to a live route.
