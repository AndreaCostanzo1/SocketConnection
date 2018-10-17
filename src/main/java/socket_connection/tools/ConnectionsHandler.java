package socket_connection.tools;

import socket_connection.SocketConnection;
import socket_connection.SocketUserAgentInterface;
import socket_connection.socket_exceptions.runtime_exceptions.BadSetupException;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionsHandler {

    private Lock availableConnectionsLock;
    private HashMap<SocketConnection,SocketUserAgentInterface> availableConnections;
    private int activeConnections;

    /**
     * Public constructor of ConnectionHandler
     */
    public ConnectionsHandler(){
        availableConnections= new HashMap<>();
        availableConnectionsLock=new ReentrantLock();
        activeConnections=0;
    }

    /**
     * This method shut down all the active connections
     */
    public void shutdownAllConnections(){
        availableConnectionsLock.lock();
        availableConnections.values().forEach(SocketUserAgentInterface::shutdown);
        activeConnections=0;
        availableConnectionsLock.unlock();
    }

    /**
     * Add a connection to the map registering active connections and their relative user agent
     * @param connection is the socket used to communicate
     * @param runningAgent is the agent related to the given socket
     */
    public void addConnection(SocketConnection connection, SocketUserAgentInterface runningAgent){
        Objects.requireNonNull(connection);
        Objects.requireNonNull(runningAgent);
        availableConnectionsLock.lock();
        availableConnections.put(connection,runningAgent);
        activeConnections++;
        availableConnectionsLock.unlock();
    }

    /**
     * Remove the connection passed from connections hash map
     * @param connection to be removed
     * @exception BadSetupException if the connection isn't in the hash map
     */
    public void removeConnection(SocketConnection connection) {
        Objects.requireNonNull(connection);
        availableConnectionsLock.lock();
        if(!Optional.ofNullable(availableConnections.remove(connection)).isPresent()) throw new BadSetupException();
        else activeConnections--;
        availableConnectionsLock.unlock();
    }

    /**
     * @return the number of active connection
     */
    @SuppressWarnings("WeakerAccess")
    public int activeConnections(){
        availableConnectionsLock.lock();
        int toReturn=activeConnections;
        availableConnectionsLock.unlock();
        return toReturn;
    }
}
