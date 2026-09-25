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

Machine prose is tagged EDN:

```clojure
#msg ["FLOW_ID" "text"]
```

The vector contains the sender Flow ID and the message text. Time, recipient,
harness kind, route identity, delivery attempts, pending messages, and
retirements stay in the typed ledger.

Living words use a separate variant:

```clojure
#psyche ["FLOW_ID" "context first" "verbatim words"]
```

Context always precedes the verbatim field. The verbatim field is serialized
exactly, including newlines and Unicode; it is never summarized or truncated.
Both variants may exceed Claude's paste threshold. Messenger sends the whole
tagged envelope into the pane and accepts Claude's `pasted_content` wrapper.
The ledger records the variant, the exact input fields, and the exact envelope
submitted to Herdr.

Pass only the body to `hm-send` or `messenger-clj send`. A field that parses as
one complete `#msg` or `#psyche` form is rejected. Ordinary prose may mention
either tag. There is no overflow file or pointer path.

## Use

During development, the Bash launcher runs Babashka against the source tree:

```sh
bin/messenger-clj --help
FLOW_ID=<self> bin/messenger-clj send TARGET 'text'
FLOW_ID=<self> bin/messenger-clj send TARGET --psyche 'why these words matter' 'verbatim words'
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

The managed Home activation exposes that package through
`~/.local/libexec/messenger-clj`; the old
`~/.local/libexec/hacky-messenger-clojure` installation is removed after the
launcher cutover succeeds.

## State continuity

The final typed state root is `~/.local/state/messenger-clj`. `HM_REGISTRY` may
select another root for tests and isolated operation. The old Python source and
JSON state are retained only as frozen migration inputs.

The installed launcher and this source select the typed state root
`~/.local/state/messenger-clj`. Deployment replaces the package target behind
all nine `hm-*` aliases atomically and does not move or rewrite that database.

Concurrent operations use unique Orchestrate lock names against the same state
path. A competing operation waits for that path lock for a bounded interval;
timeout refuses before a prompt or ledger write.

## Clojure tests

```sh
bb --config bb.edn -e \
  '(require '\''messenger-clj.core-test '\''messenger-clj.cli-test '\''messenger-clj.typed-store-test '\''messenger-clj.legacy-import-test) (apply clojure.test/run-tests ['\''messenger-clj.core-test '\''messenger-clj.cli-test '\''messenger-clj.typed-store-test '\''messenger-clj.legacy-import-test])'
```

The suite uses fake Herdr boundaries and temporary Datalevin stores. It never
sends to a live route.
