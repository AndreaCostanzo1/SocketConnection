# SocketConnection

[![CircleCI](https://circleci.com/gh/AndreaCostanzo1/SocketConnection/tree/master.svg?style=svg&circle-token=bcbb2f5301abb436270cf5b9c8b19b88bb4f13ea)](https://circleci.com/gh/AndreaCostanzo1/SocketConnection/tree/master)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
![GitHub issues](https://img.shields.io/github/issues/AndreaCostanzo1/SocketConnection)

A socket-based encrypted communication protocol developed for the project of Software Engineering I at Polimi.


## Client-side
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
To send data to the server 2 methods are available:
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
To send data to the server 2 methods are available:
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

## Server-side
The purpose of the protocol was to hide as far as possible the logic related to the implementation of the ServerSocket, to simplify the communication between the client and the server.

### Create the communication logic
First of all is necessary to create the class that will handle the communication between a client and the server. This class must **implement SocketUserAgentInterface** and must have a **default constructror** (with no arguments).
```java
//implement SockerUserAgentInterface
class MyClass implements SocketUserAgentInterface{
    
    //a default public constructor is requested!
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
	
    //a default public constructor is requested!
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
    
    //a default public constructor is requested!
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

And finally handle the communication with the client:

```java
//implement SockerUserAgentInterface
class MyClass implements SocketUserAgentInterface{
	
    //Socket connection that will be used to exchange
    //messages with the client
    SocketConnection sck;
	
    //a default public constructor is requested!
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
