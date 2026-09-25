{
  description = "Standalone Clojure messenger for live Flow routes";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    clj-build = {
      url = "github:LiGoldragon/clj-build";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, clj-build }:
    let
      systems = [ "x86_64-linux" ];
      forSystems = nixpkgs.lib.genAttrs systems;
      build = system: import ./nix {
        pkgs = nixpkgs.legacyPackages.${system};
        clj = clj-build.lib.${system};
        root = ./.;
      };
    in {
      packages = forSystems (system: rec {
        messenger-clj = (build system).package;
        default = messenger-clj;
      });
      apps = forSystems (system: {
        default = {
          type = "app";
          program = "${self.packages.${system}.default}/bin/messenger-clj";
        };
      });
      checks = forSystems (system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
          package = self.packages.${system}.default;
        in {
          inherit package;
          cli = pkgs.callPackage ./check.nix { inherit package; };
          clj-tests = (build system).tests;
        });
    };
}
