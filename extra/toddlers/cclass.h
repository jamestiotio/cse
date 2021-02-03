#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>

//declaration of functions
int sum(int* array, int size);
int min(int* array, int size);
int max(int* array, int size);
// int geometric_sum(int* array, int size);
// int stdev(int* array, int size);
// int average(int* array, int size);

//legal declaration and initialization of pointer to function
int (*function_pointers[])(int*, int) = {
    &sum, //index 0
    &min, //index 1
    &max, //index 2
    // &geometric_sum, //index 3
    // &stdev, //index 4
    // &average //index 5
};

//legal declaration and initialization of pointer to function
int (*sum_function_pointer)(int*, int) = &sum;
int (*min_function_pointer)(int*, int) = &min;
void func(int (*f)(int*, int), int* array);

float multiply(float a, float b);
float (*multiply_function_pointer)(float, float) = &multiply;

int* test_function(int* array, int size);
int* (*test_function_pointer)(int*, int);

void* special_function(int arg);