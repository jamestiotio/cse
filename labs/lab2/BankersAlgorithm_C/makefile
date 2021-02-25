all: C_code/BankerQ1.c C_code/checkerQ1.c C_code/BankerQ2.c C_code/checkerQ2.c
	gcc -o checkq1 C_code/checkerQ1.c
	gcc -o q1 C_code/BankerQ1.c 
	gcc -o checkq2 C_code/checkerQ2.c
	gcc -o q2 C_code/BankerQ2.c

test: ./checkq1 ./checkq2
	./checkq1
	./checkq2

clean:
	rm checkq2
	rm checkq1
	rm q2
	rm q1
	rm answer.txt