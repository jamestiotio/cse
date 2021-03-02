#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>

int main() {
    int fd1, fd2, fd3, n;
    char str;

    /* open an input file in read mode */
    fd1 = open("input.txt", O_RDONLY, 0);
    // fd2 will be the LOWEST available fd, which is 4
    fd2 = dup(fd1);
    fd3 = dup2(fd1, 9); // custom fd 9

    /* read the data from fd1 */
    printf("From fd1: ");
    int counter = 0;
    while ((n = read(fd1, &str, 1)) > 0) {
            printf("%c", str);
            counter++;
            if (counter > 10) break;
    }
    printf("\n");

    /* read the data from fd2 */
    printf("From fd2: ");
    counter = 0;
    while ((n = read(fd2, &str, 1)) > 0) {
            printf("%c", str);
            counter++;
            if (counter > 10) break;
    }
    printf("\n");

    int fd4 = open("input.txt", O_RDONLY, 0);
    /* read back the data from fd1 */
    printf("From fd4: ");
    counter = 0;
    while ((n = read(fd4, &str, 1)) > 0) {
            printf("%c", str);
            counter ++;
            if (counter > 10) break;
    }
    printf("\n");
}