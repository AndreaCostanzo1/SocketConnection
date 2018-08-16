package socket_connection;


import socket_connection.socket_exceptions.UndefinedInputTypeException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@FunctionalInterface
interface DecodingFunction<T>{
    void run(T t);
}

final class MessageHandler {


    private static String dataMessage ="#DATA#";
    private static final String PING_MESSAGE="";
    private static final String HELLO_MESSAGE="#HELLO#";
    private static final String SERVER_IS_READY_MESSAGE = "#SERVER_READY#";

    private static final Map<String, DecodingFunction<SocketConnection>> INPUT_TYPES=new HashMap<>();
    static {
        INPUT_TYPES.put(PING_MESSAGE, InputDecoder::handlePingMessage);
        INPUT_TYPES.put(HELLO_MESSAGE, InputDecoder::handleHelloMessage);
        INPUT_TYPES.put(SERVER_IS_READY_MESSAGE, InputDecoder::handleServerIsReadyMessage);
    }
    private static final int DATA_TAG_POSITION =0;
    private static final Predicate<String> inputIsDataType =
            s->s!=null && s.length()>=dataMessage.length() &&
                    s.substring(DATA_TAG_POSITION,dataMessage.length()+DATA_TAG_POSITION).equals(dataMessage);

    /**
     * private constructor of MessageHandler: it's a final class so couldn't exist any object of this.
     */
    private MessageHandler(){
        throw new AssertionError();
    }

    /**
     * This method valuate if the input is a ping message or not:
     * ping messages reset TTL of the socket
     * data messages are added to the buffer of the asking connection
     * @param connection is the connection who requested to compute an input
     * @param input to be computed
     * @exception UndefinedInputTypeException thrown if the input isn't a data nor a defined-type message
     */
    static void computeInput(SocketConnection connection, String input){
        if(inputIsDataType.test(input))
            handleDataInput(connection,input);
        else
            handleOthersInputs(connection,input);
    }

    /**
     * This method is used to refactor data messages and add them to buffer
     * @param connection is the connection who requested to compute an input
     * @param input to be computed
     */
    private static void handleDataInput(SocketConnection connection,String input) {
        String data=input.replace(dataMessage,"");
        connection.addToBuffer(data);
        connection.resetTTL();
    }

    /**
     * This method is used to handle non data messages
     * @param connection is the connection who requested to compute an input
     * @param input to be computed
     * @exception UndefinedInputTypeException thrown if the input isn't a data nor a defined-type message
     */
    private static void handleOthersInputs(SocketConnection connection, String input) {
        Optional<DecodingFunction<SocketConnection>> decodingFunction= Optional.ofNullable(INPUT_TYPES.getOrDefault(input, null));
        decodingFunction.ifPresentOrElse(
                function->function.run(connection),()->{throw new UndefinedInputTypeException();
                });
    }

    /**
     * @param string to be computed
     * @return a String marked as "DATA TYPE". If this string is sent to another host
     * it will be valuated as a data message.
     */
    static String computeOutput(String string){
        StringBuilder stringBuilder= new StringBuilder();
        stringBuilder.append(string);
        stringBuilder.insert(DATA_TAG_POSITION, dataMessage);
        return stringBuilder.toString();
    }

    /**
     * @param integer to be computed
     * @return a String marked as "DATA TYPE". If this string is sent to another host
     * it will be valuated as a data message.
     */
    static String computeOutput(int integer){
        StringBuilder stringBuilder= new StringBuilder();
        stringBuilder.append(integer);
        stringBuilder.insert(DATA_TAG_POSITION, dataMessage);
        return stringBuilder.toString();
    }

    /**
     * Getter for dataTag
     * @return dataTag
     */
    static String getDataTag(){
        return dataMessage;
    }

    /**
     * Getter for dataTagPosition
     * @return dataTagPosition
     */
    static int getDataTagPosition(){
        return DATA_TAG_POSITION;
    }

    /**
     * Getter for predicate "inputIsDataType"
     * @return inputIsDataType
     */
    static Predicate<String> getInputIsDataType(){
        return inputIsDataType;
    }

    /**
     * Getter for pingMessage
     * @return pingMessage
     */
    static String getPingMessage(){
        return PING_MESSAGE;
    }

    static String getHelloMessage() {
        return HELLO_MESSAGE;
    }

    static String getServerIsReadyMessage() {
        return SERVER_IS_READY_MESSAGE;
    }
}
