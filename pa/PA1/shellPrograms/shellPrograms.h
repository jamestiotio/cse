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
#define _GNU_SOURCE


#define SHELL_BUFFERSIZE 256
#define SHELL_INPUT_DELIM " \t\r\n\a"
#define SHELL_OPT_DELIM "-"

/*
Implemented functions of the shell interface
*/
int shellDisplayFile_code(char** argv);
int shellCountLine_code(char** argv);
int shellListDir_code(char** argv);
int shellListDirAll_code(char** argv);
int shellFind_code(char** argv);
int shellCheckDaemon_code();