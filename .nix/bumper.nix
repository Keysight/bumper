{ pkgs, stdenv, lib,
  maven, gradle, jdk11,
  llvmPackages_11, ncurses, makeWrapper, zlib,
  nodejs, nodePackages
}:

let
  llvmPackages = llvmPackages_11;
in stdenv.mkDerivation rec {
  pname   = "bumper";
  version = "latest";
  name    = "${pname}-${version}";

  src = ./..;
  buildInputs = [ jdk11 gradle llvmPackages.clang ];

  # runtime flags for the dev shell
  LIBCLANG_DISABLE_CRASH_RECOVERY = "1";
  LD_LIBRARY_PATH = lib.makeLibraryPath [
    stdenv.cc.cc.lib
    llvmPackages.libclang.lib
    llvmPackages.libllvm.lib
  ];
}
