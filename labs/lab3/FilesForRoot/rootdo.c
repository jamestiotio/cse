#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <sys/wait.h>
#include <sys/types.h>
#include <termios.h>

// Usage: ./rootdo ./logaccess testLogfromRootdo
int main(int argc, char* argv[]){

    char * execName = argv[1];
    char * filename = argv[2];
    char * argv_new[3] = {execName, filename, NULL};
    char password[9];

    printf("Exec name is %s, with filename %s \n", execName, filename);
    printf("Please enter your password: ");


    /*Routine to stop echoing the scanf input back to the terminal temporarily*/
    struct termios term, term_orig;
    tcgetattr(STDIN_FILENO, &term);
    term_orig = term;
    term.c_lflag &= ~ECHO;
    tcsetattr(STDIN_FILENO, TCSANOW, &term);

    /* get user input */
    scanf("%s", password);
 
    /* Remember to set back, or your commands won't echo! */
    tcsetattr(STDIN_FILENO, TCSANOW, &term_orig);

    if (!strcmp(password, "password")){ //on success, returns 0
        printf("Login granted\n");
        int pid = fork();
        if (pid == 0){
            printf("Fork success\n");
            wait(NULL);
            printf("Children returned\n");
        }
        else{
            if(execvp(execName, argv_new) == -1){
                perror("Executable not found\n");
            }
        }
    }
    else
    {
        printf("Login fail, exiting now.\n");
    }

    return 0;

}