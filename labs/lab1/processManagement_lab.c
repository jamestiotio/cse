#include "processManagement_lab.h"

/**
 * The task function to simulate "work" for each worker process
 * TODO#3: Modify the function to be multiprocess-safe 
 */
void task(long duration)
{
    // Simulate computation for x number of seconds
    usleep(duration*TIME_MULTIPLIER);

    // TODO#3: Protect the access of shared variable below
    // Update global variables to simulate statistics
    sem_wait(sem_global_data);
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
 */
void job_dispatch(int i) {
    // TODO#3:  a. Always check the corresponding shmPTR_jobs_buffer[i] for new jobs from the main process
    //          b. Use semaphore so that you don't busy wait
    //          c. If there's new job, execute the job accordingly: either by calling task(), usleep, exit(3) or kill(getpid(), SIGKILL)
    //          d. Loop back to check for new job 

    while (true) {
        int return_code = sem_wait(sem_jobs_buffer[i]);

        if (return_code == -1) {
            printf("Something has gone terribly wrong with sem_wait as it has failed.");
        }

        if (shmPTR_jobs_buffer[i].task_status == 1)
        {
            // printf("Child process %d with process ID %d doing job: %c%d\n", i, getpid(), shmPTR_jobs_buffer[i].task_type, shmPTR_jobs_buffer[i].task_duration);
            switch (shmPTR_jobs_buffer[i].task_type)
            {
            case 't':
                task(shmPTR_jobs_buffer[i].task_duration);
                shmPTR_jobs_buffer[i].task_status = 0;
                break;
            case 'w':
                usleep(shmPTR_jobs_buffer[i].task_duration * TIME_MULTIPLIER);
                shmPTR_jobs_buffer[i].task_status = 0;
                break;
            case 'z':
                // printf("Child process %d with process ID %d has exited successfully.\n", i, getpid());
                shmPTR_jobs_buffer[i].task_status = -1;
                exit(3);
                break;
            case 'i':
                // printf("ILLEGAL task %c%d encountered. Child process %d with process ID %d has exited PREMATURELY.\n", shmPTR_jobs_buffer[i].task_type, shmPTR_jobs_buffer[i].task_duration, i, getpid());
                kill(getpid(), SIGKILL);
                break;
            default:
                printf("An unknown task type has appeared! This code does not know how to deal with this encounter. Crashing...\n");
                kill(getpid(), SIGKILL);
                break;
            }
        }
    }

    // printf("Hello from child %d with pid %d and parent id %d\n", i, getpid(), getppid());
    // exit(0);
}

/** 
 * Setup function to create shared mems and semaphores
 */
void setup() {
    // TODO#1:  a. Create shared memory for global_data struct (see processManagement_lab.h)
    //          b. When shared memory is successfully created, set the initial values of "max" and "min" of the global_data struct in the shared memory accordingly
    // To bring you up to speed, (a) and (b) are given to you already. Please study how it works. 

    //          c. Create semaphore of value 1 which purpose is to protect this global_data struct in shared memory 
    //          d. Create shared memory for number_of_processes job struct (see processManagement_lab.h)
    //          e. When shared memory is successfully created, setup the content of the structs (see handout)
    //          f. Create number_of_processes semaphores of value 0 each to protect each job struct in the shared memory. Store the returned pointer by sem_open in sem_jobs_buffer[i]
    //          g. Return to main

    ShmID_global_data = shmget(IPC_PRIVATE, sizeof(global_data), IPC_CREAT | 0666);
    if (ShmID_global_data == -1) {
        printf("Global data shared memory creation failed\n");
        exit(EXIT_FAILURE);
    }
    ShmPTR_global_data = (global_data *) shmat(ShmID_global_data, NULL, 0);
    if ((int) ShmPTR_global_data == -1) {
        printf("Attachment of global data shared memory failed \n");
        exit(EXIT_FAILURE);
    }

    // Set global data min and max
    ShmPTR_global_data->max = -1;
    ShmPTR_global_data->min = INT_MAX;

    // Create a named semaphore with name "semglobaldata" of init size 1 (semaphores must be unique by name)
    sem_global_data = sem_open("semglobaldata", O_CREAT | O_EXCL, 0644, 1);

    // Check if semaphore creation has failed
    while (true) {
        if (sem_global_data == SEM_FAILED) {
            // Try to unlink since chances are it failed because there's already a semaphore with the specified name (maybe from a previously executed program, etc.)
            sem_unlink("semglobaldata");
            // Try to open again
            sem_global_data = sem_open("semglobaldata", O_CREAT | O_EXCL, 0644, 1);
        }
        else {
            break;
        }
    }

    ShmID_jobs = shmget(IPC_PRIVATE, sizeof(job) * number_of_processes, IPC_CREAT | 0666);
    if (ShmID_jobs == -1) {
        exit(EXIT_FAILURE);
    }
    shmPTR_jobs_buffer = (job *) shmat(ShmID_jobs, NULL, 0);
    if ((int) shmPTR_jobs_buffer == -1) {
        exit(EXIT_FAILURE);
    }

    shmPTR_jobs_buffer->task_status = 0;

    for (int i = 0; i < number_of_processes; i++) {
        // We use 16 bytes because of these (follow the same method as testmain_todo1; minimum is 8 bytes to avoid occasional segmentation faults due to attempted illegal RAM's protected memory access):
        // - https://stackoverflow.com/a/40679845
        // - https://stackoverflow.com/a/17298238
        // Employ proper non-sloppy memory management techniques here
        char *sem_name = malloc(sizeof(char) * 16);
        sprintf(sem_name, "semjobs%d", i);
        sem_jobs_buffer[i] = sem_open(sem_name, O_CREAT | O_EXCL, 0644, 0);
        while (true) {
            if (sem_global_data == SEM_FAILED) {
                sem_unlink(sem_name);
                sem_jobs_buffer[i] = sem_open(sem_name, O_CREAT | O_EXCL, 0644, 0);
            }
            else {
                break;
            }
        }
        free(sem_name);
    }

    return;
}

/**
 * Function to spawn all required children processes
 */
void createchildren() {
    // TODO#2:  a. Create number_of_processes children processes
    //          b. Store the pid_t of children i at children_processes[i]
    //          c. For child process, invoke the method job_dispatch(i)
    //          d. For the parent process, continue creating the next children
    //          e. After number_of_processes children are created, return to main 

    for (int i = 0; i < number_of_processes; i++) {
        pid_t pid;
        pid = fork(); // Both the parent and the child return from the fork()
        children_processes[i] = pid;

        if (pid < 0) {
            // Some error happens
            exit(-1);
        }
        else if (pid == 0) {
            // Child process
            job_dispatch(i);
        }
        else {
            // Parent process
            continue;
        }
    }

    return;
}

/**
 * The function where the main process loops and busy wait to dispatch job in available slots
 */
void main_loop(char* fileName){
    // Load jobs and add them to the shared memory
    FILE* opened_file = fopen(fileName, "r");
    char action; // Stores whether it is a 't', 'w', 'z' or 'i'
    long num; // Stores the argument of the job

    while (fscanf(opened_file, "%c %ld\n", &action, &num) == 2) { // While the file still has input
        // TODO#4: Create job, busy wait
        //      a. Busy wait and examine each shmPTR_jobs_buffer[i] for jobs that are done by checking that shmPTR_jobs_buffer[i].task_status == 0. You also need to ensure that the process i IS alive using waitpid(children_processes[i], NULL, WNOHANG). This WNOHANG option will not cause main process to block when the child is still alive. waitpid will return 0 if the child is still alive. 
        //      b. If both conditions in (a) is satisfied update the contents of shmPTR_jobs_buffer[i], and increase the semaphore using sem_post(sem_jobs_buffer[i])
        //      c. Break of busy wait loop, advance to the next task on file 
        //      d. Otherwise if process i is prematurely terminated, revive it. You are free to design any mechanism you want. The easiest way is to always spawn a new process using fork(), direct the children to job_dispatch(i) function. Then, update the shmPTR_jobs_buffer[i] for this process. Afterwards, don't forget to do sem_post as well.
        //      e. The outermost while loop will keep doing this until there's no more content in the input file. 

        bool job_is_assigned = false;

        while (job_is_assigned == false) {
            for (int i = 0; i < number_of_processes; i++) {
                int alive = waitpid(children_processes[i], NULL, WNOHANG);

                if (alive == 0 && shmPTR_jobs_buffer[i].task_status == 0) {
                    shmPTR_jobs_buffer[i].task_type = action;
                    shmPTR_jobs_buffer[i].task_duration = num;
                    shmPTR_jobs_buffer[i].task_status = 1;
                    job_is_assigned = true;
                    sem_post(sem_jobs_buffer[i]);
                    break;
                }
                else if (alive != 0) {
                    pid_t pid = fork();
                    children_processes[i] = pid;

                    if (pid < 0) {
                        printf("Failed to fork() and child %d was not created.", i);
                        exit(-1);
                    }
                    else if (pid == 0) {
                        job_dispatch(i);
                        break;
                    }
                    else {
                        shmPTR_jobs_buffer[i].task_type = action;
                        shmPTR_jobs_buffer[i].task_duration = num;
                        shmPTR_jobs_buffer[i].task_status = 1;
                        job_is_assigned = true;
                        sem_post(sem_jobs_buffer[i]);
                        break;
                    }
                }
            }
        }
    }
    fclose(opened_file);

    // printf("Main process is going to send termination signals\n");

    // TODO#4: Design a way to send termination jobs to ALL worker that are currently alive 

    int count = 0;
    while (count < number_of_processes) {
        count = 0; // Reset each time to ensure that all processes are either marked for 'z' or already terminated
        for (int k = 0; k < number_of_processes; k++)
        {
            int alive = waitpid(children_processes[k], NULL, WNOHANG);
            if (alive == 0 && shmPTR_jobs_buffer[k].task_status == 0) {
                // Send legal termination signal to the corresponding processes
                shmPTR_jobs_buffer[k].task_type = 'z';
                shmPTR_jobs_buffer[k].task_duration = 0;
                shmPTR_jobs_buffer[k].task_status = 1;
                sem_post(sem_jobs_buffer[k]);
            } else if (alive != 0) {
                count++;
            }
        }
    }

    // Wait for all children processes to properly execute the 'z' termination jobs
    int process_waited_final = 0;
    pid_t wpid;
    while ((wpid = wait(NULL)) > 0){
        process_waited_final++;
    }

    // Print final results
    printf("Final results: sum -- %ld, odd -- %ld, min -- %ld, max -- %ld, total task -- %ld\n", ShmPTR_global_data->sum_work, ShmPTR_global_data->odd, ShmPTR_global_data->min, ShmPTR_global_data->max, ShmPTR_global_data->total_tasks);
}

void cleanup() {
    // TODO#4:
    // 1. Detach both shared memory (global_data and jobs)
    // 2. Delete both shared memory (global_data and jobs)
    // 3. Unlink all semaphores in sem_jobs_buffer

    // Detach and remove shared memory locations
    int detach_status = shmdt((void *) ShmPTR_global_data);         //detach
    int remove_status = shmctl(ShmID_global_data, IPC_RMID, NULL);  //delete

    if (detach_status == -1) printf("Detach shared memory global_data ERROR\n");
    if (remove_status == -1) printf("Remove shared memory global_data ERROR\n");

    detach_status = shmdt((void *) shmPTR_jobs_buffer);             //detach
    remove_status = shmctl(ShmID_jobs, IPC_RMID, NULL);             //delete

    if (detach_status == -1) printf("Detach shared memory jobs ERROR\n");
    if (remove_status == -1) printf("Remove shared memory jobs ERROR\n");

    // Unlink all semaphores before exiting process
    int sem_close_status = sem_unlink("semglobaldata");

    if (sem_close_status == -1) {
        printf("Semaphore globaldata fails to close.\n");
    }

    for (int i = 0; i < number_of_processes; i++) {
        char *sem_name = malloc(sizeof(char) * 16);
        sprintf(sem_name, "semjobs%d", i);
        sem_close_status = sem_unlink(sem_name);
        if (sem_close_status == -1) {
             printf("Semaphore jobs %d fails to close.\n", i);
        }
        free(sem_name);
    }
}

// Real main
int main(int argc, char* argv[]){
    // printf("Lab 1 Starts...\n");

    struct timeval start, end;
    long secs_used,micros_used;

    // Start timer
    gettimeofday(&start, NULL);

    // Check and parse command line options to be in the right format
    if (argc < 2) {
        printf("Usage: sum <infile> <numprocs>\n");
        exit(EXIT_FAILURE);
    }

    // Limit number_of_processes into 10. 
    // If there's no third argument, set the default number_of_processes into 1.  
    if (argc < 3) {
        number_of_processes = 1;
    }
    else {
        if (atoi(argv[2]) < MAX_PROCESS) number_of_processes = atoi(argv[2]);
        else number_of_processes = MAX_PROCESS;
    }

    setup();
    createchildren();
    main_loop(argv[1]);

    // Parent cleanup
    cleanup();

    // Stop timer
    gettimeofday(&end, NULL);

    double start_usec = (double) start.tv_sec * 1000000 + (double) start.tv_usec;
    double end_usec =  (double) end.tv_sec * 1000000 + (double) end.tv_usec;

    printf("Your computation has used: %lf secs \n", (end_usec - start_usec)/(double)1000000);

    return (EXIT_SUCCESS);
}