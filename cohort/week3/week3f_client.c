#include <unistd.h>
#include <stdio.h>
#include <sys/socket.h>
#include <stdlib.h>
#include <netinet/in.h>
#include <string.h>
#include <fcntl.h> /* Added for the nonblocking socket */
#include <arpa/inet.h>

#define PORT 12345

int main(int argc, char const *argv[])
{
    struct sockaddr_in address;
    int sock = 0, valread;
    struct sockaddr_in serv_addr;

    char *message = "Hello from client";
    char buffer[1024] = {0};

    // create a socket
    if ((sock = socket(AF_INET, SOCK_STREAM, 0)) < 0)
    {
        printf("\n Socket creation error \n");
        return -1;
    }

    // fill block of memory 'serv_addr' with 0
    memset(&serv_addr, '0', sizeof(serv_addr));

    // setup server address
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(PORT);

    // Convert IPv4 addresses from text to binary form and store it at serv_addr.sin_addr
    if (inet_pton(AF_INET, "127.0.0.1", &serv_addr.sin_addr) <= 0)
    {
        printf("\nInvalid address/ Address not supported \n");
        return -1;
    }

    // connect to the socket with defined serv_addr setting (if client is ran before server, connection fails as nobody is listening at the specified port socket)
    if (connect(sock, (struct sockaddr *)&serv_addr, sizeof(serv_addr)) < 0)
    {
        printf("\nConnection Failed \n");
        return -1;
    }

    // send some data over
    send(sock, message, strlen(message), 0);
    printf("Hello message sent to server\n");

    // read from server back
    valread = read(sock, buffer, 1024);
    printf("%s\n", buffer);

    return 0;
}