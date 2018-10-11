package socket_connection;

import socket_connection.socket_exceptions.runtime_exceptions.BadSetupException;

/**
 * This class work is to handle all kind of defined-type messages that are used
 * from SocketConnection to getInstance, to check if the connection is still active and
 * other stuff.
 *
 * CSM means ConnectionStatusMessages
 */
final class CSMHandler {
    private CSMHandler(){
        throw new AssertionError();
    }

    /**
     * This method handles a ping message
     * @param connection which received the message
     */
    static void handlePingMessage(SocketConnection connection){
        connection.resetTTL();
    }

    /**
     * This method handles a hello message from the client
     * @param connection which received the message
     */
    static void handleHelloMessage(SocketConnection connection){
        if(connection.isServerSide()&& connection.isReady()) throw new BadSetupException();
        connection.setToReady();
        connection.resetTTL();
    }

    /**
     * This method handles the message sent from the server to notify client that
     * it's ready
     * @param connection which received the message
     */
    static void handleServerIsReadyMessage(SocketConnection connection) {
        if(!connection.isServerSide()&& connection.isReady()) throw new BadSetupException();
        connection.setToReady();
        connection.resetTTL();
    }
}
