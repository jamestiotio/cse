# Lab 1: Process Management Lab

> Code Author: James Raphael Tiovalen

In this lab, we are tasked to manage a pool of UNIX resources. In particular, we are required to:

- Perform `fork()` and handle its return value
- Manage a set of processes
- Manage inter-processes communications using shared memory
- Protect shared resources with semaphores
- Manage the process pool:
  - Check the status of child processes
  - Dispatch jobs to child processes
  - Respawn (prematurely) terminated children processes
- Perform basic I/O operations

To compile the script, run this command: `gcc -o out processManagement_lab.c -lpthread` (necessary when implementing threads and semaphores, since we need to link with the `pthread` library).

## TODO#4a Explanation on `main_loop()` Design Implementation

### Worker Process Revival Method

The global shared memory job buffer consists of N "pigeonholes"/"slots", each for one child process. `main_loop()` will continuously check the job buffer. If there is a new input obtained from the input file, it will first check whether the task status of each child is done (indicated by the task status value of `0`) or not. If it finds a child process that has finished doing its task, it will assign the new input to the child by executing a `sem_post()` on that specific job buffer slot. If another input is being fed in, it will busy wait (which is allowed) and repeat the checking of each child's status in the job buffer (for as long as there are new inputs from the input file).

The most outer while loop reads the input file line by line by using the `fscanf()` function. A boolean flag `job_is_assigned` is initialized to be false. Then, while this new job is not yet assigned, it will enter the inner while loop. In the for loop, we go through each slot of the job buffer to check whether each child is alive or not by using the `waitpid()` function. If a particular child has finished its job and it is still alive, we post the new input to the child via the `sem_post()` while also updating the task content, as well as setting the `job_is_assigned` boolean flag to `true`. We will then break out of the loop and continue onto the next input line.

However, if the child is terminated due to an illegal task, the child will be revived through forking, creating a new worker and dispatching the job to said worker accordingly by using `job_dispatch()`.

If there are no more input jobs present, the code will proceed to the termination stage and it will no longer revive any dead children.

### Active Worker Process Legal Termination Method

Once all of the input lines have been processed, all the worker processes that are still currently alive need to be terminated after they have finished their respective jobs. This is done by sending `z0` signals/jobs to them.

The `count` variable is used to keep track of the total number of terminated processes since we need to ensure that all processes are accounted for before we finish executing the whole program. We constantly check whether the number of dead processes are equal to the number of processes created (which is the `number_of_processes` global variable). Within the for loop, we check whether each child process is alive and has completed their task or not. If they are alive and have finished their job, we set a new `z0` task content into the respective job slot so as to legally terminate said child process and update the `sem_jobs_buffer` semaphore by calling `sem_post()`. If there are still child processes who have not finished their jobs, we busy wait. If the child is detected to be not alive, we simply increment the `count` variable and we do not bother to revive them since there is no need to.

Once the total number of terminated processes (`count`) is equivalent to the number of processes initially created (`number_of_processes`), we exit the loop, wait for all the child processes to properly execute the `z0` termination job, and start cleaning up.
