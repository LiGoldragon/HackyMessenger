# Hacky Messenger — architecture

HM has no daemon, socket, or multiplexer of its own. Each command is one
process that reads a file registry, asks Herdr about live agents, and, for a
send, types one prompt into one pane.

## Parts

`hm.py` holds the `Messenger` class and the command line. `bin/hm-*` are
Bash wrappers, one per subcommand. `supervisor.py --stdin` reads Herdr pane
events (one `events.subscribe` stream) and marks bindings: `pane_exited`,
`pane_closed`, and a null agent status mark a binding `exited`; `pane_moved`
marks it in transition. It never deletes a registration or retires a Flow.

## Registry

One JSON file per Flow, `FLOW.json`, in `~/.local/state/hacky-messenger` or
`HM_REGISTRY`. A record holds session, pane, terminal, agent name, harness,
native thread, and, when registered through a probe, `readiness_proof`.
Records are replaced atomically. Every registry write and every send runs
under an Orchestrate reservation of the registry directory; contention fails
visibly and is never retried. `attempts.jsonl` is an fsynced ledger of every send
decision; `pending/` keeps the text of a message held as `NotRegistered` or
`InTransition`. `retired/` holds retirement markers.

## Send

A send validates the text (nonempty, at most 64 KiB, no terminal control
characters except newline and tab) and requires `FLOW_ID`. It waits up to
`--hold-seconds` for a missing or transitional binding, then, under the
reservation, checks retirement, the binding, the live Herdr agent, its
terminal, readiness, status, and the foreground process's native thread.
Any failed check records a `Held` attempt and types nothing. It then sends a
`Machine.Relay` envelope through `herdr agent prompt`, re-reads the target, and
prints `Transported` or, with `--wait-presented`, `Presented`. A failure after
the prompt is `Uncertain` and is never retried. It never falls back to another
route or Flow.

Abrupt send first sends Escape through `herdr agent send-keys` (one for Codex;
two for Claude, whose vim editor mode eats the first, followed by Enter after
the prompt). Escape does not clear text already in the input box.

## Route changes

Registration refuses a Flow bound to a different terminal, a retired native
thread, and a route hold. `rebind` changes only the agent name of an unchanged
route. `move` persists a route hold before Herdr moves the pane, follows the
new pane ID, and on failure moves the terminal back; an unverified
compensation leaves the hold for manual repair. `deregister` is route repair,
not retirement. `retire` writes an evidence-bound marker (exact route, native
thread, SHA-256 of a lifecycle receipt) that blocks every later send and
registration of that native thread; `import-retirement` records one after a
separately witnessed deregistration.

## Limits

Identity checking and prompting are separate Herdr calls, so a terminal
replacement can race a send. A printed grade is not a read receipt.
