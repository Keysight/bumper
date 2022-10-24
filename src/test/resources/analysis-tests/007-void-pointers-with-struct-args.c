struct S {};
void f(struct S x) {}
void g(struct S x) {}

// an array pointers containing functions with one argument
// of type struct S
void (*pointers[])(struct S x) = { &f, &g };
