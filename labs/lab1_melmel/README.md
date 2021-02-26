# 50005Lab1

Tackling task 4a. 

Debugging: just use a bunch of printf statements and github to see the carelessness.

## Implementation 
1. Use a loop that is while(1) for the busy wait portion to check if everything is fulfilled.
2. Since we are handling two arrays, children_processes as well as shmPTR_jobs_buffer, we must always and consistently check for any terminated processes in the task status.
3. We close the semaphore of the jobs buffer in the busy wait loop itself.
4. we use a boolean variable task here to annotate whether a task in the jobs buffer is done so that we can break out of the loop that checks for the status of the children process, whether or not they are active.
6. Loop through in another loop through the children processes array to check for anymore child processses that are illegally or prematurely terminated.
7. Through the use of waitpid again, we check the status of each child process on whether it is illegal.
8. If it is illegal, we spawn new worker processes using fork to take over the current one.
9. Do the usual fork checking to be safe and activate the child processes.
10. exit when the fork fails and execute the relevant jobs.
11. Do the usual termination process.
12. loop the whole child processes until we terminate all of them.
