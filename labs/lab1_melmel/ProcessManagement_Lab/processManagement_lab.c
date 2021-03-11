#include "processManagement_lab.h"

/**
 * The task function to simulate "work" for each worker process
 * TODO#3: Modify the function to be multiprocess-safe 
 * */
void task(long duration)
{
    
    // simulate computation for x number of seconds
    usleep(duration*TIME_MULTIPLIER);

    // TODO: protect the access of shared variable below
    // update global variables to simulate statistics
    int sem_initialised = sem_wait(sem_global_data);
    if (sem_initialised != 0){
        //printf("Error in sempahore init");
        exit(0);
    }

    ShmPTR_global_data->sum_work += duration;
    ShmPTR_global_data->total_tasks ++;
    if (duration % 2 == 1) {
        ShmPTR_global_data->odd++;
    }
    if (duration < ShmPTR_global_data->min) {
        ShmPTR_global_data->min = duration;
    }
    if (duration > ShmPTR_global_data->max) {
        ShmPTR_global_data->max = duration;
    }

    sem_post(sem_global_data);
}


/**
 * The function that is executed by each worker process to execute any available job given by the main process
 * */

void job_dispatch(int i){

    // TODO#3:  a. Always check the corresponding shmPTR_jobs_buffer[i] for new  jobs from the main process
    //          b. Use semaphore so that you don't busy wait
    //          c. If there's new job, execute the job accordingly: either by calling task(), usleep, exit(3) or kill(getpid(), SIGKILL)
    //          d. Loop back to check for new job 


    // printf("Hello from child %d with pid %d and parent id %d\n", i, getpid(), getppid());
    // exit(0);

    while(true) {
        int sem_initialisation = sem_wait(sem_jobs_buffer[i]); 
        if (sem_initialisation != 0){
            //printf("Error in Initialization: Semaphore");
            exit(0);
        }

        // Starting job

        while (shmPTR_jobs_buffer[i].task_status == 1)
        {
            //printf("Child process %d with pid %d doing job: %c%d\n", i, getpid(), shmPTR_jobs_buffer[i].task_type, shmPTR_jobs_buffer[i].task_duration);
            switch (shmPTR_jobs_buffer[i].task_type)
                {
                case 't':
                    // printf("CHILD: t\n"); // debug
                    task(shmPTR_jobs_buffer[i].task_duration);
                    shmPTR_jobs_buffer[i].task_status = 0;
                    // printf("CHILD: worker %i completed %c\n", i, shmPTR_jobs_buffer[i].task_type); // debug
                    break;
                case 'w':
                    // printf("CHILD: w\n"); // debug
                    usleep(shmPTR_jobs_buffer[i].task_duration * TIME_MULTIPLIER);
                    shmPTR_jobs_buffer[i].task_status = 0;
                    // printf("CHILD: process %i's task status is %i\n", i, shmPTR_jobs_buffer[i].task_status); // debug
                    // printf("CHILD: worker %i completed %c\n", i, shmPTR_jobs_buffer[i].task_type);           // debug
                    break;
                case 'z':
                    // printf("CHILD: z\n"); // debug
                    //printf("Child process %d with pid %d has exited successfully \n",i,getpid());
                    exit(3);
                    shmPTR_jobs_buffer[i].task_status = 0;
                    // printf("CHILD: process %i's task status is %i\n", i, shmPTR_jobs_buffer[i].task_status); // debug
                    // printf("CHILD: worker %i completed %c\n", i, shmPTR_jobs_buffer[i].task_type);           // debug
                    break;
                case 'i':
                    // printf("CHILD: i\n"); // debug
                    
                    // printf("CHILD: about to kill id %i\n", getpid());                                        // debug
                    // printf("CHILD: process %i's task status is %i\n", i, shmPTR_jobs_buffer[i].task_status); // debug
                    kill(getpid(), SIGKILL);
                    shmPTR_jobs_buffer[i].task_status = 0;
                    // printf("CHILD: worker %i completed %c\n", i, shmPTR_jobs_buffer[i].task_type); // debug
                    break;
                }
                

        }
    }

}

