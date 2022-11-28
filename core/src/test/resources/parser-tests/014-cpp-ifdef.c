#define X 42

int f() {
    #ifdef X
        printf("good");
    #else
        printf("bad");
    #endif
}