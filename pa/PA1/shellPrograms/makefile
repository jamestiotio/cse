# Define required macros here
REMOVE = rm
CC = gcc


# Explicit rules, all the commands you can call with make 
# (note: the <tab> in the command line is necessary for make to work) 
# target:  dependency1 dependency2 ...
#       <tab> command

#Called by: make targetname 
#also executed when you just called make. This calls the first target. 
all: target_find target_display target_listdirall target_listdir target_countline target_daemon target_checkdaemon

target_find: shellFind_code.c
	$(CC) shellFind_code.c -o find

target_display: shellDisplayFile_code.c
	$(CC) shellDisplayFile_code.c -o display 

target_listdirall: shellListDirAll_code.c
	$(CC) shellListDirAll_code.c -o listdirall

target_listdir: shellListDir_code.c
	$(CC) shellListDir_code.c -o listdir

target_countline: shellCountLine_code.c
	$(CC) shellCountLine_code.c -o countline

target_daemon: shellDaemonize_code.c
	$(CC) shellDaemonize_code.c -o summond


target_checkdaemon: shellCheckDaemon_code.c 
	$(CC) shellCheckDaemon_code.c -o checkdaemon 

clean:
	$(REMOVE) find display listdirall listdir countline summond checkdaemon