/** 
 * Setup function to create shared mems and semaphores
 * **/
void setup(){

    // TODO#1:  a. Create shared memory for global_data struct (see processManagement_lab.h)
    //          b. When shared memory is successfully created, set the initial values of "max" and "min" of the global_data struct in the shared memory accordingly
    // To bring you up to speed, (a) and (b) are given to you already. Please study how it works. 

    //          c. Create semaphore of value 1 which purpose is to protect this global_data struct in shared memory 
    //          d. Create shared memory for number_of_processes job struct (see processManagement_lab.h)

    //          e. When shared memory is successfully created, setup the content of the structs (see handout)
    //          f. Create number_of_processes semaphores of value 0 each to protect each job struct in the shared memory. Store the returned pointer by sem_open in sem_jobs_buffer[i]
    //          g. Return to main

    ShmID_global_data = shmget(IPC_PRIVATE, sizeof(global_data), IPC_CREAT | 0666);
    //printf("The shared memory global data is %d\n" ,ShmID_global_data);
    //the shmget returns an int

    if (ShmID_global_data == -1){
        //printf("Global data shared memory creation failed\n");
        exit(EXIT_FAILURE);
    }
    ShmPTR_global_data = (global_data *) shmat(ShmID_global_data, NULL, 0);

    if ((int) ShmPTR_global_data == -1){
        //printf("Attachment of global data shared memory failed \n");
        exit(EXIT_FAILURE);
    }

    //set global data min and max
    ShmPTR_global_data->max = -1;
    ShmPTR_global_data->min = INT_MAX;

    //          c. Create semaphore of value 1 which purpose is to protect this global_data struct in shared memory 
    sem_global_data = sem_open("semglobaldata",O_CREAT | O_EXCL, 0644,1);
    while(1){
        if (sem_global_data == SEM_FAILED){
            sem_unlink("semglobaldata");
            sem_global_data = sem_open("semglobaldata",O_CREAT | O_EXCL, 0644,1);

            //printf("The while loop is running");
        }
        else{
            break;
        }
    }

    ShmID_jobs = shmget(IPC_PRIVATE, sizeof(job)*number_of_processes, IPC_CREAT | 0666);
    //printf("the shared memory id of the jobs  %d \n",ShmID_jobs);
    
    if (ShmID_jobs == -1){
        //printf("Failed to create shared memory\n");
        exit(EXIT_FAILURE);
    }

    shmPTR_jobs_buffer = (job *) shmat(ShmID_jobs, NULL, 0);
    if ((int) shmPTR_jobs_buffer == -1){
        //printf("Fail to attach the memory to this address space \n");
        exit(EXIT_FAILURE);
    }

    for (int i = 0; i < number_of_processes; i++)
    {
        //use the following semaphore names for the job buffer
        char *semjob_name = malloc(sizeof(char)*16);
        sprintf(semjob_name,"semjobs%d",i);
        // create and assign semaphore "sem_jobs_buffer[i]" with initial value 0
        //sem open is used for IPC which is this in this case.
        sem_jobs_buffer[i] = sem_open(semjob_name, O_CREAT | O_EXCL, 0644, 0);
        // check if failed, if does, recreate: unlink, open
        while (true)
        {
            if (sem_jobs_buffer[i] == SEM_FAILED)
            {
                // unlink, might have name clash
                sem_unlink(semjob_name);

                // create again
                sem_jobs_buffer[i] = sem_open(semjob_name, O_CREAT | O_EXCL, 0644, 0);
            }
            else{
                break;
                }
        }
        //free(semjobsi);
    }

    return;
    }

/**
 * Function to spawn all required children processes
 **/
 
