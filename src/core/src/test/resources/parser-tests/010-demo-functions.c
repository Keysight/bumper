void test_1(int);

#define ARRAY_SIZE 8

void test_1(int k) {
    int array[ARRAY_SIZE];
    if (k >= 0 && k <= ARRAY_SIZE) {
        array[k] = 0;
    }
}

#include <stdio.h>

void func_assign_for_equals(int k) {
	int j = 2;
	int i, l, m;
	// Warning - assignment of variable and comparison. Assignment destination variable already initialized
	if (j = k) {
		printf("j");
	}
	// Warning - assignment of constant and comparison. Assignment destination variable already initialized
    if (k = 5) {
        printf("%d\n", k);
    }
	// No warning - assignment of variable and comparison, assignment destination variable not previously initialized
    if (i = k) {
        printf("%d\n", k);
    }
	// No warning - assignment of constant and comparison, assignment destination variable not previously initialized
    if (l = 5) {
        printf("%d\n", k);
    }
	if (j == 6) {
		m = 5;
	}
	else {
		while (k != 6) {
			// No warning - m is not initialized on any path from start which reaches this point for the first time
			if (m = 5) {
				printf("a");
			}
			m = i;
		}
	}
	// Warning - assignment of constant and comparison. Assignment destination variable already initialized
	// Detected even in complex condition generating a single branch instruction
    if ((j == 5) | (j = 6)){
        printf("%d\n", k);
    }
	// Warning - assignment of constant and comparison. Assignment destination variable already initialized
	// Detected even in complex multi line condition generating multiple branch instructions
    if ((j == 5) ||
        (j = 6)){
        printf("%d\n", k);
    }
}
