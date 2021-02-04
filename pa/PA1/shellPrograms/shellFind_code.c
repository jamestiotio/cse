#include "shellPrograms.h"
/*
 List all files matching the name in args[1] under current directory and subdirectories
*/
int shellFind_code(char** args){

	if (args[1] == NULL){ 
		printf("Usage: find keyword\n");
		return 1;
	}

	char* dir_name = args[0];
	char* toMatch = args[1];
	char dirname[SHELL_BUFFERSIZE];

	DIR* d;
    /* Open the directory specified by "dir_name". */
    if (strcmp(dir_name, "find") == 0){
    	d = opendir (".");
    	// getcwd(dirname, SHELL_BUFFERSIZE);
    	sprintf(dirname, ".");
    	dir_name = dirname;
    }
    else{
    	d = opendir(dir_name);
    }

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

        /* Print the name of the file and directory if it matches the keyword */
        if(strstr(d_name, toMatch)!=NULL){
        	printf ("%s/%s\n", dir_name, d_name);
        }

        if (entry->d_type & DT_DIR) {

            /* Check that the directory is not "d" or d's parent. */
            if (strcmp (d_name, "..") != 0 &&
                strcmp (d_name, ".") != 0) {
                int path_length;
                char path[PATH_MAX];
 
                path_length = snprintf (path, 1024,
                                        "%s/%s", dir_name, d_name);

                if (path_length >= PATH_MAX) {
                    fprintf (stderr, "Path length has got too long.\n");
                    exit (EXIT_FAILURE);
                }
                /* Recursively call "list_dir" with the new path. */
                args[0] = path;
                shellFind_code(args);
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
    return shellFind_code(args);
}