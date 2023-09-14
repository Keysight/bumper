void f() {}
void g() {}

typedef void (*funcptr)();
funcptr pointers[] = { &f, &g };
