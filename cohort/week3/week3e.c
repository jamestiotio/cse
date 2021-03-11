#include <sys/wait.h>
#include <sys/types.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/ipc.h>
#include <sys/shm.h>

int main(int argc, char const *argv[])
{
    pid_t pid;
    int *ShmPTR;
    int ShmID;

    /**
         This process asks for a shared memory of 4 bytes (size of 1 int) and attaches this
        shared memory segment to its address space.
    **/

    // There are limits (in the form of kernel parameters) to how much shared memory you can allocate (SHMMAX, SHMMIN, SHMALL, etc)   
    ShmID = shmget(IPC_PRIVATE, 1 * sizeof(int), IPC_CREAT | 0666);
    if (ShmID < 0)
    {
        printf("*** shmget error (server) ***\n");
        exit(1);
    }

    /**
         SHMAT attach the shared memory to the process
        This code is called before fork() so both parent
        and child processes have attached to this shared memory.

        Pointer ShmPTR contains the address to the shared memory segment.
    **/

    ShmPTR = (int *)shmat(ShmID, NULL, 0);

    if ((int)ShmPTR == -1)
    {
        printf("*** shmat error (server) ***\n");
        exit(1);
    }
    printf("Parent process has created a shared memory segment.\n");

    pid = fork();

    if (pid < 0)
    {
        fprintf(stderr, "Fork has failed. Exiting now");
        return 1; // exit error
    }
    else if (pid == 0)
    {
        *ShmPTR = *ShmPTR + 1; // dereference ShmPTR and increase its value
        printf("shared_int in child process: %d\n", *ShmPTR);
    }
    else
    {
        printf("shared_int in parent process: %d\n", *ShmPTR); // race condition
        wait(NULL); // move this above the print statement to see the change in ShmPTR value
        printf("Child has exited.\n");
    }
    return 0;
}