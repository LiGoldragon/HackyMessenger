# messenger-clj

`messenger-clj` is the standalone Clojure command line messenger for live Flow
routes in Herdr. It resolves a Flow ID to its registered pane, checks the route
and native harness identity, and submits each durable envelope once.

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
#psyche ["FLOW_ID" "context first" "1/2" "first verbatim piece "]
#psyche ["FLOW_ID" nil             "2/2" "second verbatim piece"]
```

Each complete `#psyche` envelope is at most 800 characters. Messenger packs the
verbatim input at word or whitespace boundaries, recomputes the pieces until
the `i/n` labels stabilize, and submits them sequentially. Context appears only
on the first piece. Concatenating the piece fields reproduces the verbatim
input exactly, including whitespace, newlines, and Unicode. A context or word
that cannot fit is held durably before any prompt. If one prompt is uncertain,
later pieces are not attempted.

`#msg` has no 800-character limit and continues to carry one whole machine
message. The ledger records the variant, part numbers, exact input fields, and
exact envelope for every submission attempt.

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

The Nix package is built with [clj-build](https://github.com/LiGoldragon/clj-build).
`bb.edn` doubles as the `deps.edn` that clj-build resolves once in a
fixed-output derivation; everything after that runs offline. The package holds a
Babashka uberjar built from `src/` in Nix, the pinned Datalevin pod 0.8.25,
`messenger-clj`, and the nine `hm-*` command links. No build product is
committed: the uberjar exists only as the output of the current source. The
build lives in `nix/`; `flake.nix` only indexes it.

```sh
nix build
nix run . -- --help
nix flake check
```

`nix flake check` builds the package, runs it against an empty typed ledger
(`check.nix`), and runs the Clojure test suite below as the `clj-tests` check,
with the pinned pod and a stub `orchestrate`.

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

Set `MESSENGER_CLJ_DATALEVIN_POD` to a Datalevin 0.8.25 `dtlv` to use the
pinned pod instead of fetching it. The suite uses fake Herdr boundaries and
temporary Datalevin stores. It never sends to a live route, but one core test
takes a Lock through the `orchestrate` on `PATH`.
