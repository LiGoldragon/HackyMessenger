{ runCommand, bash, babashka-unwrapped, source, config }:
runCommand "messenger-clj-test-source" { } ''
  cp -r ${source} "$out"
  chmod -R u+w "$out"
  grep -rlF '#!/usr/bin/env bash' "$out" | while IFS= read -r file; do sed -i 's|#!/usr/bin/env bash|#!${bash}/bin/bash|' "$file"; done
  cat > "$out/bin/messenger-clj" <<'LAUNCHER'
  #!${bash}/bin/bash
  set -euo pipefail
  exec ${babashka-unwrapped}/bin/bb --config ${config} --classpath "$MESSENGER_CLJ_CLASSPATH" -m messenger-clj.main "$@"
  LAUNCHER
  chmod +x "$out/bin/messenger-clj"
''
