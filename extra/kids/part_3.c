#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <pthread.h>

#include <sys/ipc.h>
#include <sys/shm.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>

//prepare an array of pthread_t (its thread id)
pthread_t tid;

/*
A struct creation for example
*/
typedef struct Coordinate
{
    int x;
    int y;
    int id;
} Vec2D;

void *functionForThread(void* args) {
    //cast the argument into Vec2D type, because we know thats what we fed in as argument in pthread_create
    Vec2D *myPoint_pointer = (Vec2D *)args;

    //accessing argument data through pointer
    printf("Hello from thread id %d! The coordinate passed is %d, %d \n",  myPoint_pointer->id, myPoint_pointer->x, myPoint_pointer->y);

    //sleep for 2 seconds
    sleep(2);

    //modify the argument
    myPoint_pointer->x = myPoint_pointer->x + 10;
    myPoint_pointer->y = myPoint_pointer->y + 10;

    //cast it back to void* as that's what we are supposed to return 
    return (void*) myPoint_pointer;
}

int child_process_function(int* array, int size, int id){
    printf("Hello from child number %d with pid %d!\n", id, getpid());
    int answer = 0;
    for (int i = 0; i<size; i++){
        answer += array[i];
        array[i] += 10;
    }

    printf("Answer is %d\n", answer);
    return answer;
}

int main() {
    Vec2D point;
    point.x = 1;
    point.y = 2;
    point.id = 0;

    int thread_error_check = pthread_create(&tid, NULL, functionForThread, &point);

    //check error
    if (thread_error_check != 0)
    {
        perror("Failed to create thread. \n");
        exit(1);
    }

    printf("Main thread is waiting...\n");
    void* threadReturn = NULL;

    // second argument: the address of a variable whose content is a pointer to the answer
    // void**: address of a void* pointer
    thread_error_check = pthread_join(tid, &threadReturn); //blocks until thread returns

    if (thread_error_check != 0)
    {
        perror("Failed to join. \n");
        exit(1);
    }

    //cast it to Vec2D pointer type
    Vec2D *threadReturnPointerCasted = (Vec2D *)threadReturn;

    //print its content
    printf("A thread has terminated. The return coordinate is %d, %d \n", threadReturnPointerCasted->x, threadReturnPointerCasted->y);

    // Demo of race condition
    pthread_t another_tid[5];

    Vec2D another_point[5];
    for (int i = 0; i < 5; i++)
    {
        another_point[i].x = 1;
        another_point[i].y = 2;
        another_point[i].id = i;
        int another_thread_error_check = pthread_create(&another_tid[i], NULL, functionForThread, &another_point[i]);
        //check error
        if (another_thread_error_check != 0)
        {
            perror("Failed to create thread. \n");
            exit(1);
        }
    }

    printf("Main thread is waiting...\n");
    void *anotherThreadReturn = NULL;

    for (int i = 0; i < 5; i++)
    {
        int another_thread_error_check = pthread_join(another_tid[i], &anotherThreadReturn);

        if (another_thread_error_check != 0)
        {
            perror("Failed to join. \n");
            exit(1);
        }

        //cast it to Vec2D pointer type
        Vec2D *anotherThreadReturnPointerCasted = (Vec2D *)anotherThreadReturn;

        //print its content
        printf("A thread with id %d has terminated. The return coordinate is %d, %d \n", anotherThreadReturnPointerCasted->id, anotherThreadReturnPointerCasted->x, anotherThreadReturnPointerCasted->y);
    }

    int forkReturnValue;

    int array[10] = {1,2,3,4,5,6,7,8,9,10};

    pid_t myPID = getpid();
    printf("The main process id is %d \n", myPID);
    
    forkReturnValue = fork();
    
    //error checking
    if (forkReturnValue < 0){
        perror("Failed to fork. \n");
        exit(1);
    }

    //child process
    if (forkReturnValue == 0){
        //child process will have forkReturnValue of 0
        child_process_function(array, 10,0);
    }
    else
    {
        //parent process will have forkReturnValue of > 0, which is the pid of the child
        //wait for a child (any 1)
        pid_t childPid = wait(NULL);
        printf("Child process has finished. Main process exiting\n");
    }

    printf("The address of the array in pid %d starts at %p \n", getpid(), array);
    printf("The value of the array in pid %d is : ", getpid());
    for(int i = 0; i<10 ;i++){
        printf(" %d ", array[i]);
    }
    printf("\n");

    for (int i = 0; i < 5; i++)
    {
        forkReturnValue = fork();

        //error checking
        if (forkReturnValue < 0)
        {
            perror("Failed to fork. \n");
            exit(1);
        }

        //child process
        if (forkReturnValue == 0)
        {
            //child process will have forkReturnValue of 0
            child_process_function(array, 10, i);
            break; //dont create more children!
        }
    }

    //executed by parent process, since the forkReturnValue will retain the pid of the last child created
    if (forkReturnValue != 0)
    {

        while(wait(NULL) > 0); //wait for all children
        printf("Children processes has all finished. Main process exiting\n");
    }

    printf("The address of the array in pid %d starts at %p \n", getpid(), array);
    printf("The value of the array in pid %d is : ", getpid());
    for (int i = 0; i < 10; i++)
    {
        printf(" %d ", array[i]);
    }
    printf("\n");

    //1. allocate shared memory, get its id
    int ShmID = shmget(IPC_PRIVATE, sizeof(Vec2D), S_IRUSR | S_IWUSR);
    //2. attach to this process’ address space
    Vec2D* ShmPTR = (Vec2D *)shmat(ShmID, NULL, 0); // get Vec2D from the Thread part, same struct used
    //init to zero
    ShmPTR->x = 0;
    ShmPTR->y = 0;

    int pid = fork();
    if (pid < 0)
    {
        printf("*** fork error (server) ***\n");
        exit(1);
    }
    else if (pid == 0)
    {
        printf("From pid %d, x and y value is (%d, %d) \n", getpid(), ShmPTR->x, ShmPTR->y);
        //sleep, hoping parent will finish by then
        sleep(5);
        printf("From pid %d, new x and y value is (%d, %d) \n", getpid(), ShmPTR->x, ShmPTR->y);
        //child change the shared memory value
        ShmPTR->x = 5;
        ShmPTR->y = 5;
        exit(0); //child exits
    }

    //sleep, hoping child will finish printing by then
    sleep(1);
    //parent code
    ShmPTR->x = 10;
    ShmPTR->y = 10;
    wait(NULL); //wait for child
    printf("From pid %d, new x and y value is (%d, %d) \n", getpid(), ShmPTR->x, ShmPTR->y);

    //3. detach shared memory
    shmdt((void *)ShmPTR);
    //4. delete shared memory
    shmctl(ShmID, IPC_RMID, NULL);
}