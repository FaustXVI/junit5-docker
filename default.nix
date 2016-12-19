let
  _pkgs = import <nixpkgs> {};
in
{ pkgs ? import (_pkgs.fetchFromGitHub { owner = "NixOS";
                                         repo = "nixpkgs-channels";
                                         rev = "759620505595d72879d1d8c74a59c0868cce8f71";
                                         sha256 = "05x9szam0yqjdiz69px165krzsycsa419yq0zsv6s5lczmbl7cvn";
                                       }) {}
}:

pkgs.stdenv.mkDerivation rec {
  name = "junit5-docker";
  env = pkgs.buildEnv { name = name; paths = buildInputs; };
  buildInputs = [
      pkgs.openjdk
      pkgs.maven
      pkgs.docker
  ];
  MAVEN_OPTS = "-Djvm=${pkgs.openjdk}/bin/java";
}
