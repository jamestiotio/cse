#include <sys/wait.h>
#include <sys/types.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char const *argv[])
{
    int level = 3;
    pid_t pid[level];

    for (int i = 0; i < level; i++)
    {
        pid[i] = fork();

        if (pid[i] < 0)
        {
            fprintf(stderr, "Fork has failed. Exiting now");
            return 1; // exit error
        }
        else if (pid[i] == 0)
        {
            printf("Hello from child %d \n", i);
        }
    }

    return 0;
}