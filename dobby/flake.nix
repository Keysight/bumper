{
  description = "Dobby, the house compilation command parser";

  inputs  = {
    nixpkgs.url = github:NixOS/nixpkgs/nixos-22.05;
  };

  outputs = { self, nixpkgs }:
    let
      pkgs = nixpkgs.legacyPackages.x86_64-linux;
    in {
      packages.x86_64-linux = rec {
        dobby = pkgs.callPackage ./.nix/dobby.nix {};
      };

      defaultPackage.x86_64-linux = self.packages.x86_64-linux.dobby;
    };
}
