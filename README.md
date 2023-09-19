Bumper: a C Frontend
====================

Bumper is a (partial) model of well typed C programs, and an implementation
of the parsers + type-checker that constructs the model for the JVM. 
The implementation is based on (but decoupled from) libclang.

Bumper's model aims to be faithful to the static semantics of C. 
We have used it to parse and analyze large embedded C projects.

The main limitation at the moment is that we do not parse expressions and
statements. There is a model, but we do not parse them, because we do not need to analyze them.
(There is infrastructure to extract the textual representation of the right-hand side of definitions.)

In addition to the model and parser, Bumper also ships a module with an (approximate) C lexer to
produce syntax-highlighted code, and some basic analyses for the model.
To be able to process compiler options, it implements a parser generator for
Clang options and a concrete parser generated from the Clang option specification.

To get a decent impression of what it does and how it does it, check out the
test module and the instantiation of those tests in the libclang implementation module.

References
==========

Bumper's model of C is inspired by [CompCert's](https://github.com/AbsInt/CompCert) (Leroy, X., et al.)
abstract syntax of [C](https://github.com/AbsInt/CompCert/blob/master/cparser/C.mli) _after_ elaboration. 
In turn, CompCert stands on the shoulders of [CIL](http://people.eecs.berkeley.edu/~necula/cil/) (Necula, G.C., et al.).

Bumper itself was originally authored by Arjen Rouvoet at Riscure.
This library is not officially supported or maintained by Riscure. 
If it crashes or you have feature requests, please make contact through 
Github where this code is hosted.

How to Build?
=============

Run `gradlew tasks` to make Gradle print all tasks.
Run `gradlew :bumper-libclang:test` to run the test suite of the libclang backend.

License
=======

Bumper: a C Frontend for the JVM
Copyright (C) 2023, Riscure

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
