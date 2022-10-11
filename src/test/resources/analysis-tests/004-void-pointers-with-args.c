void f(int x) {}
void g(int x) {}


typedef void (*funcptr)(int x);
funcptr pointers[] = { &f, &g };
