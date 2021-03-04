/* toctou_prog.c */



#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <time.h>
#include <sys/types.h>
#define DELAY 1

int main(int argc, char * argv[])

{ 
    char * fileName = argv[1];
    char username[64];
    char password[64];
    int i;
    FILE * fileHandler;

    printf("rootprog invoked with process of REAL UID : %d, REAL GID : %d, effective UID: %d\n", getuid(), getgid(), geteuid());
    printf("Please enter the username: ");
    /* get user input */
    scanf("%s", username);
    printf("Please enter the password: ");
    scanf("%s", password);
    
    /**
     * The purpose of calling “access()” system call is to check whether the real user has the “access” permission to the file (provided by the user as a command line argument). -- you can check the calling process' UID and GID using getuid() and getgid()
     * 
     * Once the program has made sure that the real user indeed has the right, the program opens the file and writes the user input into the file.
     * 
     **/

    if(!access(fileName, W_OK)) 
    {
        printf("Access Granted \n");
        /*Simulating the Delay*/ 
        sleep(DELAY); // sleep for 1 secs
        fileHandler = fopen(fileName, "a+");
        if (fileHandler == NULL){
            printf("File cannot be opened\n");
        }
        
        fprintf(fileHandler, "\nPID %d is writing -- ", getpid());
        fwrite(username, sizeof(char), strlen(username), fileHandler);
        fwrite(": ", sizeof(char), 2, fileHandler);  
        fwrite(password, sizeof(char), strlen(password), fileHandler); 
        fwrite("\n", sizeof(char), 1, fileHandler); 
        fclose(fileHandler);
        printf("Exit success\n");
    } 
    else{
        printf("ERROR, permission denied\n");
    }

    return 0;

}

