#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <sys/wait.h>
#include <sys/types.h>

// chmod 744 logaccess
// only ROOT can read, write, execute 
// no SUID bit set

int main(int argc, char* argv[]){

    char * message = argv[1];
    FILE * fileHandler;

    char * fileName = "../Root/rootlogfile.txt";

    fileHandler = fopen(fileName, "a+");
    if (fileHandler == NULL){
        printf("Root logfile cannot be opened\n");
        return 1;
    }

    fprintf(fileHandler, "\nPID %d is writing -- ", getpid());
    fprintf(fileHandler, "%s", message);
    fprintf(fileHandler, "\n");
    fclose(fileHandler);
    printf("Root log write success\n");

    return 0;

}