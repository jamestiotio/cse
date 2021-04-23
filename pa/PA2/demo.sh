#!/bin/bash
# TODO
# NOTE: Only for PA2 demo purposes!
# Firstly, this will spawn and arrange the terminal windows as necessary for the demo.
# Then, this script will launch and auto-run the necessary scripts to indicate that the PA2 client-server programs are working properly.
# Timing constants indicated in this script are arbitrarily set, again for demo purposes.
# Created by James Raphael Tiovalen (2021)

# Get user's default terminal
terms=(x-terminal-emulator gnome-terminal urxvt termit terminator konsole)
for t in ${terms[*]}
do
    if [ $(command -v $t) ]
    then
        detected_term=$t
        break
    fi
done

tab="--tab"
cmd="bash -c '<command-line_or_script>';bash"
foo=""

for i in 1 2 ... n; do
    foo+=($tab -e "$cmd")
done

$detected_term "${foo[@]}"

exit 0
