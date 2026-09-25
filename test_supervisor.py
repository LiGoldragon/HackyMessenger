import contextlib
import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import hm
import supervisor


class SupervisorTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.registry = Path(self.temp.name)
        self.messenger = hm.Messenger(self.registry)
        self.lock = patch.object(hm.Messenger, 'reservation', lambda self, flow: contextlib.nullcontext())
        self.lock.start(); self.addCleanup(self.lock.stop)
        self.agent = {'session': 'test', 'name': 'seat', 'pane_id': 'w1:p2',
                      'terminal_id': 'term', 'agent': 'codex', 'interactive_ready': True}
        with patch.object(self.messenger, 'agents', return_value=[self.agent]):
            self.messenger.register('flow', 'seat', 'test', native_thread='11111111-2222-3333-4444-555555555555')

    def record(self):
        return json.loads((self.registry / 'flow.json').read_text())

    def test_exit_marks_but_never_deletes_or_retires(self):
        self.assertEqual(supervisor.apply_event(self.registry, {'type': 'pane_exited', 'pane_id': 'w1:p2'}), ['flow'])
        self.assertEqual(self.record()['state'], 'exited')
        self.assertTrue((self.registry / 'flow.json').exists())
        self.assertFalse((self.registry / 'retired' / 'flow.json').exists())

    def test_move_holds_the_exact_binding_for_a_registrar(self):
        supervisor.apply_event(self.registry, {'type': 'pane_moved', 'pane_id': 'w1:p2'})
        record = self.record()
        self.assertEqual(record['state'], 'transition')
        self.assertEqual(record['route_hold'], 'pane_moved')

    def test_null_agent_status_marks_exit(self):
        supervisor.apply_event(self.registry, {'type': 'pane_agent_status_changed', 'pane_id': 'w1:p2', 'agent': None})
        self.assertEqual(self.record()['exit']['event'], 'pane_agent_status_changed')


if __name__ == '__main__':
    unittest.main()
