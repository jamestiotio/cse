#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>


int main() {
    int fd1, fd2, n;
    char str[100];
    /* open an input file in read mode */
    fd1 = open("input.txt", O_RDONLY, 0);
    /* create an output file with read write permissions, 0666 is the file permission created, the 0 in front means OCTAL notation --- 666 is the octal notation 

    If you want to create a directory, the same permission logic can be made also, but directories ARE EXECUTABLES:
        if (mkdir("myFolder", 0777) == -1)
            cerr << "Error :  " << strerror(errno) << endl;

        else
            cout << "Directory created!";
    */
    fd2 = open("output.txt", O_CREAT | O_RDWR, 0666);
    /* read the data from input file and write it to output file */
    while ((n = read(fd1, str, 10)) > 0) {
            write(fd2, str, n);
    }
    /* move the cursor to the 13th byte of the input file */
    lseek(fd1, 12, 0);
    /* write the given text in output file */
    write(fd2, "\nMoved cursor to 12th bytes\n", 28);
    /* writes the contents of input file from 12 byte to EOF */
    while ((n = read(fd1, str, 10)) > 0) {
            write(fd2, str, n);
    }
    /* close both input and output files */
    close(fd1);
    close(fd2);
    return 0;
}