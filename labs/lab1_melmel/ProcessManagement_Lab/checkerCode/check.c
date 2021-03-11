#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#define TOTAL_TEST 2

int main()
{
    int correct = 0;
    char *answer_0 = "Final results: sum -- 3, odd -- 1, min -- 3, max -- 3, total task -- 1\n";
    char *answer_1 = "Final results: sum -- 6, odd -- 2, min -- 1, max -- 3, total task -- 3\n";
    char *answers[2] = {answer_0, answer_1}; //content is the starting address of each answer string

    int i = 0;
    char command[128];

    while (i < TOTAL_TEST)
    {
        sprintf(command, "cat ./checkerCode/input");
        sprintf(command + 23, "%d", i);
        if (i > 9)
        {
            sprintf(command + 25, " > input");
        }
        else
        {
            sprintf(command + 24, " > input");
        }
        printf("command content: %s \n", command);

        system(command);

        system("./out input 3 > answer.txt");

        FILE *fp = fopen("answer.txt", "r+");

        //read the first line which is the answer
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
        i++;
    }
    printf("You scored %d/%d\n", correct, TOTAL_TEST);

    return 0;
}