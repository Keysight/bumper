typedef int (*tzbsp_syscall_func_t) ();

typedef struct tzbsp_syscall_entry_s {
    tzbsp_syscall_func_t func;
} tzbsp_syscall_entry_t;

int func_a(int k) {
    return k+1;
}

int func_b() {
    return 1;
}

tzbsp_syscall_entry_t func_switch(int k) {
    tzbsp_syscall_entry_t res;
    switch (k) {
    case 0:
        res = (tzbsp_syscall_entry_t) {
            .func = func_a
        };
        break;
    case 1:
        res = (tzbsp_syscall_entry_t) {
            .func = func_b
        };
        break;
    }
    return res;
}