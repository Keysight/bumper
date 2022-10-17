/* function type */
typedef void f();

/* function type returning void pointer */
typedef void *h();

/* function pointer with two anonymous arguments*/
typedef void (*g)(int,int);

/* function pointer */
typedef void (*f0)();

/* array of function pointers */
typedef void (*f1[])();

/* pointer ot array of function pointers */
typedef void (*(*f2[1]))();
typedef void (**f2_alt[1])();

/* 2D array of function pointers */
typedef void (*f3[1][2])();

/* pointer to 2D array of function pointers */
typedef void (*(*f4[1][2]))();
typedef void (**f4_alt[1][2])();

/* array of function pointers that return an int pointer */
typedef int* (*f5[])();

/* function pointer that takes function pointer as argument */
typedef void (*f6)(int (*f)());

/* A function returning a function pointer */
int (*broWat())(int, int);

/* The same function, with a more explicit syntax for 'no arguments' */
int (*broWatVoid(void))(int, int);

/* The equivalent function returning an array of constant size does not parse: */
/* int (returnAVector(void))[3]; */

/* A function taking a function parameter */
int map(void (*g)(int, int));

int mapWithNamedParams(void (*g)(int x, int y));
