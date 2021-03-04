all: C_code/Banker.c C_code/checkerQ1.c C_code/checkerQ2.c
	gcc -o checkq1 C_code/checkerQ1.c
	gcc -o q1 C_code/Banker.c 
	gcc -o checkq2 C_code/checkerQ2.c
	gcc -o q2 C_code/Banker.c

testq1: ./checkq1 
	./checkq1

testq2: ./checkq2
	./checkq2

clean:
	rm -f checkq2
	rm -f checkq1
	rm -f q2
	rm -f q1
	rm -f answer.txt