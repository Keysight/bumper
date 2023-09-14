{ pkgs, stdenv, lib, gradle, jdk11}:

stdenv.mkDerivation rec {
  pname   = "dobby";
  version = "nightly";
  name    = "${pname}-${version}";

  src = ./..;
  buildInputs = [ jdk11 gradle ];
}
