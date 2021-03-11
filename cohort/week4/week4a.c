#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>

void *functionC();
pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
int counter = 0;

int main()
{
    int rc1, rc2;
    pthread_t thread1, thread2;

    /* Create independent threads each of which will execute functionC */

    if ((rc1 = pthread_create(&thread1, NULL, &functionC, NULL)))
    {
        printf("Thread 1 creation failed: %d\n", rc1);
    }

    if ((rc2 = pthread_create(&thread2, NULL, &functionC, NULL)))
    {
        printf("Thread 2 creation failed: %d\n", rc2);
    }

    // Main thread waits until both threads have finished execution

    pthread_join(thread1, NULL);
    pthread_join(thread2, NULL);

    return 0;
}

void *functionC()
{
    pthread_mutex_lock(&mutex);
    counter++;
    printf("Counter value: %d\n", counter);
    pthread_mutex_unlock(&mutex);
}