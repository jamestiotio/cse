# Programming Assignment 2: Secure File Transfer Protocol

> Basic SFTP Implementation.

Team Members (Pair ID 0, Class CI03):

- [James Raphael Tiovalen](https://github.com/jamestiotio)
- [Leong Yun Qin Melody](https://github.com/caramelmelmel)

> Same partnership as PA1.

In this programming assignment, we are tasked to implement a secure file upload application from a client to an Internet file server (following the client-server paradigm). By secure, we mean two properties. First, before you do your upload as the client, you should authenticate the identity of the file server so you won’t leak your data to random entities including criminals. Second, while carrying out the upload, you should be able to protect the confidentiality of the data against eavesdropping by any curious adversaries.

The server will be called `SecStore`. It’s an Internet server that is running at some IP address, ready to accept connection requests from clients. When a client has a file to upload, it will:

1. Initiate the connection,
2. Handshake with the server, and then
3. Perform the upload.

The CSE teaching staff will act as our trusted CA (Certificate/Certification Authority), their service being called `Csertificate`.

In particular, these are the basic requirements:

1. The server doesn’t have to interpret the content of the file, i.e., you can treat the file as a stream of bytes without worrying about the meaning of those bytes. 

2. However, you should be able to **handle arbitrary files** (e.g., binary files instead of say ASCII texts only), and your upload must be reliable. By **reliability**, we mean the server will store **exactly** what the client sent, **without any loss, reordering, or duplication of data**. Implement your file upload using standard *TCP sockets*.

3. The server must be able to *receive MULTIPLE file uploads* from the **same client** in the **same connection once established**, and only *TERMINATE* the connection upon request. **The starter code only receives one file and terminates. Modify this to support multiple file upload. You can make your code prompt for user input to key in filename, OR, put the filenames as ARGUMENTS before the program is run.**

4. Implement AP (Authentication *handshake* Protocol) in your file upload application.

5. Implement CP1 (Confidentiality Protocol 1) in your file upload application. This protocol uses RSA for data confidentiality.

6. Implement CP2 (Confidentiality Protocol 2) in your file upload application. This protocol uses AES for data confidentiality. Your protocol must negotiate a session key for the AES after the client has established a connection with the server. It must also ensure the confidentiality of the session key itself.

7. **Measure the data throughput** of CPL1 vs. CPL2 for uploading files of a range of sizes. Plot your results, and compare their performance.

System requirements: implemented in Java using the Java Cryptography Extension (JCE), which should already be included in a standard Java distribution.