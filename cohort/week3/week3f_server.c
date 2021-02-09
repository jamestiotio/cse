#include <unistd.h>
#include <stdio.h>
#include <sys/socket.h>
#include <stdlib.h>
#include <netinet/in.h>
#include <string.h>
#include <fcntl.h> /* Added for the nonblocking socket */

#define PORT 12345

int main(int argc, char const *argv[])
{
    int server_fd, new_socket, valread;
    int opt = 1;
    struct sockaddr_in address;
    int addrlen = sizeof(address);

    char buffer[1024] = {0};
    char *message = "Hello from server";

    // Creating socket, and obtan the file descriptor
    // Option:
    //      - SOCK_STREAM (TCP -- Week 11)
    //      - AF_INET (IPv4 Protocol -- Week 11)
    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0)
    {
        perror("socket failed");
        exit(EXIT_FAILURE);
    }

    // Attaching socket to  port 12345
    if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEPORT,
                    &opt, sizeof(opt)))
    {
        perror("setsockopt");
        exit(EXIT_FAILURE);
    }
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(PORT);

    // Assign name to the socket
    /**
    * When a socket is created with socket(2), it exists in a name
        space (address family) but has no address assigned to it.  bind()
        assigns the address specified by addr to the socket referred to
        by the file descriptor sockfd.  addrlen specifies the size, in
        bytes, of the address structure pointed to by addr.
        Traditionally, this operation is called "assigning a name to a
        socket".
    **/

    if (bind(server_fd, (struct sockaddr *)&address,
            sizeof(address)) < 0)
    {
        perror("bind failed");
        exit(EXIT_FAILURE);
    }

    // server listens for new connection (blocking system call)
    if (listen(server_fd, 3) < 0)
    {
        perror("listen");
        exit(EXIT_FAILURE);
    }

    // accept incoming connection, creating a 1-to-1 socket connection with this client (this accept() is blocking)
    if ((new_socket = accept(server_fd, (struct sockaddr *)&address,
                            (socklen_t *)&addrlen)) < 0)
    {
        perror("accept");
        exit(EXIT_FAILURE);
    }

    // Blocking read
    valread = read(new_socket, buffer, 1024);

    // Nonblocking read
    // fcntl(new_socket, F_SETFL, O_NONBLOCK); /* Change the socket into non-blocking state */
    // valread = recv(new_socket, buffer, 1024, 0);

    printf("%s\n", buffer);
    send(new_socket, message, strlen(message), 0);
    printf("Hello message sent to client\n");
    return 0;
}