void createchildren(){
    // TODO#2:  a. Create number_of_processes children processes
    //          b. Store the pid_t of children i at children_processes[i]
    //          c. For child process, invoke the method job_dispatch(i)
    //          d. For the parent process, continue creating the next children
    //          e. After number_of_processes children are created, return to main 

    //number_of_processes;
    //pid_t children_processes[MAX_PROCESS]; // id of all child processes
    //remember to use fork()
    //test with other test cases before submitting

    pid_t pid;
    for(int i = 0; i<number_of_processes;i++){
        pid = fork();

        if(pid == 0){
            job_dispatch(i);
            exit(0);
        }

        else if(pid > 0){
            children_processes[i] = pid;
            //printf("fork is successful \n");}
        }
        else{
            //fprintf(stderr,"Fork has failed, exiting now");
            return;
        }
}
}

/**
 * The function where the main process loops and busy wait to dispatch job in available slots
 * */
void main_loop(char* fileName){

    // load jobs and add them to the shared memory
    FILE* opened_file = fopen(fileName, "r");
    char action; //stores whether its a 'p' or 'w'
    long num; //stores the argument of the job 

    while (fscanf(opened_file, "%c %ld\n", &action, &num) == 2) { //while the file still has input

        //TODO#4: create job, busy wait
        //      a. Busy wait and examine each shmPTR_jobs_buffer[i] for jobs that are done by checking that shmPTR_jobs_buffer[i].task_status == 0. You also need to ensure that the process i IS alive using waitpid(children_processes[i], NULL, WNOHANG). This WNOHANG option will not cause main process to block when the child is still alive. waitpid will return 0 if the child is still alive. 
        //      b. If both conditions in (a) is satisfied update the contents of shmPTR_jobs_buffer[i], and increase the semaphore using sem_post(sem_jobs_buffer[i])
        //      c. Break of busy wait loop, advance to the next task on file 
        //      d. Otherwise if process i is prematurely terminated, revive it. You are free to design any mechanism you want. The easiest way is to always spawn a new process using fork(), direct the children to job_dispatch(i) function. Then, update the shmPTR_jobs_buffer[i] for this process. Afterwards, don't forget to do sem_post as well 
        //      e. The outermost while loop will keep doing this until there's no more content in the input file. 

        pid_t pid;
        bool task = false;

        while (true){
            // First, check for any available job with active worker
            //printf("checking for active worker\n");
            for (int i = 0; i < number_of_processes; i++){
                if (waitpid(children_processes[i], NULL, WNOHANG) == 0 && shmPTR_jobs_buffer[i].task_status == 0)
                {
                    
                    shmPTR_jobs_buffer[i].task_status = 1;
                    shmPTR_jobs_buffer[i].task_type = action;
                    shmPTR_jobs_buffer[i].task_duration = num;

                    sem_post(sem_jobs_buffer[i]);
                    task = true;
                    break;
                }
            }

            // Finished checking all processes, if taskFinished is true then we do not have to check for terminated worker processes
            if (task){
                break;
            }

            // Else, loop through all processes and check for illegally terminated workers
            for (int i = 0; i < number_of_processes; i++){
                int status;
                int alive = waitpid(children_processes[i], &status, WNOHANG);

                if (alive != 0){
                    // Spawn new worker to take over current one
                    //printf("Found an illegal process\n");

                    shmPTR_jobs_buffer[i].task_status = 0;
                    pid = fork();

                    // Check for error
                    if (pid > 0){
                        children_processes[i] = pid;
                        shmPTR_jobs_buffer[i].task_status = 1;
                        shmPTR_jobs_buffer[i].task_type = action;
                        shmPTR_jobs_buffer[i].task_duration = num;
                        sem_post(sem_jobs_buffer[i]);
                        task = true;
                        break;

                    //fprintf(stderr, "Process creation has failed");
                    return; // exit error
                    } 

                    else if (pid < 0){
                        //fprintf(stderr, "Process creation has failed");
                        return;
                    }

                    else {
                        job_dispatch(i);
                        exit(0);
                    } 
                }
            }

            // Break out of while loop to read the next line
            if (task){
                break;
            }
        }   

    }
    fclose(opened_file);

    //printf("Main process is going to send termination signals\n");

    // TODO#4: Design a way to send termination jobs to ALL worker that are currently alive

    bool isalive;

    while (true){
        isalive = false;
        // Keep looping until we are able to terminate all alive processes
        for (int i = 0; i < number_of_processes; i++){
            // Check that the worker is still alive
            if (waitpid(children_processes[i], NULL, WNOHANG) == 0){
                isalive = true;

                // Check if we need to send a termination signal
                if (shmPTR_jobs_buffer[i].task_status == 0){
                    shmPTR_jobs_buffer[i].task_status = 1;
                    shmPTR_jobs_buffer[i].task_type = 'z';
                    shmPTR_jobs_buffer[i].task_duration = 0;
                    sem_post(sem_jobs_buffer[i]);
                    //printf("Child process %d with pid %d has exited successfully\n",i,getpid());
                }
            }
        }

        if (isalive == false){
            break;
        }

    }


    //wait for all children processes to properly execute the 'z' termination jobs
    int process_waited_final = 0;
    pid_t wpid;
    while ((wpid = wait(NULL)) > 0){
        printf("WAITING\n");
        process_waited_final ++;
    }
    
    // print final results
    printf("Final results: sum -- %ld, odd -- %ld, min -- %ld, max -- %ld, total task -- %ld\n", ShmPTR_global_data->sum_work, ShmPTR_global_data->odd, ShmPTR_global_data->min, ShmPTR_global_data->max, ShmPTR_global_data->total_tasks);
}

