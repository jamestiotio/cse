# Define required macros here
REMOVE = rm
CC = gcc


# Explicit rules, all the commands you can call with make 
# (note: the <tab> in the command line is necessary for make to work) 
# target:  dependency1 dependency2 ...
#       <tab> command

#Called by: make targetname 
#also executed when you just called make. This calls the first target. 
all: rootprogram rootdoprogram logaccessprogram rootprogram_nosuid setup 

rootprogram: rootprog.c
	rm -rf ../Root/
	mkdir ../Root/
	$(CC) rootprog.c -o ../Root/rootprog

rootdoprogram: rootdo.c
	$(CC) rootdo.c -o ../Root/rootdo 

logaccessprogram: logaccess.c 
	$(CC) logaccess.c -o ../Root/logaccess 

rootprogram_nosuid: rootprog.c 
	$(CC) rootprog.c -o ../Root/rootprog_nosuid

# refresh the root files and root log file
# Set UID for rootprog and rootdo
# Set logaccess to be executable only by ROOT
setup: 
	chmod 744 ../Root/logaccess 
	chmod u+s ../Root/rootprog 
	chmod u+s ../Root/rootdo
	echo THIS IS ROOT FILE > ../Root/rootfile.txt
	echo THIS IS ROOT FILE > ../Root/rootlogfile.txt

test:
	ls -la ../Root
	../Root/rootdo ../Root/logaccess testlogFile
	cat ../Root/rootlogfile.txt

clean:
	$(REMOVE) ../Root/rootprog
	$(REMOVE) ../Root/rootdo 
	$(REMOVE) ../Root/logaccess 
	$(REMOVE) ../Root/rootprog_nosuid
	$(REMOVE) ../Root/rootfile.txt
	$(REMOVE) ../Root/rootlogfile.txt
	rm -rf ../Root/

