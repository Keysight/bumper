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

References
==========

Bumper's model of C is inspired by [CompCert's](https://github.com/AbsInt/CompCert) (Leroy, X., et al.)
abstract syntax of [C](https://github.com/AbsInt/CompCert/blob/master/cparser/C.mli) _after_ elaboration. 
In turn, CompCert stands on the shoulders of [CIL](http://people.eecs.berkeley.edu/~necula/cil/) (Necula, G.C., et al.).

Bumper itself was originally authored by Arjen Rouvoet at Riscure.

How to Build?
=============

Run `gradlew tasks` to make Gradle print all tasks.
Run `gradlew :bumper-libclang:test` to run the test suite of the libclang backend.
