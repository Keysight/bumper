# Dobby

The compile command parser.

## Why

We build developer tools on top of existing builds. Usually we obtain compile commands
from a compilation database produced by CMAKE or Clang. We then have to create
variations of those commands to build ASTS that we can analyze, or to build
binaries that we can run on different target platforms.

To reliably create those variations, we need to attach enough meaning to the command.
Dobby parses commands for various compilers, attaching this meaning.
Dobby also implements methods for altering the commands, taking into account everything
we know about the intended meaning.

All in all, parsing compile commands is more an art than a science. Specifications are spotty,
and we must deal with custom compilers for which we do not have a specification at all.

