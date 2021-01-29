#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#define BUFFERSIZE 1024

// defining struct
typedef struct Vector_Int
{
    int x;
    int y;
    int z;
    char name[64];
} Vector;

// define functions
void print_vector(Vector input);
float square(float a);
Vector clear_vector(Vector input);
void clear_vector_byreference(Vector *input);

int global_variable;

void test_global(void);
int test_static(void);
int test_local(void);

void test();
int* test_pointer;

int* test_malloc(int size_array);

void modify_array(int* array, int array_size);

int function1();