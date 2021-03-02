#include <stdio.h>
#include <stdlib.h>


int main() {
    char str1[20];

    printf("Enter name: ");
    scanf("%s", str1);

    printf("Name entered : %s \n", str1);
    fprintf(stdout, "EXIT! \n");
    return 0;
}