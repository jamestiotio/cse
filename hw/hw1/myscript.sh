#!/bin/bash
greeting = "Welcome"
user = $(whoami)
day = $(date +%A)

echo "$greeting back $user! Today is $day, which is the best day of the entire week!"
echo "Your Bash shell version is: $BASH_VERSION. Enjoy!"

string_a = "UNIX" 
string_b = "GNU"

echo "Are $string_a and $string_b strings equal?" 
[ $string_a = $string_b ]
echo $?

num_a = 100
num_b = 200

echo "Is $num_a equal to $num_b ?" 
[ $num_a -eq $num_b ]
echo $?

if [ $num_a -lt $num_b ]; then
    echo "$num_a is less than $num_b!"
fi

for i in 1 2 3; do
    echo $i
done