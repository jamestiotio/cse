#include <sys/wait.h>
#include <sys/types.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char const *argv[])
{
    pid_t pid;

    pid = fork();
    printf("pid: %d\n", pid);

    if (pid < 0)
    {
        fprintf(stderr, "Fork has failed. Exiting now");
        return 1; // exit error
    }
    else if (pid == 0)
    {
        execlp("/bin/ls", "ls", NULL);
    }
    else
    {
        wait(NULL);
        printf("Child has exited.\n");
    }
    return 0;
}