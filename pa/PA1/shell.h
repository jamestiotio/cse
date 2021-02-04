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


#define SHELL_BUFFERSIZE 256
#define SHELL_INPUT_DELIM " \t\r\n\a"
#define SHELL_OPT_DELIM "-"

/*
  List of builtin commands, followed by their corresponding functions.
 */
const char *builtin_commands[] = {
  "cd", // calls shellCD
  "help", // calls shellHelp
  "exit", // calls shellExit
  "usage", // calls shellUsage
  "display", // calls shellDisplayFile
  "countline", // calls shellCountline
  "listdir", // calls shellListDir
  "listdirall", // calls shellListDirAll
  "find", // calls shellFind
  "summond", // calls shellSummond
  "checkdaemon" // calls shellCheckDaemon
};

int numOfBuiltinFunctions() {
  return sizeof(builtin_commands) / sizeof(char *);
};


/*
The fundamental functions of the shell interface
*/
void shellLoop(void);
char **shellTokenizeInput(char *line);
char *shellReadLine(void);
int shellExecuteInput(char **args);


/*
Functions of the shell interface
*/
int shellCD(char **args);
int shellHelp(char **args);
int shellExit(char **args);
int shellUsage (char** args);
int shellDisplayFile(char** args);
int shellCountLine(char** args);
int shellListDir(char** args);
int shellListDirAll(char** args);
int shellFind(char** args);
int shellSummond(char** args);
int shellCheckDaemon(char** args);

/*This is array of functions, with argument char***/
int (*builtin_commandFunc[]) (char **) = {
  &shellCD, //builtin_commandFunc[0]
  &shellHelp, //builtin_commandFunc[1]
  &shellExit, //builtin_commandFunc[2]
  &shellUsage,//builtin_commandFunc[3]
  &shellDisplayFile, //builtin_commandFunc[4]
  &shellCountLine, //builtin_commandFunc[5]
  &shellListDir,//builtin_commandFunc[6]
  &shellListDirAll,//builtin_commandFunc[7]
  &shellFind,//builtin_commandFunc[8]
  &shellSummond, //builtin_commandFunc[9]
  &shellCheckDaemon //builtin__commandFunc[10]
};



