#include "shellPrograms.h"

/*
	List the items in the directory
*/
int shellListDir_code(char **args)
{
    if (args[1] != NULL)
    {
        char *token = strtok(args[1], SHELL_OPT_DELIM);
        printf("Token is %s\n", token);

        if (token != NULL)
        {
            if (strcmp(token, "a") == 0)
            {
                //call listdirall,
                //execvp still need the ./shellPrograms because this was called
                //by a process that was at the .. directory
                if (execvp("./shellPrograms/listdirall", args) == -1)
                {
                    perror("Failed to execute, command is invalid.");
                }
                return 1;
            }
            else
            {
                printf("Invalid option. Use -a to display all files within the current directory and its subdirectories.\n");
            }
        }
    }

    // print out all the contents of the directory using opendir() function
    DIR *d;
    struct dirent *dir;
    d = opendir(".");
    if (d)
    {
        while ((dir = readdir(d)) != NULL)
        {
            printf("%s\n", dir->d_name);
        }
        closedir(d);
    }
    else
    {
        printf("Directory doesn't exist. \n");
    }

    return 1;
}

int main(int argc, char **args)
{
    return shellListDir_code(args);
}