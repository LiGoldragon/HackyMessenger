{
  description = "Standalone Clojure messenger for live Flow routes";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }:
    let
      systems = [ "x86_64-linux" ];
      forSystems = nixpkgs.lib.genAttrs systems;
      packageFor = system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
          babashka = pkgs.babashka-unwrapped;
          datalevinPod = pkgs.fetchzip {
            url = "https://github.com/juji-io/datalevin/releases/download/0.8.25/dtlv-0.8.25-ubuntu-latest-amd64.zip";
            hash = "sha256-pS/F4UIWKdU/ftZR1hNXBsnI9TNRYXyjia2UKNJHU9k=";
            stripRoot = false;
          };
        in pkgs.stdenvNoCC.mkDerivation {
          pname = "messenger-clj";
          version = "0.2.0";
          src = nixpkgs.lib.cleanSourceWith {
            src = ./.;
            filter = path: type:
              let relative = nixpkgs.lib.removePrefix "${toString ./.}/" (toString path);
              in !(relative == ".jj" || nixpkgs.lib.hasPrefix ".jj/" relative
                   || relative == ".git" || nixpkgs.lib.hasPrefix ".git/" relative
                   || relative == "result");
          };
          dontBuild = true;
          installPhase = ''
            runHook preInstall
            install -Dm644 dist/messenger-clj.clj "$out/share/messenger-clj/messenger-clj.clj"
            install -Dm644 nix/bb.edn "$out/share/messenger-clj/bb.edn"
            install -Dm755 nix/launcher.sh "$out/bin/messenger-clj"
            substituteInPlace "$out/bin/messenger-clj" \
              --replace-fail '@babashka@' '${babashka}' \
              --replace-fail '@datalevinPod@' '${datalevinPod}' \
              --replace-fail '@out@' "$out"
            for command in send send-abrupt list register deregister rebind move retire heartbeat-state; do
              ln -s messenger-clj "$out/bin/hm-$command"
            done
            runHook postInstall
          '';
          doInstallCheck = true;
          installCheckPhase = ''
            "$out/bin/messenger-clj" --help > help
            grep -F 'Usage: messenger-clj' help
            test "$(readlink "$out/bin/hm-send")" = messenger-clj
            ! grep -R 'python\|hm.py' "$out/bin"
          '';
          meta = {
            description = "Typed Clojure CLI messenger with hm-* compatibility commands";
            license = nixpkgs.lib.licenses.epl20;
            platforms = [ "x86_64-linux" ];
            mainProgram = "messenger-clj";
          };
        };
    in {
      packages = forSystems (system: {
        default = packageFor system;
        messenger-clj = packageFor system;
      });
      apps = forSystems (system: {
        default = {
          type = "app";
          program = "${self.packages.${system}.default}/bin/messenger-clj";
        };
      });
      checks = forSystems (system:
        let pkgs = nixpkgs.legacyPackages.${system};
        in {
          package = self.packages.${system}.default;
          cli = pkgs.callPackage ./check.nix { package = self.packages.${system}.default; };
        });
    };
}