void cleanup(){
    //TODO#4: 
    // 1. Detach both shared memory (global_data and jobs)
    // 2. Delete both shared memory (global_data and jobs)
    int detach_status = shmdt((void *) ShmPTR_global_data); 
    detach_status = shmdt((void *) shmPTR_jobs_buffer);
    int remove_status = shmctl(ShmID_global_data, IPC_RMID, NULL); 
    remove_status = shmctl(ShmID_jobs, IPC_RMID, NULL); 

    // 3. Unlink all semaphores in sem_jobs_buffer

    //unlink all semaphores before exiting process
    int sem_close_status = sem_unlink("semglobaldata");
    
    for (int i = 0; i<number_of_processes; i++){
        char *sem_name = malloc(sizeof(char)*16);

        sprintf(sem_name, "semjobs%d", i);

        sem_close_status = sem_unlink(sem_name);
        
        free(sem_name);
    }

    return;


}

// Real main

int main(int argc, char* argv[]){

    // printf("Lab 1 Starts...\n");

    struct timeval start, end;
    long secs_used,micros_used;

    //start timer
    gettimeofday(&start, NULL);

    //Check and parse command line options to be in the right format
    if (argc < 2) {
        printf("Usage: sum <infile> <numprocs>\n");
        exit(EXIT_FAILURE);
    }


    //Limit number_of_processes into 10. 
    //If there's no third argument, set the default number_of_processes into 1.  
    if (argc < 3){
        number_of_processes = 1;
    }
    else{
        if (atoi(argv[2]) < MAX_PROCESS) number_of_processes = atoi(argv[2]);
        else number_of_processes = MAX_PROCESS;
    }

    setup();
    createchildren();
    main_loop(argv[1]);

    //parent cleanup
    cleanup();

    //stop timer
    gettimeofday(&end, NULL);

    double start_usec = (double) start.tv_sec * 1000000 + (double) start.tv_usec;
    double end_usec =  (double) end.tv_sec * 1000000 + (double) end.tv_usec;

    printf("Your computation has used: %lf secs \n", (end_usec - start_usec)/(double)1000000);


    return (EXIT_SUCCESS);
}
