{ pkgs, stdenv, lib, gradle, jdk11}:

stdenv.mkDerivation rec {
  pname   = "bumper";
  version = "nightly";
  name    = "${pname}-${version}";

  src = ./..;
  buildInputs = [ jdk11 gradle ];
}
