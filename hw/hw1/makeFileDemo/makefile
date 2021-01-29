# Define required macros here
REMOVE = rm
CC = gcc
DEPENDENCIES = main.c hello.c factorial.c binary.c
OUT = myexecoutprogMacro.o

# Explicit rules, all the commands you can call with make 
# (note: the <tab> in the command line is necessary for make to work) 
# target:  dependency1 dependency2 ...
#       <tab> command

#Called by: make myexecout 
#also executed when you just called make. This calls the first target. 
myexecout: main.c hello.c factorial.c binary.c
	gcc -o myexecoutprog.o main.c hello.c factorial.c binary.c 

myexecoutFromMacro: $(DEPENDENCIES)
	$(CC) $(DEPENDENCIES) -o $(OUT)

myexecout2: main.o hello.o factorial.o binary.o
	gcc -o myexecoutprog2 main.o hello.o factorial.o binary.o


##make clean will remove myexecoutprog.o from the directory
clean:
	rm myexecoutprog.o

cleanFromMacro: 
	$(REMOVE) $(OUT)

clean2:
	rm myexecoutprog2 *.o

#Implicit rules
main.o : main.c functions.h
factorial.o: factorial.c functions.h
hello.o: hello.c functions.h
binary.o: binary.c functions.h