#include "shellPrograms.h"

int shellListDirAll_code(char** args)
{
	char* dir_name = args[1];
	char dirname[SHELL_BUFFERSIZE];
	
    DIR * d;

    /* Open the directory specified by "dir_name". */
    if (dir_name == NULL || strcmp(dir_name,"-a") == 0){
    	d = opendir (".");
    	// getcwd(dirname, SHELL_BUFFERSIZE);
    	sprintf(dirname, ".");
    	dir_name = dirname;
    }
    else
    	d = opendir(dir_name);

    /* Check it was opened. */
    if (! d) {
        fprintf (stderr, "Cannot open directory '%s': %s\n",
                 dir_name, strerror (errno));
        return 1;
    }

    while (1) {
        struct dirent * entry;
        const char * d_name;

        /* "Readdir" gets subsequent entries from "d". */
        entry = readdir (d);
        if (! entry) {
            /* There are no more entries in this directory, so break
               out of the while loop. */
            break;
        }
        d_name = entry->d_name;

        /* Print the name of the file and directory. */
        printf ("%s/%s\n", dir_name, d_name);


        if (entry->d_type & DT_DIR) {

            /* Check that the directory is not "d" or d's parent. */
            if (strcmp (d_name, "..") != 0 &&
                strcmp (d_name, ".") != 0) {
                int path_length;
                char path[PATH_MAX];
 
                path_length = snprintf (path, PATH_MAX,
                                        "%s/%s", dir_name, d_name);
                printf ("%s\n", path);
                if (path_length >= PATH_MAX) {
                    fprintf (stderr, "Path length has got too long.\n");
                    exit (EXIT_FAILURE);
                }
                /* Recursively call "list_dir" with the new path. */
                args[1] = path;
                shellListDirAll_code(args);
            }
  }
    }
    /* After going through all the entries, close the directory. */
    if (closedir (d)) {
        fprintf (stderr, "Could not close '%s': %s\n",
                 dir_name, strerror (errno));
    }

    return 1;
}

int main(int argc, char** args){
    printf("I am called by execvp\n");
    return shellListDirAll_code(args);
}