#include <stdio.h>
#include <stdlib.h>

int x[100]; // static array with global scope
static int y[100]; // static array with file scope
char *sentence = "Hello world"; // static array with global scope, constant, readonly

char* function2() {
    int z[100]; // automatic array (local, no clear documentation on where it is implemented, but likely in stack instead of heap)
    char *sentence = "HELLO"; // static read-only array
    char sentence2[] = "WORLD"; // automatic array, in stack
    return sentence;
}

int main(int argc, char const *argv[]) {
    // print the string "HELLO" pointed by sentence
    function2();
}