# This file descibe the environment needed to build junit5-docker
# Make sure you have `nix` install (https://nixos.org/nix/)
# In order to create the environment, just run `nix-shell`

# First, lets fix the version of the repository we will get ou dependencies from
let
  _pkgs = import <nixpkgs> {};
  nixpkgs = _pkgs.fetchFromGitHub { owner = "NixOS";
                                         repo = "nixpkgs-channels";
                                         rev = "1849e695b00a54cda86cb75202240d949c10c7ce";
                                         sha256 = "1fw9ryrz1qzbaxnjqqf91yxk1pb9hgci0z0pzw53f675almmv9q2";
                                       };
in
# then override the packages with the version-fixed one
with import nixpkgs {};

# now we can build our specific environment (named derivation by nix)
stdenv.mkDerivation {
  name = "junit5-docker";
  # let's declare our depencies. 
  # The names to put here can be find thanks to the commande `nix-env -qaP "regex"`
  buildInputs = [
      openjdk
      maven
      docker
  ];
  # declare the environment variables we need
  MAVEN_OPTS = "-Djvm=${openjdk}/bin/java";
}
