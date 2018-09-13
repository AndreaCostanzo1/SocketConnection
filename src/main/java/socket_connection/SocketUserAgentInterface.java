package socket_connection;

public interface SocketUserAgentInterface extends Runnable {
    /**
     * This method should be used to set the SocketConnection.
     */
    void setConnection(SocketConnection connection);

    /**
     * This method is used to shutdown the user agent and the relative
     * socketConnection.
     */
    void shutdown();
}
