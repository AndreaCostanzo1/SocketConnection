package socket_connection;

public interface SocketUserAgentInterface extends Runnable {
    /**
     * This method should be used to set the SocketConnection.
     */
    void setConnection(SocketConnection connection);

    /**
     * This method should be used to notify that something went wrong during connection.
     * It usually means that client disconnected. How to use the information about disconnection
     * is left to the implementing class. Displaying it and shutdown the agent could be a good idea
     */
    void notifySetUpError();

    /**
     * This method is used to shutdown the user agent and the relative
     * socketConnection.
     */
    void shutdown();
}
