#include <string.h>
#include <limits.h>
#include "constants.h"

int main()
{
    printf("Initiating ruthless checker...\n");
    int correct = 0;
    const int loop_times = TOTAL_TEST / TOTAL_FILES;
    char *answers[TOTAL_FILES] = {}; // Content is the starting address of each answer string

    // Generate answer strings from generated input files
    for (int k = 0; k < TOTAL_TEST / loop_times; k++) {
        char *answer = (char*) malloc(sizeof(char) * 512);
        int sum = 0, odd = 0, min = INT_MAX, max = -1, total = 0;
        char *filename = (char*) malloc(sizeof(char) * 256);
        sprintf(filename, "./customEnhancedCheckerCode/input%d", k);
        FILE* opened_file = fopen(filename, "r");
        char action; // Stores whether it is a 't', 'w', 'z' or 'i'
        long num; // Stores the argument of the job
        while (fscanf(opened_file, "%c %ld\n", &action, &num) == 2) {
            if (action == 't') {
                sum += num;
                total += 1;

                if (num % 2 == 1) {
                    odd += 1;
                }

                if (num < min) {
                    min = num;
                }

                if (num > max) {
                    max = num;
                }
            }
        }
        fclose(opened_file);
        free(filename);
        sprintf(answer, "Final results: sum -- %d, odd -- %d, min -- %d, max -- %d, total task -- %d\n", sum, odd, min, max, total);
        answers[k] = answer;
    }

    char command[256];
    char out_cmd_buf[256];

    int num_of_procs[TOTAL_TEST / TOTAL_FILES] = {3, 10, 420, 69};

    // Run code through all generated input files with different number_of_processes parameter every time
    for (int j = 0; j < loop_times; j++) {
        int i = 0;
        while (i < TOTAL_TEST / loop_times)
        {
            sprintf(command, "cat ./customEnhancedCheckerCode/input");
            sprintf(command + 37, "%d", i);
            if (i > 9)
            {
                sprintf(command + 39, " > input");
            }
            else
            {
                sprintf(command + 38, " > input");
            }
            printf("command content: %s \n", command);

            system(command);

            snprintf(out_cmd_buf, sizeof(out_cmd_buf), "./out input %d > answer.txt", num_of_procs[j]);
            system(out_cmd_buf);

            FILE *fp = fopen("answer.txt", "r+");

            // Read the first line which is the answer
            char *line = NULL;

            size_t length = 0;
            ssize_t read = getline(&line, &length, fp);

            if (read != -1)
            {
                if (strcmp(line, answers[i]) == 0)
                {
                    printf("Answer is correct\n");
                    correct++;
                }
                else
                {
                    printf("Wrong answer\n");
                }
            }
            fclose(fp);
            i++;
        }
    }
    printf("You scored %d/%d\n", correct, TOTAL_TEST);

    return 0;
}