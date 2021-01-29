#include "functions.h"

#define BUFFERLENGTH 64

int main(){
   print_hello();

   printf("Key in a number to obtain its factorial:\n");
   char inputNum[BUFFERLENGTH];

   fgets(inputNum, BUFFERLENGTH, stdin);

   int num = atoi(inputNum);
   printf("The factorial of %d is %d \n", num, factorial(5));

   printf("The 32-bit binary representation of %d is ", num);
   decimalToBinary(num);
   printf("\n");

   return 0;
}