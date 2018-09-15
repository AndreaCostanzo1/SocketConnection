package socket_connection.socket_exceptions;

public class BadMessagesSequenceException extends Exception {
    private final String message;

    public BadMessagesSequenceException(String message){
        this.message=message;
    }

    @Override
    public String getMessage(){
      return this.message;
    }
}
