/*
 * daemonize.c
 * This example daemonizes a process, writes a few log messages,
 * sleeps 60 seconds and terminates afterwards.
 */

// To test compile: gcc Daemonize.c -o summond
// ./summond
// see output at Console : search the log message or process name i.e: summond
// can check using ps aux | grep summond
// for better formatted:  ps -ejf | egrep 'STIME|summond'

#include "shellPrograms.h"

// TODO: change to appropriate path
//char *path = "/Users/natalie_agus/Dropbox/50.005 Computer System Engineering/2020/PA1 Makeshell Daemon/PA1/logfile_test.txt";
char *path = "/Users/caramelmel/cse/pa/PA1/systemlog.txt";

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

    //child process 
    if(pid == 0){
        //TOOO 3 CLEAR NOW ?! I caught you not sleeping at 3 am 
        setsid();

        //TODO 4 SEE HOW MUCH IM READING HMPH
        signal(SIGCHLD, SIG_IGN);
        signal(SIGHUP, SIG_IGN);

        //TODO 5 
        pid_t pid_2 = fork();

        if(pid_2< 0 ){
            fprintf(stderr,"Fork has failed exiting now\n");
            return 1;
        }
        else if(pid_2 > 0){
            //printf("Closing the parent process right now\n");
            exit(1);
        }
        else if (pid_2 == 0){
            //child process daemon
            //set file mode and create task
            //DEBUGGING NEEDED: the error on the compiler 
            umask(0);

            //THIS IS TO TEST FOR DEBUGGING AS UNMASK THREW AN ERROR ON MY COMPILER
            //printf("compiled here\n");

            //TASK 7
            chdir("/");
            
            //TASK 8 
            //loop through the filelines 
            for(int i = sysconf(_SC_OPEN_MAX);i>=0;i--){
                close(i);
            }
            int fd0 = open("/dev/null", O_RDWR);
            int fd1 = dup(0);
            int fd2 = dup(0);
        }

    }
    else if(pid > 0){
        //TO INDICATE TO YOU this is part 2
        //printf("closing the parent process right now\n");
        exit(1);
    }

    else{
        fprintf(stderr,"Fork has failed, exiting now\n");
        //exit with error
        return 1;
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
    create_daemon();

    //printf("created daemon correctly\n");
    /* Open the log file */
    openlog("customdaemon", LOG_PID, LOG_DAEMON);
    syslog(LOG_NOTICE, "Daemon started.");
    closelog();

    return daemon_work();
}