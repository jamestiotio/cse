#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#define NUMQUESTIONS 1

int main()
{

    int i = 1;
    char full_command[64] = {};
    char full_filename_answer[64] = {};

    char command_1[] = "./q1 ./testcases/q1_";
    char command_2[] = ".txt > answer.txt";
    char filename_answer_p1[] = "./testcases/answerq1_";
    char filename_answer_p2[] = ".txt";

    int correct_answers = 0;

    while (i <= NUMQUESTIONS)
    {
        sprintf(full_command, "%s", command_1);
        sprintf(full_command + sizeof(command_1) - 1, "%d", i);
        sprintf(full_command + sizeof(command_1), "%s", command_2);

        sprintf(full_filename_answer, "%s", filename_answer_p1);
        sprintf(full_filename_answer + sizeof(filename_answer_p1) - 1, "%d", i);
        sprintf(full_filename_answer + sizeof(filename_answer_p1), "%s", filename_answer_p2);

        int result = system(full_command);
        if (result != 0)
        {
            perror("Command fails. Try again");
            exit(0);
        }

        FILE *answer = fopen(full_filename_answer, "r+");
        FILE *student_answer = fopen("answer.txt", "r+");
        
        int state = 0;
        if (answer != NULL && student_answer != NULL)
        {
            state = 1;
            // compare char by char
            int student_answer_char = 0;
            int answer_char = 0;
            while (state)
            {
                student_answer_char = getc(student_answer);
                answer_char = getc(answer);
                if (student_answer_char != answer_char)
                {
                    state = 0;
                }
                else if (student_answer_char == EOF && answer_char == EOF)
                {
                    break;
                }
            }
        }

        if (state)
        {
            correct_answers++;
        }
        i++;
    }

    printf("For Q1: You have scored %d/%d \n", correct_answers, NUMQUESTIONS);
}