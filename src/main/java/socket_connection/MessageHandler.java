package socket_connection;


import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import socket_connection.socket_exceptions.runtime_exceptions.UndefinedInputTypeException;
import socket_connection.socket_exceptions.runtime_exceptions.BadSetupException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

@FunctionalInterface
interface DecodingFunction<T>{
    void run(T t);
}

final class MessageHandler {


    private static String dataMessage ="#DATA#";
    private static Charset charset=StandardCharsets.UTF_16;
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
        String data = unBox(input);
        if(inputIsDataType.test(data))
            handleDataInput(connection,data);
        else
            handleOthersInputs(connection,data);
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
     * @exception BadSetupException look at:
     * {@link InputDecoder#handleHelloMessage(SocketConnection)}
     * {@link InputDecoder#handleServerIsReadyMessage(SocketConnection)}
     */
    private static void handleOthersInputs(SocketConnection connection, String input) {
        Optional<DecodingFunction<SocketConnection>> decodingFunction= Optional.ofNullable(INPUT_TYPES.getOrDefault(input, null));
        decodingFunction.ifPresentOrElse(
                function->function.run(connection), /*if present*/
                ()->{throw new UndefinedInputTypeException(); /*if not present*/
                });
    }

    /**
     * @param string to be computed
     * @return a string marked as "DATA TYPE". If this string is sent to another host
     * it will be valuated as a data message.
     */
    static String computeOutput(String string){
        StringBuilder stringBuilder= new StringBuilder();
        stringBuilder.append(string);
        stringBuilder.insert(DATA_TAG_POSITION, dataMessage);
        return box(stringBuilder.toString());
    }

    /**
     * @param integer to be computed
     * @return a string marked as "DATA TYPE". If this string is sent to another host
     * it will be valuated as a data message.
     */
    static String computeOutput(int integer){
        StringBuilder stringBuilder= new StringBuilder();
        stringBuilder.append(integer);
        stringBuilder.insert(DATA_TAG_POSITION, dataMessage);
        return box(stringBuilder.toString());
    }

    /**
     * @param data containing data to box
     * @return a string containing data as bytes
     */
    private static String box(String data){
        Gson gson= new Gson();
        return gson.toJson(data.getBytes(charset));
    }

    /**
     * @param data containing data to box
     * @return a string containing data as bytes
     * @exception UndefinedInputTypeException is launched if the string received isn't a bytes representation of
     * something
     */
    private static String unBox(String data){
        Gson gson= new Gson();
        try {
            return new String(gson.fromJson(data, byte[].class), charset);
        }catch (JsonSyntaxException e){
            throw new UndefinedInputTypeException();
        }
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
        return box(PING_MESSAGE);
    }

    /**
     * Getter for helloMessage
     * @return helloMessage
     */
    static String getHelloMessage() {
        return box(HELLO_MESSAGE);
    }

    /**
     * Getter for serverIsReadyMessage
     * @return serverIsReadyMessage
     */
    static String getServerIsReadyMessage() {
        return box(SERVER_IS_READY_MESSAGE);
    }

    /**
     * @return used charset
     */
    static Charset getUsedCharset(){
        return charset;
    }
}
