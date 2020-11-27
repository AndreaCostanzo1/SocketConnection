# SocketConnection :email:

[![CircleCI](https://circleci.com/gh/AndreaCostanzo1/SocketConnection/tree/master.svg?style=svg)](https://circleci.com/gh/AndreaCostanzo1/SocketConnection/tree/master)
[![codecov](https://codecov.io/gh/AndreaCostanzo1/SocketConnection/branch/master/graph/badge.svg?token=VpTqBJobEH)](https://codecov.io/gh/AndreaCostanzo1/SocketConnection)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
![GitHub issues](https://img.shields.io/github/issues/AndreaCostanzo1/SocketConnection)


A socket-based encrypted:closed_lock_with_key: communication protocol developed to handle the communication logic in the project of Software Engineering I at Polimi.
This protocol encrypts data using the RSA algorithm with key lenghts of 2048 bits and keeps track of the connection status even without the need to send messages between hosts with the provided write methods.

## Client-side :computer:
For what concern the client-side of the application its usage is pretty similar to a regular socket.
### Setup
The connection is set up by giving the **IP address** and the  **port** on which we are supposed to reach the server. If the server is unreachable, the constructor will throw a **FailedToConnectException**.
```java
//random IP address and port
String ip = "127.0.0.1";
int port = 14000;
try{
    //trying to connect
    SocketConnection sck = new SocketConnection(ip,port);
} catch (FailedToConnectException e) {
    //host unreachable
}
```

### Read and write a String
```java
try{
    //write
    String messageToSend = "Hello";
    sck.writeString(messageToSend);
    //read
    String messageReceived = sck.readString();
} catch (UnreachableHostException e) {
    //connection lost...
}
```
### Read and write a int
```java
try{
    //write
    int numberToSend = 1;  
    sck.writeInt(numberToSend);
    //read
    int numberReceived = sck.readInt();
} catch (UnreachableHostException e) {
    //connection lost...
}
```

## Server-side :satellite:
The purpose of the protocol was to hide as far as possible the logic related to the implementation of the ServerSocket, to simplify the communication between the client and the server.

### Create the communication logic
First of all it is necessary to create the class that will handle each single connection client-server from server side. This class must **implement SocketUserAgentInterface** and must have a **default constructror** (with no arguments).
```java
//implement SockerUserAgentInterface
class MyClass implements SocketUserAgentInterface{
    
    //a default public constructor is required!
    public MyClass(){
        //....
    }
}
```

Than we have to **save the SocketConnection** created when a client connects to the server:

```java
//implement SockerUserAgentInterface
class MyClass implements SocketUserAgentInterface{
	
    //Socket connection that will be used to exchange
    //messages with the client
    SocketConnection sck;
	
    //a default public constructor is required!
    public MyClass(){  
        //....
    }

    @Override
    public setConnection(SocketConnection conn){ 
        //save the socket connection
        sck= conn;
    }
	
}
```

Now we have to handle a possible request from the server to shutdown the connection:
```java
//implement SockerUserAgentInterface
class MyClass implements SocketUserAgentInterface{
	
    //Socket connection that will be used to exchange
    //messages with the client
    SocketConnection sck;
    
    //a default public constructor is required!
    public MyClass(){
        //....
    }
    
    @Override
    public setConnection(SocketConnection conn){
        //save the socket connection
	sck= conn; 
    }

    @Override
    public shutdown(){
        //code here;
    }	
}
```

And finally we're going to handle the communication with the client:

```java
//implement SockerUserAgentInterface
class MyClass implements SocketUserAgentInterface{
	
    //Socket connection that will be used to exchange
    //messages with the client
    SocketConnection sck;
	
    //a default public constructor is required!
    public MyClass(){
	    //....
    }
    
    @Override
    public setConnection(SocketConnection conn){
	//save the socket connection
	sck= conn; 
    }

    @Override
    public shutdown(){
        //code here
    }

    @Override
    public run(){
        //send and receive messages
	//THIS IS JUST AN EXAMPLE
	try{
	    String helloFromClient = sck.readString();
	    sck.writeString("Helloo! :)"); 
	} catch (UnreachableHostException e) {
	    //connection lost...
	} 
    }	    
}
```

### Server set up
Now we have to create the server that will handle all the connection requests. The server is run on a separated thread, that will start automatically after creation. For each connection request, a new instance of the class implementing "SocketUserAgent" is created to handle the connection with the client.

```java
int port;
//MyClass implements SocketUserAgentInterface
ServerSocketConnection server = new ServerSocketConnection(port, MyClass.class);
```

It's also possible to delay the start of the server. In this way, we can choose to run it manually later:
```java
int port;
//by passing true as third parameter we set manualStart=true;
ServerSocketConnection server = 
    new ServerSocketConnection(port, MyClass.class, true);
//INSERT HERE YOUR CODE

//THEN START THE SERVER
server.start();
```

### Server shut down
To shutdown the server just call the shutdown method.
```java
try{
    server.shutdown();
} catch (ServerShutdownException e) {
    //server already terminated
}
```
