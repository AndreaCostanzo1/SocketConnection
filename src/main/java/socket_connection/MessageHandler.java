package socket_connection;


import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import socket_connection.socket_exceptions.runtime_exceptions.UndefinedInputTypeException;
import socket_connection.socket_exceptions.runtime_exceptions.BadSetupException;
import socket_connection.tools.ConfigurationHandler;
import socket_connection.tools.MessageHandlerConfigurations;

import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Predicate;

@FunctionalInterface
interface DecodingFunction<T>{
    void run(T t);
}

class MessageHandler {

    private static final Map<String, DecodingFunction<SocketConnection>> behavioursMap =new HashMap<>();

    private static Charset charset;
    private static String pingMessage;
    private static String dataMessage;
    private static String helloMessage;
    private static String serverIsReadyMessage;
    private static boolean configured=false;
    private static int dataTagPosition;
    private static Predicate<String> inputIsDataType;

    /**
     * public constructor of MessageHandler. This class is a singleton
     */
    MessageHandler(){
        setup();
    }

    /**
     * This method is used to setup the whole class
     */
    private static synchronized void setup() {
        if(!configured){
            MessageHandlerConfigurations config= ConfigurationHandler.getInstance().getMessageHandlerConfigurations();
            getMessages(config);
            setupBehaviour();
            configured=true;
        }
    }

    /**
     * This method is used to setup strings used to define the type of messages
     */
    private static void getMessages(MessageHandlerConfigurations config) {
        Objects.requireNonNull(config);
        pingMessage=config.getPingMessage();
        dataMessage=config.getDataMessage();
        dataTagPosition=config.getDataTagPosition();
        helloMessage=config.getHelloMessage();
        charset=config.getCharset();
        serverIsReadyMessage=config.getServerIsReadyMessage();
    }

    /**
     * This method is used to define handler behaviour when is asked to handle a message received
     */
    private static void setupBehaviour() {
        behavioursMap.put(pingMessage, CSMHandler::handlePingMessage);
        behavioursMap.put(helloMessage, CSMHandler::handleHelloMessage);
        behavioursMap.put(serverIsReadyMessage, CSMHandler::handleServerIsReadyMessage);
        inputIsDataType =
                s->s!=null && s.length()>=dataMessage.length() &&
                        s.substring(dataTagPosition,dataMessage.length()+ dataTagPosition).equals(dataMessage);
    }

    /**
     * This method valuate if the input is a ping message or not:
     * ping messages reset TTL of the socket
     * data messages are added to the buffer of the asking connection
     * @param connection is the connection who requested to compute an input
     * @param input to be computed
     * @exception UndefinedInputTypeException thrown if the input isn't a data nor a defined-type message
     */
    void computeInput(SocketConnection connection, String input){
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
    private void handleDataInput(SocketConnection connection,String input) {
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
     * {@link CSMHandler#handleHelloMessage(SocketConnection)}
     * {@link CSMHandler#handleServerIsReadyMessage(SocketConnection)}
     */
    private void handleOthersInputs(SocketConnection connection, String input) {
        Optional<DecodingFunction<SocketConnection>> decodingFunction= Optional.ofNullable(behavioursMap.getOrDefault(input, null));
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
    String computeOutput(String string){
        StringBuilder stringBuilder= new StringBuilder();
        stringBuilder.append(string);
        stringBuilder.insert(dataTagPosition, dataMessage);
        return box(stringBuilder.toString());
    }

    /**
     * @param integer to be computed
     * @return a string marked as "DATA TYPE". If this string is sent to another host
     * it will be valuated as a data message.
     */
    String computeOutput(int integer){
        StringBuilder stringBuilder= new StringBuilder();
        stringBuilder.append(integer);
        stringBuilder.insert(dataTagPosition, dataMessage);
        return box(stringBuilder.toString());
    }

    /**
     * @param data containing data to box
     * @return a string containing data as bytes
     */
    private String box(String data){
        Gson gson= new Gson();
        return gson.toJson(data.getBytes(charset));
    }

    /**
     * @param data containing data to box
     * @return a string containing data as bytes
     * @exception UndefinedInputTypeException is launched if the string received isn't a bytes representation of
     * something
     */
    private String unBox(String data){
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
    String getDataTag(){
        return dataMessage;
    }

    /**
     * Getter for dataTagPosition
     * @return dataTagPosition
     */
    int getDataTagPosition(){
        return dataTagPosition;
    }

    /**
     * Getter for predicate "inputIsDataType"
     * @return inputIsDataType
     */
    Predicate<String> getInputIsDataType(){
        return inputIsDataType;
    }

    /**
     * Getter for pingMessage
     * @return pingMessage
     */
    String getPingMessage(){
        return box(pingMessage);
    }

    /**
     * Getter for helloMessage
     * @return helloMessage
     */
    String getHelloMessage() {
        return box(helloMessage);
    }

    /**
     * Getter for serverIsReadyMessage
     * @return serverIsReadyMessage
     */
    String getServerIsReadyMessage() {
        return box(serverIsReadyMessage);
    }

    /**
     * @return used charset
     */
    Charset getUsedCharset(){
        return charset;
    }
}
