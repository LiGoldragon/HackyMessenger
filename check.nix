{ pkgs }:
pkgs.runCommand "hacky-messenger-check" { nativeBuildInputs = [ pkgs.python3 ]; } ''
  export PYTHONDONTWRITEBYTECODE=1
  python3 -m unittest discover -s ${./.} -v
  touch "$out"
''
