# Programming Assignment 1

> **_CSEShell_**, not your ordinary shell.

Team Members (Pair ID 1, Class CI03):

- [James Raphael Tiovalen](https://github.com/jamestiotio)
- [Leong Yun Qin Melody](https://github.com/caramelmelmel)

In this programming assignment, we are tasked to create a shell as well as a daemon process, both of which are common applications of `fork()`. In particular, we are required to:

- Create a shell and wait for user input
- Write several other custom programs that can be invoked by the shell
- Parse user input and invoke `fork()` with the appropriate program
- Create a program that results in a daemon process
- Use the shell to keep track the state of the corresponding daemon processes

Features for future development:

- Tab completion
- Run custom executables/binaries (by indicating their path as the first argument/token)