#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h> //pth
// #include <kthread.h>

int shared_integer; // instantiate integer in fixed memory

// This is a user-level thread
void *myThreadFunc(void *vargp)
{
    sleep(1);
    char argument = *(char *)vargp;
    printf("Printing helloWorld from spawned thread with argument: %c\n", argument);
    shared_integer++;

    char answer = 'a'; // in stack
    char *answer_heap = malloc(sizeof(char)); // in heap

    *answer_heap = 'a';
    pthread_exit((void *)answer_heap);

    // pthread_exit((void *) &answer); // will result in segfault
}

int main()
{
    shared_integer = 10;
    pthread_t thread_id;

    char input_argument = 'b'; // to pass to thread
    char *result;              // to hold return value

    // pthread_create(pthread_t* thread_id, NULLABLE thread attributes, void* function_for_thread, NULLABLE void* argument)
    pthread_create(&thread_id, NULL, myThreadFunc, &input_argument);

    // If you would like to instantiate/create a kernel-level thread, uncomment the following code line
    // kthread_create(int (*function)(void *data), void *data, const char name[], ...)

    sleep(1);
    printf("shared_integer: %d\n", shared_integer); // potential race condition

    // pthread_join(pthread_t thread_id, void** return_value);
    // Second argument of type void **: pass the address of "result", which is a pointer to the supposed return value of the thread

    // blocking
    pthread_join(thread_id, (void **)&result);
    printf("After thread join...\n");

    printf("shared_integer: %d\n", shared_integer);
    printf("result: %c\n", *result);

    exit(0);
}