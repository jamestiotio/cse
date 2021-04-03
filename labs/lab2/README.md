# Lab 2: Banker's Algorithm Lab

> Code Author: James Raphael Tiovalen

In this lab, we are tasked to implement the Banker's algorithm. In particular, we are required to:

- Implement a basic bank system
- Implement a safety check algorithm
- Analyse and discuss about the complexity of the Banker's algorithm

This is the given scenario for this lab:

> There are several customers request and release resources from the bank. The banker will grant a request only if it leaves the system in a safe state. A request is denied if it leaves the system in an unsafe state.
>
> The bank will employ the banker’s algorithm, whereby it will consider requests from n customers for m resources. The bank will keep track of the resources using the following variables: `numberOfCustomers`, `numberOfResources`, `available`, `maximum`, `allocation` and `need`.
