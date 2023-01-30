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

Limitations
===========

The libclang backend cannot tell that the function `f` has a dependency on enum `E` in the following situation:

```c
enum E { FirstVal = 1, SecondVal = 2 };

int f(int x) {
    if (x == FirstVal) {
        return 1;
    } else {
        return 0;
    }
}
```

Note however, that it will inform you about the dependency in the following situation:

```c
enum E { FirstVal = 1, SecondVal = 2 };

int f(enum E x) {
    if (x == FirstVal) {
        return 1;
    } else {
        return 0;
    }
}
```
