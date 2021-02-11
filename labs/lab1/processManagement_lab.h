#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/time.h> // for clock_gettime()
#include <sys/ipc.h>
#include <sys/shm.h>
#include <signal.h>
#include <limits.h>
#include <stdbool.h>
#include <semaphore.h>
#include <fcntl.h>

/*DO NOT MODIFY THIS FILE*/
 
#define TIME_MULTIPLIER 100 // used to multiply the amount of time needed to execute a task/wait in usleep
#define MAX_PROCESS 10 // limit the number of maximum process

/**
 * Data struct containing global_data
 * */
typedef struct global_data{
    long sum_work;
    long odd;
    long min;
    long max;
    long total_tasks;
}global_data;

//shared_nemory global_data to share between children + main processes
int ShmID_global_data;
global_data *ShmPTR_global_data;
sem_t* sem_global_data;


/**
 * Data struct contaning job description
 * */
typedef struct job {
    char task_type;
    int task_duration;
    int task_status;    // if task_status == -1 : termination job
                        // if task_status == 1 : new job available
                        // if task_status == 0 : no job available or job done. Init task_status to this value. 
}job;

//shared_nemory global_jobs to share to child process
int ShmID_jobs;
job *shmPTR_jobs_buffer;
sem_t* sem_jobs_buffer[MAX_PROCESS];


//for main proc
int number_of_processes; // the total number of processes from input, capped at 10 
pid_t children_processes[MAX_PROCESS]; // id of all child processes

// Main functions
void task(long duration);
void job_dispatch(int process_id);
void setup();
void createchildren();
void main_loop(char* filename);
void cleanup();
