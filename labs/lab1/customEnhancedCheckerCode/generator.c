#include <time.h>
#include "constants.h"

/**
 * Return a random number between 0 and limit inclusive (uniform distribution without any skew).
 * Source: https://stackoverflow.com/a/2999130
 */
int rand_lim(int limit) {
    int divisor = RAND_MAX / (limit + 1);
    int retval;

    do { 
        // This is just a quick hack so it is cryptographically insecure (use libsodium if a secure random integer is needed).
        // More information here: https://stackoverflow.com/a/39475626
        retval = rand() / divisor;
    } while (retval > limit);

    return retval;
}

int main()
{
    printf("Starting auto-generation of input files...\n");
    srand(time(NULL));
    char task_types[3] = {'t', 'w', 'i'};   // 'z' is not included here since it is used by the main code to legally terminate processes (input files will not have 'z' task type)

    for (int k = 0; k < TOTAL_FILES; k++) {
        printf("Generating file: input%d\n", k);
        int num_of_lines = rand_lim(MAX_NUMBER_OF_LINES);
        char *filename = (char*) malloc(sizeof(char) * 256);
        sprintf(filename, "./customEnhancedCheckerCode/input%d", k);
        FILE* opened_file = fopen(filename, "w+");

        for (int i = 0; i < num_of_lines; i++) {
            char task_type = task_types[rand_lim(2)];
            int duration = rand_lim(MAX_DURATION);
            fprintf(opened_file, "%c%d\n", task_type, duration);
        }

        fclose(opened_file);
    }
    return 0;
}