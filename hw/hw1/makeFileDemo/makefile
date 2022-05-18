# Define required macros here
REMOVE = rm
CC = gcc
DEPENDENCIES = main.c hello.c factorial.c binary.c
OUT = prog.o

# Explicit rules, all the commands you can call with make 
# (note: the <tab> in the command line is necessary for make to work) 
# target: dependency1 dependency2 ...
#       <tab> command

# Called by: make prog
# Also executed when you just called make. This calls the first target. 
prog: main.c hello.c factorial.c binary.c
	gcc -o prog.o main.c hello.c factorial.c binary.c 

prog1: $(DEPENDENCIES)
	$(CC) $(DEPENDENCIES) -o $(OUT)

prog2: main.o hello.o factorial.o binary.o
	gcc -o prog2 main.o hello.o factorial.o binary.o


## make clean will remove prog.o from the directory
clean:
	rm prog.o

clean1:
	$(REMOVE) $(OUT)

clean2:
	rm prog2 *.o

# Implicit rules
main.o: main.c functions.h
factorial.o: factorial.c functions.h
hello.o: hello.c functions.h
binary.o: binary.c functions.h