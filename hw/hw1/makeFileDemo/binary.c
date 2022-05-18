#include "functions.h"

void decimalToBinary(int num) {
    // Initialize mask
    unsigned int mask = 0x80000000;
    size_t bits = sizeof(num) * CHAR_BIT;

    for (int count = 0; count < bits; count++) {
        // print
        (mask & num) ? printf("1") : printf("0");

        // shift one to the right
        mask = mask >> 1;
    }
}