package app;

import socket_connection.SocketConnection;
import socket_connection.SocketUserAgentInterface;

import java.util.ArrayList;
import java.util.List;

public class Agent implements SocketUserAgentInterface {
    public Agent(){

    }
    static List<SocketConnection> connections=new ArrayList<>();
    @Override
    public void setConnection(SocketConnection connection) {
        connections.add(connection);
    }

    @Override
    public void notifySetUpError() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void run() {

    }
}
