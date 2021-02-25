all: Banker.java TestBankQ1.java TestBankQ2.java 
	javac Banker.java TestBankQ1.java TestBankQ2.java 
	gcc -o checkq1 checkerQ1.c
	gcc -o checkq2 checkerQ2.c

test: checkq1 checkq2 Banker.class TestBankQ1.class TestBankQ2.class
	./checkq1
	./checkq2


clean:
	rm checkq1
	rm checkq2
	rm Banker.class
	rm TestBankQ1.class
	rm TestBankQ2.class
	rm answer.txt