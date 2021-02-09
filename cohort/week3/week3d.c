#include <sys/wait.h>
#include <sys/types.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char const *argv[])
{
    pid_t pid;

    int shared_int = 10;

    static float shared_float = 25.5;

    pid = fork();

    if (pid < 0)
    {
        fprintf(stderr, "Fork has failed. Exiting now");
        return 1; // exit error
    }
    else if (pid == 0)
    {
        shared_int++;
        printf("shared_int in child process: %d\n", shared_int);
        shared_float = shared_float + 3;
        printf("shared_float in child process: %f\n", shared_float);
    }
    else
    {
        printf("shared_int in parent process: %d\n", shared_int);
        printf("shared_float in parent process: %f\n", shared_float);
        wait(NULL);
        printf("Child has exited.\n");
    }
    return 0;
}