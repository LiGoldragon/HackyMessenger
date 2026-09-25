{ pkgs, package }:
pkgs.runCommand "messenger-clj-check" { nativeBuildInputs = [ package ]; } ''
  messenger-clj --help > help
  grep -F 'Usage: messenger-clj' help
  test -x ${package}/bin/hm-send
  test -x ${package}/bin/hm-heartbeat-state
  touch "$out"
''
