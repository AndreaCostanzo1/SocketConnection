package socket_connection.socket_exceptions.runtime_exceptions.socket_connection_events;

public class DataReceivedException extends ConnectionEventException {

    public DataReceivedException(String eventData) {
        super(eventData);
    }
}
