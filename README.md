# Secure-File-Transfer
This program uses public and private key encryption to send a file over the Internet using TCP sockets. Currently, the functionality allows the Client module to securely upload a file to the Server module. Both the Client and the Server have their own folders so that they can be run indpendently on different computers.  

### The Server
The server is multi-threaded, so can accept and process requests from multiple clients simultaneously. The clients wishing to communicate with the server can do it securely by encrypting their information using the server's public key. This is done by first sending a ```GET PUBLIC KEY``` request, which will cause the server to respond with its public key. This asymmetric cryptography technique is implemented using the RSA algorithm. The Server folder includes public.der and private.der, as those files are required by the server module to run. To generate your own different private and public keys, the following can be used: 
``` 
openssl genrsa -out private.pem 2048
openssl pkcs8 -topk8 -in private.pem -outform DER -out private.der -nocrypt
openssl rsa -in private.pem -pubout -outform DER -out public.der
```
 After the server successfully decrypts and saves the received file, it would send an acknowledge message to the client by either sending a ```SUCCSESS``` message, for a successful transmission or, ```ERROR:``` (following an error description) if an error occurred.

### The Client
The client starts its communication with the server by first requesting its RSA public key. It can send any file located in the Client folder (both text and binary files are supported). As the goal is to protect the file's data as it being sent to the server, the client encrypts the file content using 128 bit AES encrpytion. Since this is a symmetric key encryption technique, the server also needs this AES key to decrypt the file. Therefore, when sending the ```TRANSFER FILE``` request,  the client sends the AES key itself encrypted using the server's public key, as well as the file name and content encrypted by the AES key.

## Compiling and Running instructions
Enter the following to run the server:  
```bash
cd Server  
javac *.java  
java ServerReceiver
```
Or the following for the client:
```bash
cd Client
javac *.java
java ClientSender
```
