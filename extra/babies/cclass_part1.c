#include "cclass.h"

int main(int argc, char** argv) {
    printf("Constant BUFFERSIZE has a value of %d \n", BUFFERSIZE);

    int x = 5;
    float y = 3.0;
    char a = 'a';
    char b = 'b';
    char c = 'c';

    printf("Printing integer x: %d \n", x);
    printf("Printing float y: %f \n", y);
    printf("Printing characters abc: %c %c %c \n", a,b,c);
    printf("Printing characters as ASCII: %d %d %d \n", a,b,c);

    printf("Size of int is %d bytes, size of float is %d bytes, size of char is %d bytes\n", sizeof(int), sizeof(float), sizeof(char));

    int vector_int[3] = {1,2,3};
    float vector_float[3] = {0.3,0.4,0.5};
    char characters[5] = {'a','i','u','e','o'};

    printf("Contents of vector_int %d %d %d \n", vector_int[0], vector_int[1], vector_int[2]);
    printf("Contents of vector_float %f %f %f \n", vector_float[0], vector_float[1], vector_float[2]);
    printf("Contents of the second char: %c\n", characters[1]);

    int *vector_int_pointer = vector_int;
    printf("Address of vector_int array is 0x%llx\n", vector_int_pointer);
    printf("Address of the first element in vector_int array is 0x%llx\n", &vector_int[0]);
    printf("Address of the second element in vector_int array is 0x%llx\n", &vector_int[1]);
    printf("Address of the third element in vector_int array is 0x%llx\n", &vector_int[2]);

    printf("Printing address using pointer : \n");
    printf("Address of the first element in vector_int array is 0x%llx\n", vector_int_pointer);
    printf("Address of the second element in vector_int array is 0x%llx\n", vector_int_pointer + 1);
    printf("Address of the third element in vector_int array is 0x%llx\n", vector_int_pointer + 2);

    //change the second element of vector_int
    printf("The original second element is %d\n", vector_int_pointer[1]);
    vector_int_pointer[1] = 5;
    printf("The new second element is %d\n", vector_int_pointer[1]);
    printf("The new second element is %d\n", *(vector_int_pointer+1));

    int z = 5;

    // int* z_pointer is equivalent to int *z_pointer
    int *z_pointer = &z; // z_pointer's content is an ADDRESS of an integer (thus, z_pointer is an integer pointer)

    printf("Value of z is %d \n", z);
    printf("Z is stored in address 0x%llx\n", z_pointer);
    printf("The pointer to Z is stored in address 0x%llx\n", &z_pointer);
    printf("Size of Z pointer is: %d \n", sizeof(z_pointer));

    // change value of z through pointer
    *z_pointer = 6;
    printf("The new value of z is %d\n", *z_pointer);

    char hello_world[12] = {'h','e','l','l','o',' ','w','o','r','l','d'};
    printf("%s", hello_world);

    char hello_world[12] = {'h','e','l','l','o',' ','w','o','r','l','d', '\0'};
    printf("%s\n", hello_world);

    //allocates in a read-only portion of static memory, NOT modifiable, READ only
    char *hello_world_readonly = "hello world";
    printf("%s\n", hello_world_readonly);

    printf("Size of hello_world_better pointer %d\n",sizeof(hello_world_readonly));

    char hello_world_init[] = "hello world";
    //change the letter in the string
    hello_world_init[1] = 'u';
    printf("The new string is %s\n", hello_world_init);

    //allocates in a read-only portion of static memory, NOT modifiable, READ only
    char *hello_world_readonly = "hello world";
    printf("%s\n", hello_world_readonly);

    hello_world_readonly[1] = 'u'; //this results in unpredictable behavior
    printf("The new string is %s\n", hello_world_readonly);

    char hello_world_array[12] = "hello world";
    char *hello_world_pointer = hello_world_array;

    printf("hello_world_pointer: %d\n", hello_world_pointer)
    printf("hello_world_array: %d\n", hello_world_array)

    char sentence[BUFFERSIZE] = "";
    sprintf(sentence, "Hello World");
    printf("The sentence is: %s \n", sentence);
    sprintf(sentence, "This is another sentence overwriting the previous one. Lets write a number %d. ", 5);
    printf("The sentence now is modified to: %s \n", sentence);
    char sentence_append[64] = "The quick brown fox jumps over a lazy dog";
    strcat(sentence, sentence_append);
    printf("%s \n", sentence);

    // defining struct
    struct Vector_Int {
        int x;
        int y;
        int z;
        char name[64];
    };

    // structure variable declaration, empty member values
    struct Vector_Int v1;

    // manual member initialization
    v1.x = 2;
    v1.y = 3;
    v1.z = 10;
    sprintf(v1.name, "Vector 1");

    // structure variable auto member initialization
    struct Vector_Int v2 = {3,5,11, "Vector 2"};

    printf("Values of v1 is x:%d y:%d z:%d name: %s\n", v1.x, v1.y, v1.z, v1.name);
    printf("Values of v2 is x:%d y:%d z:%d name: %s\n", v2.x, v2.y, v2.z, v2.name);

    struct Info {
        char name[32];
        int age;
        struct address {
            char area_name[32];
            int house_no;
            char district[32];
        } address;
    };

    struct Info my_Info = {"Alice", 25, "Somapah Road", 8, "Upper Changi"};

    printf("Name: %s, age %d, area name %s, house number %d, district %s\n", my_Info.name, my_Info.age, my_Info.address.area_name, my_Info.address.house_no, my_Info.address.district);

    struct address my_Addrs = {"Another Road", 15, "Lower Changi"};
    printf("Another address %s %d %s \n", my_Addrs.area_name, my_Addrs.house_no, my_Addrs.district);

    struct address {
        char area_name[32];
        int house_no;
        char district[32];
    };

    struct Info {
        char name[32];
        int age;
        struct address address; //now this is a member
    };

    struct Info my_Info = {"Alice", 25, "Somapah Road", 8, "Upper Changi"};

    printf("Name: %s, age %d, area name %s, house number %d, district %s\n", my_Info.name, my_Info.age, my_Info.address.area_name, my_Info.address.house_no, my_Info.address.district);

    struct address my_Addrs = {"Another Road", 15, "Lower Changi"};
    printf("Another address %s %d %s \n", my_Addrs.area_name, my_Addrs.house_no, my_Addrs.district);

    struct Vector_Int{
        int x;
        int y;
        int z;
        char name[64];
    };

    struct Vector_Int vector_sample;

    printf("Size of Vector_Int struct is %d bytes\n", sizeof(struct Vector_Int));
    printf("Size of its members are x %d bytes, y %d bytes, z %d bytes, and name %d bytes\n", sizeof(vector_sample.x), sizeof(vector_sample.y), sizeof(vector_sample.z), sizeof(vector_sample.name));

    struct Info many_info[3] = {{"Alice", 25, "Somapah Road", 8, "Upper Changi"},
                                {"Bob", 22, "Somapah Road", 19, "Upper Changi"},
                                {"Michael", 30, "Another Road", 25, "East Changi"}};

    for (int i = 0; i < 3; i++) {
        printf("Name: %s, age %d, area name %s, house number %d, district %s\n", many_info[i].name, many_info[i].age, many_info[i].address.area_name, many_info[i].address.house_no, many_info[i].address.district);
    }

    typedef struct Info InfoData;
    InfoData many_info[3] = {{"Alice", 25, "Somapah Road", 8, "Upper Changi"},
                                {"Bob", 22, "Somapah Road", 19, "Upper Changi"},
                                {"Michael", 30, "Another Road", 25, "East Changi"}};

    for (int i = 0; i < 3; i++) {
        printf("Name: %s, age %d, area name %s, house number %d, district %s\n", many_info[i].name, many_info[i].age, many_info[i].address.area_name, many_info[i].address.house_no, many_info[i].address.district);
    }

    float array_floats[8];
    for (int i = 0; i<8; i++) {
        array_floats[i] = (float) i/8;
        printf("%f, ", array_floats[i]);
    }
    printf("\n");

    int i = 0;
    while (i < 8) {
        array_floats[i] += 0.5f;
        printf("%f, ", array_floats[i]);
        i ++;
    }
    printf("\n");

    i = 0;
    do {
        array_floats[i] -= 0.5f;
        printf("%f, ", array_floats[i]);
        i ++;
    }
    while (i < 8);
    printf("\n");

    for (int i = 0; i<128; i++) {
        char c = i;
        printf("%c ", c);
    } // c does not exist out of the for-loop scope

    char c;
    for (int i = 0; i<128; i++) {
        c = i;
        printf("%c ", c);
    }
    //c exists, as 127
    printf("final c: %c.\n", c); //its a space

    float output = square(3.f);
    printf("Output is %f \n", output);

    Vector v1 = {3,7,10};
    printf("Address of v1 members: 0x%llx, 0x%llx, 0x%llx\n", &v1.x, &v1.y, &v1.z);
    print_vector(v1);
    v1 = clear_vector(v1);
    print_vector(v1);

    Vector v2 = {31,99,21};
    printf("Address of v2 members: 0x%llx, 0x%llx, 0x%llx\n", &v2.x, &v2.y, &v2.z);
    print_vector(v2);
    clear_vector_byreference(&v2);
    print_vector(v2);

    printf("The global variable is %d \n", global_variable);
    test_global();
    printf("The global variable is now %d \n", global_variable);
    printf("The static variable is %d \n", test_static());
    printf("The static variable is %d \n", test_static());
    printf("The local variable is %d \n", test_local());
    printf("The local variable is %d \n", test_local());

    // POINTER and SIZE comes together!
    int buffersize;
    printf("Enter total number of elements: ");
    scanf("%d", &buffersize);

    //allocates memory in heap
    int *x = (int*) malloc(sizeof(int)*buffersize); //type cast it
    //print the address x is pointing to
    printf("Memory address allocated by malloc starts at 0x%llx\n", x);
    //print the address of the pointer x
    printf("This pointer is stored at address 0x%llx\n", &x);

    // do something with the array
    for (int i = 0; i < buffersize; i++) {
        x[i] = i;
    }

    printf("Enter additional number of elements: ");
    scanf("%d", &buffersize);

    //resize the array, buffersize can be smaller than original amount. The remainder is automatically freed
    //the unused memory initially pointed by x is also automatically freed
    int *y = realloc(x, buffersize);
    printf("Memory address allocated by realloc starts at 0x%llx\n", y);
    printf("Memory address allocated by realloc ends at 0x%llx\n", y + buffersize);
    printf("This new pointer is stored at address 0x%llx\n", &y);
    for (int i = 0; i < buffersize; i++) {
        printf("Original content element %d is %d \n", i, x[i]);
        x[i] += i; //do something with the array
    }

    //free heap manually
    free(y);
    free(x);

    //persistence
    test();
    printf("test_pointer: %d, %d, %d\n", test_pointer[0], test_pointer[1], test_pointer[2]);
    free(test_pointer);

    int *pointer = test_malloc(10);
    printf("Returned pointer is at address 0x%llx \n", &pointer);
    printf("Pointer is pointing to address 0x%llx \n", pointer);
    // test print content
    for (int i = 0; i<10; i++) {
        printf("%d ", pointer[i]);
    }
    printf("\n");

    //free the memory allocated
    free(pointer);

    int buffersize;
    printf("Enter total number of elements: ");
    scanf("%d", &buffersize);

    //allocates memory in heap
    int *x = (int*) malloc(sizeof(char)*buffersize); //type cast it

    //initialize to some value
    printf("The original array value is : ");
    for (int i = 0; i < buffersize; i++) {
        x[i] = i;
        printf("%d ", x[i]);
    }
    printf("\n");

    //pass it to the function to modify
    modify_array(x, buffersize);

    //print its content
    printf("The new array value is : ");
    for (int i = 0; i < buffersize; i++) {
            printf("%d ", x[i]);
    }   
    printf("\n");

    //free it
    free(x);

    // At the end of the third call of function1, the final value of x is 8.
    function1();
    function1();
    function1();
}

