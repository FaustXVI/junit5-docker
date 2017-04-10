let
  _pkgs = import <nixpkgs> {};
in
{ pkgs ? import (_pkgs.fetchFromGitHub { owner = "NixOS";
                                         repo = "nixpkgs-channels";
                                         rev = "1849e695b00a54cda86cb75202240d949c10c7ce";
                                         sha256 = "1fw9ryrz1qzbaxnjqqf91yxk1pb9hgci0z0pzw53f675almmv9q2";
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
