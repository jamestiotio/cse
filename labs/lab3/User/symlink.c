#include <stdio.h>
#include <unistd.h>
#include <string.h>

/**
 * Turns the file whose path in argv[1] as a symbolic link to the file with path defined 
 * as argv[2] 
 **/
int main(int argc, char * argv[])
{
    if (argc < 3){
        printf("Usage: ./symlink <symlinkpath> <destinationpath>\n");
        return 1;
    }
    unlink(argv[1]);
    if (symlink(argv[2], argv[1]) == 0){
        return 0;
    }
    else{
        printf("Symlink fails \n");
        return 1;
    }
}



