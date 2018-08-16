package app;

import socket_connection.ServerSocketConnection;
import socket_connection.SocketConnection;
import socket_connection.SocketUserAgentInterface;
import socket_connection.socket_exceptions.FailedToConnectException;
import socket_connection.socket_exceptions.NoDefaultConstructorException;
import socket_connection.socket_exceptions.ServerAlreadyClosedException;
import socket_connection.socket_exceptions.ServerShutdownException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;




public final class Main {
    private Main(){
        throw new AssertionError();
    }

    @SuppressWarnings("all")
    public static void main(String[] args){
        try {
            ServerSocketConnection server;
            server = new ServerSocketConnection(11000, Agent.class);
            method();
            server.close();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Agent.connections.forEach(connection-> System.out.println(connection.isConnected()));
        } catch (IOException | IllegalAccessException | InvocationTargetException | NoDefaultConstructorException | InstantiationException e) {
            e.printStackTrace();
        } catch (ServerShutdownException e) {
            e.printStackTrace();
        } catch (ServerAlreadyClosedException e) {
            e.printStackTrace();
        }
    }

    private static void method() {
        try {
            new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(),11000);
            new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(),11000);
            new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(),11000);
        } catch (FailedToConnectException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
