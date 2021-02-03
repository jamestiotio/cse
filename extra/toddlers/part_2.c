#include "cclass.h"

int* testfunction(int input) {
    int y = 6;
    char x = 'k';
    int *x_array = malloc(sizeof(int) * input);
    return x_array;
}

float multiply(float a, float b) {
    return a * b;
}

int sum(int* array, int size){
    int sum_value = 0;
    for (int i = 0; i<size; i++){
        sum_value += array[i];
    }
    return sum_value;
}

int min(int* array, int size){
    int min_value = INT64_MAX;
    for (int i = 0; i<size; i++) {
        if (array[i] < min_value) {
            min_value = array[i];
        }
    }
    return min_value;
}

int max(int* array, int size){
    int max_value = INT64_MIN;
    for (int i = 0; i<size; i++) {
        if (array[i] > max_value) {
            max_value = array[i];
        }
    }
    return max_value;
}

void func(int (*f)(int *, int), int *array)
{
    printf("return value is : %d \n", (*f)(array, 10));
}

void* special_function(int arg){
    if (arg == 0){
        char* c = malloc(sizeof(char));
        c[0] = 'a';
        return c;
    }
    else{
        int* i = malloc(sizeof(int));
        i[0] = 128;
        return i;
    }
}

int main(int argc, char** argv) {
    int x = 5;
    int* x_pointer = &x; // address of x
    printf("%d", *x_pointer); // dereference the pointer, so print the value (NOT the address)

    int array[10] = {1,2,3,4,5,6,7,8,9,10};

    func(sum_function_pointer, array);
    func(min_function_pointer, array);

    //call the function using pointer
    int result = sum_function_pointer(array, 10);

    printf("The result is %d \n", result);

    char another_input;
    printf("Please enter a number: \n");
    scanf("%s", &another_input);

    int user_input = atoi(&another_input);

    printf("User input is %d \n", user_input);
    // select a function based on user input
    function_pointers[user_input](array, 10);

    char* c = (char *)special_function(0);
    int* i = (int *)special_function(1);
    printf("Result of special function with arg 0 : %c \n", *c);
    printf("Result of special function with arg 1 : %d \n", *i);

    free(c);
    free(i);

    //check arguments
    if (argc < 2)
    {
        printf("Please key in filename\n");
        return 0;
    }

    for (int i = 0; i<argc; i++){
        printf("Argument %d is: %s \n", i, argv[i]);
    }

    //open the file, with flag of O_RDONLY if you only want to read
    int fd = open(argv[1], O_RDONLY);

    //error checking
    if (fd < 0)
    {
        perror("Failed to open file. \n");
        exit(1);
    }

    //initiallize a character array to contain what you will read later
    char char_buffer[128];

    //byte offset
    int byte = 0;

    //read 1 byte by 1 byte, put it into the buffer
    int check_read = read(fd, char_buffer + byte, 1);

    //keep reading 1 byte until nothing else to read
    while (check_read > 0)
    {
        byte++;
        check_read = read(fd, char_buffer + byte, 1);
    }

    printf("Called read() system call.  %d bytes  were read.\n", byte);

    //add terminating character so that you can print it
    char_buffer[byte] = '\0';

    printf("Those bytes are as follows: %s \n", char_buffer);

    //close the file
    close(fd);

    //check arguments
    if (argc < 3)
    {
        printf("Please key in filename\n");
        return 0;
    }

    int fd2 = open(argv[2], O_RDWR|O_CREAT|O_APPEND, 0666); // To clear and overwrite the file, use O_TRUNC option instead
    char sentence_to_write[128] = "Hello, test writing to file \n";

    int check_write = write(fd2, sentence_to_write, strlen(sentence_to_write));

    //error checking
    if (check_write < 0){
        perror("Failed to write \n");
        exit(1);
    }

    close(fd2);

    FILE *out = fopen(argv[2], "a"); 
    if (out != NULL){
        fprintf(out, "%s", "Hello Again! \n"); 
        fclose(out);
    }
    else{
        perror("File failed to be opened\n");
        return 0;
    }

    // After reading a file, if the same file need to be processed again, reset the pointer to the first line of the file
    // lseek(file_descriptor, BYTE_absolute_location, SEEK_SET)

    //open the file, with flag of O_RDONLY if you only want to read
    int another_fd = open(argv[1], O_RDONLY);

    //error checking
    if (another_fd < 0)
    {
        perror("Failed to open file. \n");
        exit(1);
    }

    int n = 5;
    int* ptr = (int*)malloc(n * sizeof(int));
    // Check if the memory has been successfully
    // allocated by malloc or not
    if (ptr == NULL) {
        printf("Memory not allocated.\n");
        exit(0);
    }

    return 0;
}