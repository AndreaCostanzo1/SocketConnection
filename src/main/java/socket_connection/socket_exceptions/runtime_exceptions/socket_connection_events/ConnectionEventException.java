package socket_connection.socket_exceptions.runtime_exceptions.socket_connection_events;

public class ConnectionEventException extends RuntimeException {
    private String eventData;

    public ConnectionEventException(String eventData){
        this.eventData = eventData;
    }

    public String getEventData() {
        return eventData;
    }
}
