#include "shellPrograms.h"
#define FILENAME "output.txt"

/*  A program that prints how many summoned daemons are currently alive */
int shellCheckDaemon_code()
{
   /** TASK 8 **/
   //Create a command that trawl through output of ps -efj and contains "summond"
   char *command = malloc(sizeof(char) * 256);
   sprintf(command, "ps -efj | grep summond | grep -v tty | grep -v pts | grep -v 'grep' > output.txt");

   // TODO: Execute the command using system(command) and check its return value
   // System executes the value
   if (system(command) == -1) {
      exit(EXIT_FAILURE);
   }

   free(command);

   // TODO: Analyse the file output.txt, wherever you set it to be. You can reuse your code for countline program
   // 1. Open the file
   // 2. Fetch line by line using getline()
   // 3. Increase the daemon count whenever we encounter a line
   // 4. Close the file
   // 5. print your result

   int live_daemons = 0;

   // Step 1
   FILE *fp = fopen(FILENAME, "r");

   if (fp == NULL) {
      printf("Error opening file or directory! The specified file might not exist or the process does not have the necessary permissions to read the file.\n");
      return 1;
    }

   size_t size = SHELL_BUFFERSIZE;
   size_t line_size = 0;
   char* line = (char*) malloc(sizeof(char) * size);

   while ((line_size = getline(&line, &size, fp)) != -1) {
      // printf("The value of the line size is %d \n", line_size);
      live_daemons++;
      fwrite(line, line_size, 1, stdout);
   }

   // TODO: Close any file pointers and free any dynamically allocated memory
   free(line);
   line = NULL;
   fclose(fp);

   if (live_daemons == 0) {
      printf("No daemon is alive right now\n");
   }
   else {
      printf("There are in total of %d live daemons \n", live_daemons);
   }

   return 1;
}

int main(int argc, char **args)
{
   return shellCheckDaemon_code();
}
