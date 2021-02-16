#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <semaphore.h>
#include <unistd.h>

#define RESOURCES 10
#define REPEAT 100

sem_t *blank_space;
sem_t *content;

int buffer[RESOURCES];
int write_index = 0;
int read_index = 0;

void *producer_function(void *arg)
{
    for (int i = 0; i < REPEAT; i++)
    {
        // wait
        sem_wait(blank_space);
        // write to buffer
        buffer[write_index] = i;
        // advance write pointer
        write_index = (write_index + 1) % RESOURCES;
        // signal
        sem_post(content);
    }

    return NULL;
}

void *consumer_function(void *arg)
{
    for (int i = 0; i < REPEAT; i++)
    {
        // wait
        sem_wait(content);
        // read from buffer
        int value = buffer[read_index];
        printf("Consumer reads: %d \n", value);
        // advance write pointer
        read_index = (read_index + 1) % RESOURCES;
        // signal
        sem_post(blank_space);
    }

    return NULL;
}

int main()
{
    // instantiate named semaphore, works on macOS
    // sem_t *sem_open(const char *name, int oflag,
    //                   mode_t mode, unsigned int value);
    // @mode: set to have R&W permission for owner, group owner, and other user
    blank_space = sem_open("blank_space", O_CREAT,
                            S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH,
                            RESOURCES);
    printf("%p \n", (void *)blank_space);
    if (blank_space == (sem_t *)SEM_FAILED)
    {
        printf("Sem Open Failed.\n");
        exit(1);
    }
    content = sem_open("content", O_CREAT,
                        S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH,
                        0);
    printf("%p \n", (void *)content);
    if (content == (sem_t *)SEM_FAILED)
    {
        printf("Sem Open Failed.\n");
        exit(1);
    }

    pthread_t producer, consumer;
    pthread_create(&producer, NULL, producer_function, NULL);
    pthread_create(&consumer, NULL, consumer_function, NULL);

    printf("Joining threads\n");
    pthread_join(producer, NULL);
    pthread_join(consumer, NULL);

    // if you don't destroy, it persists in the system
    // run the command: ipcs -s
    // to remove: ipcrm -s <sem_id>
    sem_unlink("blank_space");
    sem_unlink("content");
    return 0;
}