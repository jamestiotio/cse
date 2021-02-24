## This is the standard makefile for lab submission.
all : processManagement_lab.c ./checkerCode/check.c
	gcc processManagement_lab.c -lpthread -o out
	gcc ./checkerCode/check.c -o ./checkerCode/checkout


test : ./checkerCode/checkout
	./checkerCode/checkout


## This is the custom makefile for the purposes of the ruthless custom checker version 2: electric boogaloo (comment these out when submitting).
# all : processManagement_lab.c ./customEnhancedCheckerCode/generator.c ./customEnhancedCheckerCode/check.c
# 	gcc processManagement_lab.c -lpthread -o out
# 	gcc ./customEnhancedCheckerCode/generator.c -o ./customEnhancedCheckerCode/generate
# 	gcc ./customEnhancedCheckerCode/check.c -o ./customEnhancedCheckerCode/checkout


# test : ./customEnhancedCheckerCode/generate ./customEnhancedCheckerCode/checkout
# 	./customEnhancedCheckerCode/generate
# 	./customEnhancedCheckerCode/checkout
