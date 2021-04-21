#!/bin/bash
# TODO
# NOTE: Only for PA2 demo purposes!
# Firstly, this will spawn and arrange the terminal windows as necessary for the demo.
# Then, this script will launch and auto-run the necessary scripts to indicate that the PA2 client-server programs are working properly.
# Timing constants indicated in this script are arbitrarily set, again for demo purposes.
# Created by James Raphael Tiovalen (2021)

tab="--tab"
cmd="bash -c '<command-line_or_script>';bash"
foo=""

for i in 1 2 ... n; do
    foo+=($tab -e "$cmd")
done

x-terminal-emulator "${foo[@]}"

exit 0
