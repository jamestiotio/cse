// THIS FILE IS TO TEST UNMASK AS IT WAS not working on my system 
#include <sys/wait.h>
#include <sys/types.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h> 
#include <dirent.h>
#include <errno.h>
/* "readdir" etc. are defined here. */
#include <dirent.h>
/* limits.h defines "PATH_MAX". */
#include <limits.h>
#include <ctype.h>

#include <signal.h>
#include <sys/stat.h>
#include <syslog.h>
#include <time.h>
#include <fcntl.h>
#include <sys/resource.h>

int main() {
    umask(0);
    printf("Managed to set file permissions to 0777");
    return 1;
}