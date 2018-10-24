package socket_connection.socket_exceptions.runtime_exceptions.socket_connection_events;

public class ServerReadyException extends ConnectionEventException {
    public ServerReadyException(String eventData) {
        super(eventData);
    }
}
