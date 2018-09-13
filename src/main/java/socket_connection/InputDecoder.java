package socket_connection;

import socket_connection.socket_exceptions.BadSetupException;

final class InputDecoder {
    private InputDecoder(){
        throw new AssertionError();
    }

    static void handlePingMessage(SocketConnection connection){
        connection.resetTTL();
    }

    static void handleHelloMessage(SocketConnection connection){
        if(connection.isServerSide()&& connection.isReady()) throw new BadSetupException();
        connection.setToReady();
        connection.resetTTL();
    }

    static void handleServerIsReadyMessage(SocketConnection connection) {
        if(!connection.isServerSide()&& connection.isReady()) throw new BadSetupException();
        connection.setToReady();
        connection.resetTTL();
    }
}
