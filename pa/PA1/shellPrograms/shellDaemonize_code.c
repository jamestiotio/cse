/*
 * daemonize.c
 * This example daemonizes a process, writes a few log messages,
 * sleeps 60 seconds and terminates afterwards.
 * NOTE: Technically, this violates the traditional daemon definition since this terminates after a certain period of time, but we don't really care for the sake of this assignment.
 */

// To test compile: gcc Daemonize.c -o summond
// ./summond
// see output at Console : search the log message or process name i.e: summond
// can check using ps aux | grep summond
// for better formatted:  ps -ejf | egrep 'STIME|summond'

#include "shellPrograms.h"

// TODO: Change to appropriate path
// char *path = "/Users/natalie_agus/Dropbox/50.005 Computer System Engineering/2020/PA1 Makeshell Daemon/PA1/logfile_test.txt";
char *path = "";

/*This function summons a daemon process out of the current process*/
static int create_daemon()
{
    /** TASK 7 **/
    // Incantation on creating a daemon with fork() twice

    // 1. Fork() from the parent process
    // 2. Close parent with exit(1)
    // 3. On child process (this is intermediate process), call setsid() so that the child becomes session leader to lose the controlling TTY
    // 4. Ignore SIGCHLD, SIGHUP
    // 5. Fork() again, parent (the intermediate) process terminates
    // 6. Child process (the daemon) set new file permissions using umask(0). Daemon's PPID at this point is 1 (the init)
    // 7. Change working directory to root

    // 8. Close all open file descriptors using sysconf(_SC_OPEN_MAX) and redirect fd 0,1,2 to /dev/null
    // 9. Return to main
    pid_t pid = fork();

    // Child process
    if (pid == 0) {
        // Step 3
        if (setsid() < 0) exit(EXIT_FAILURE);

        // Step 4
        signal(SIGCHLD, SIG_IGN);
        signal(SIGHUP, SIG_IGN);

        // Step 5
        pid_t pid_2 = fork();

        if (pid_2 < 0) {
            fprintf(stderr,"Fork has failed, exiting now...\n");
            exit(EXIT_FAILURE);
        }
        else if (pid_2 > 0) {
            // printf("Closing the parent process right now...\n");
            exit(1);
        }
        else if (pid_2 == 0) {
            // Child process daemon
            // Set file mode and create task
            umask(0);

            // printf("Compiled here...\n");

            // Step 7
            chdir("/");
            
            // Step 8
            // Loop through the filelines
            for (int i = sysconf(_SC_OPEN_MAX); i>=0; i--) {
                close(i);
            }

            int fd0 = open("/dev/null", O_RDWR);
            int fd1 = dup(0);
            int fd2 = dup(0);
        }

    }
    else if (pid > 0) {
        // printf("Closing the parent process right now...\n");
        exit(1);
    }

    else {
        fprintf(stderr,"Fork has failed, exiting now...\n");
        // Exit with error
        exit(EXIT_FAILURE);
    }

    return 1;
}

static int daemon_work()
{

    int num = 0;
    FILE *fptr;

    // Write PID of daemon in the beginning
    fptr = fopen(path, "a");
    if (fptr == NULL)
    {
        return EXIT_FAILURE;
    }

    fprintf(fptr, "%d with FD %d\n", getpid(), fileno(fptr));
    fclose(fptr);

    while (1)
    {

        // Use appropriate location if you are using MacOS or Linux
        // TODO: Change to appropriate path
        fptr = fopen(path, "a");

        if (fptr == NULL)
        {
            return EXIT_FAILURE;
        }

        fprintf(fptr, "PID %d Daemon writing line %d to the file.  \n", getpid(), num);
        num++;

        fclose(fptr);

        sleep(10);

        if (num == 10)
            break;
    }

    return EXIT_SUCCESS;
}

int main(int argc, char **args)
{
    // Do a custom input to set path on runtime
    // Take note that getline() is POSIX-specific
    char *line = NULL;
    size_t len = 0;
    ssize_t read = 0;
    puts("Please specify the absolute path of the file to be used as a log file: ");
    read = getline(&line, &len, stdin);
    path = line;

    create_daemon();

    // printf("Created daemon correctly.\n");
    /* Open the log file */
    openlog("customdaemon", LOG_PID, LOG_DAEMON);
    syslog(LOG_NOTICE, "Daemon started.");
    closelog();

    return daemon_work();
}