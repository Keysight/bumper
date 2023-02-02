Bumper: a C Frontend
====================

This defines a (partial) AST for C programs, and an interface for generating and working with that AST.
It also provides an implementation of that interface using libclang.

How to Build?
=============

Run `gradlew tasks` to make Gradle print all tasks.

Run `gradlew :bumper-libclang:test` to run the test suite of the libclang backend.

bumper-core has no tests.

implementations/antlr has no implementation and no tests (for now).
