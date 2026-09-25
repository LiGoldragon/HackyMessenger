{ pkgs, package }:
pkgs.runCommand "messenger-clj-check" { nativeBuildInputs = [ package ]; } ''
  export HOME="$TMPDIR/home" HM_REGISTRY="$TMPDIR/registry"
  mkdir -p "$HOME"
  messenger-clj --help > help
  grep -F 'Usage: messenger-clj' help
  test -x ${package}/bin/hm-send
  hm-heartbeat-state > state
  grep -F '"routes":[]' state
  touch "$out"
''
