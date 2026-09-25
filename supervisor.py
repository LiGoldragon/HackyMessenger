#!/usr/bin/env python3
"""Field's non-destructive HM registry projection of Herdr pane events.

Run one instance with ``herdr api events.subscribe`` feeding JSON events to
stdin.  It marks exact bindings exited or transitional; it never removes a
registration, retires a Flow, or sends a prompt.
"""
import argparse
import json
import os
import sys
import tempfile
from pathlib import Path

from hm import Messenger


EXIT_EVENTS = {'pane_exited', 'pane_closed'}
WATCHED_EVENTS = EXIT_EVENTS | {'pane_moved', 'pane_agent_status_changed'}


def event_type(event):
    return event.get('type') or event.get('event_type')


def event_pane(event):
    pane = event.get('pane')
    return event.get('pane_id') or (pane.get('pane_id') if isinstance(pane, dict) else None)


def write_atomic(path, value):
    with tempfile.NamedTemporaryFile('w', dir=path.parent, delete=False) as out:
        json.dump(value, out, sort_keys=True)
        out.write('\n')
        out.flush()
        os.fsync(out.fileno())
        temporary = Path(out.name)
    temporary.replace(path)


def apply_event(root, event):
    kind, pane_id = event_type(event), event_pane(event)
    if kind not in WATCHED_EVENTS or not isinstance(pane_id, str):
        return []
    messenger = Messenger(root)
    changed = []
    for path in sorted(messenger.root.glob('*.json')):
        flow = path.stem
        try:
            with messenger.reservation(flow):
                record = messenger.read(flow)
                if record.get('pane_id') != pane_id:
                    continue
                replacement = dict(record)
                if kind in EXIT_EVENTS or (kind == 'pane_agent_status_changed' and event.get('agent') is None):
                    replacement['state'] = 'exited'
                    replacement['exit'] = {'event': kind, 'pane_id': pane_id}
                elif kind == 'pane_moved':
                    replacement['state'] = 'transition'
                    replacement['transition'] = {'event': kind, 'predecessor': pane_id,
                                                 'successor_expected': None}
                    replacement['route_hold'] = 'pane_moved'
                else:
                    continue
                write_atomic(path, replacement)
                changed.append(flow)
        except Exception as error:
            raise RuntimeError(f'could not project {kind} for {flow}: {error}') from error
    return changed


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--registry', default=os.environ.get('HM_REGISTRY'))
    parser.add_argument('--stdin', action='store_true', help='read newline-delimited Herdr events')
    args = parser.parse_args()
    if not args.stdin:
        parser.error('provide Herdr events on stdin with --stdin')
    for line in sys.stdin:
        try:
            event = json.loads(line)
            changed = apply_event(args.registry, event)
            print(json.dumps({'event': event_type(event), 'changed': changed}, sort_keys=True), flush=True)
        except (ValueError, RuntimeError) as error:
            print(f'hm-supervisor: {error}', file=sys.stderr, flush=True)
            return 1


if __name__ == '__main__':
    main()
