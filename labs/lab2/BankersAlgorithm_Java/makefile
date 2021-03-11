all: Banker.java TestBankQ1.java TestBankQ2.java 
	javac Banker.java TestBankQ1.java TestBankQ2.java 
	gcc -o checkq1 checkerQ1.c
	gcc -o checkq2 checkerQ2.c

testq1: checkq1 Banker.class TestBankQ1.class
	./checkq1

testq2: checkq2 Banker.class  TestBankQ2.class
	./checkq2

clean:
	rm -f checkq1
	rm -f checkq2
	rm -f Banker.class
	rm -f TestBankQ1.class
	rm -f TestBankQ2.class
	rm -f answer.txt