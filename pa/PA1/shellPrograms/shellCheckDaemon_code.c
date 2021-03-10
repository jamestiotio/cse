#include "shellPrograms.h"

/*  A program that prints how many summoned daemons are currently alive */
int shellCheckDaemon_code()
{
   /** TASK 8 **/
   //Create a command that trawl through output of ps -efj and contains "summond"
   char *command = malloc(sizeof(char) * 256);
   sprintf(command, "ps -efj | grep summond  | grep -v tty > output.txt");

   // TODO: Execute the command using system(command) and check its return value
   //system executes the value
   do{
      if(system(command)!=-1){
         break;
      }   
   }while(1);

   int live_daemons = 0;
   // TODO: Analyse the file output.txt, wherever you set it to be. You can reuse your code for countline program
   // 1. Open the file
   // 2. Fetch line by line using getline()
   // 3. Increase the daemon count whenever we encounter a line
   // 4. Close the file
   // 5. print your result

   //TODO 1.
   FILE *fp;
   fp = fopen("output.txt","r");

   if (fp == NULL) {
      printf("Error opening file or directory! The specified file might not exist or the process does not have the necessary permissions to read the file.\n");
      return 1;
    }
   
   size_t size = SHELL_BUFFERSIZE;;
   size_t line_size = 0;
   char* line = (char*) malloc(sizeof(char) * size);;

   do{
      //printf("The value of the line size is %d \n",line_size);
      live_daemons ++;
      fwrite(line,line_size,1,stdout);

   }while((line_size = getline(&line,&size,fp))!=-1 );

   fclose(fp);

   if (live_daemons == 0){
      printf("No daemon is alive right now\n");
   }
   else
   {
      printf("There are in total of %d live daemons \n", live_daemons);
   }


   // TODO: close any file pointers and free any statically allocated memory 

   free(command);
   free(line);
   return 1;
}

int main(int argc, char **args)
{
   return shellCheckDaemon_code();
}