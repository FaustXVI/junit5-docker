{ }:

let
  pkgs = import <nixpkgs> { };
in
  pkgs.stdenv.mkDerivation  {
      name = "junit5-docker-jekyll";
      buildInputs = [
          pkgs.jekyll
      ];
  }
