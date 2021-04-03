# Lab 3: TOCTOU Race Condition Attack Lab

In this lab, we are tasked to investigate a program with TOCTOU (Time of Check - Time of Use) race-condition vulnerability. This lab is written entirely in C and it is more of **an investigative lab** with fewer coding components compared to the previous labs. In particular, we are required to:

- Understand what is a TOUTOU bug and why is it prone to attacks
- Detect race-condition caused by the TOCTOU bug
- Provide a fix to this TOCTOU vulnerability
- Examine file permissions and modify them
- Understand the concept of **‘privileged programs’:** user level vs root level
- Compile programs and make documents with different privilege level
- Understand how **sudo** works
- Understand the difference between symbolic and hard links
- Write a .c program that creates a symbolic link to existing file