// if not defined in the header file, then need to define this function BEFORE being called in the main() function
float square(float a) {
    return a * a;
}

void print_vector(Vector input) {
    printf("{x:%d, y:%d, z:%d}\n", input.x, input.y, input.z);
}

Vector clear_vector(Vector input) {
    printf("Address of clear_vector input members: 0x%llx, 0x%llx, 0x%llx\n", &input.x, &input.y, &input.z);
    input.x = 0;
    input.y = 0;
    input.z = 0;
    return input;
}

void clear_vector_byreference(Vector *input) {
    printf("Address of clear_vector_byreference input members: 0x%llx, 0x%llx, 0x%llx\n", &input->x, &input->y, &input->z);
    input->x = 0;
    input->y = 0;
    input->z = 0;
}

int test_static(void) {
    static int static_variable = 20;
    static_variable += 1;
    return static_variable;
}

int test_local(void) {
    int local_variable = 20;
    local_variable += 1;
    return local_variable;
}

void test_global(void) {
    global_variable++;
}

void test() {
    int size = 3;
    int init_value = 10;
    test_pointer = (int*) malloc(sizeof(int) * size);
    // setting all values to something
    for (size_t i = 0; i < size; i++) {
        test_pointer[i] = init_value + i;
    }
    printf("test_pointer: %d, %d, %d\n", test_pointer[0], test_pointer[1], test_pointer[2]);
}

int* test_malloc(int size_array) {
    int *x_local = malloc(sizeof(int) * size_array);
    for (int i = 0; i < size_array; i++) {
        x_local[i] = i * i;
    }
    printf("Local pointer is at address 0x%llx\n", &x_local);
    printf("Pointer is pointing to address 0x%llx \n", x_local);
    return x_local;
}

void modify_array(int* array, int array_size) {
    for (int i = 0; i < array_size; i++) {
        array[i] += i;
}

/*
Within a function, a static variable is one whose memory location is
preserved between function calls. It is static in that it is initialized
only once and retains its value between function calls.
*/
int function1() {
    static int x = 5;
    x += 1;
    printf("Value of x: %d \n", x);
    return 0;
}