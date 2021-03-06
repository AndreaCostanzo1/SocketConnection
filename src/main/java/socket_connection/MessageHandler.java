package socket_connection;


import socket_connection.cryptography.exceptions.NullKeyException;
import socket_connection.socket_exceptions.runtime_exceptions.socket_connection_events.ConnectionEventException;
import socket_connection.socket_exceptions.runtime_exceptions.socket_connection_events.DataReceivedException;
import socket_connection.socket_exceptions.runtime_exceptions.UndefinedInputTypeException;
import socket_connection.socket_exceptions.runtime_exceptions.socket_connection_events.HelloEventException;
import socket_connection.socket_exceptions.runtime_exceptions.socket_connection_events.ServerReadyException;
import socket_connection.configurations.ConfigurationHandler;
import socket_connection.tools.DataFormatter;
import socket_connection.configurations.MessageHandlerConfigurations;

import java.nio.charset.Charset;
import java.security.Key;
import java.security.PrivateKey;
import java.util.*;
import java.util.function.Predicate;



class MessageHandler {

    private static final Map<String, Optional<DecodingFunction<MessageHandler,String>>> behavioursMap =new HashMap<>();
    private DataFormatter dataFormatter;
    private static String pingMessage;
    private static String dataMessage;
    private static String helloMessage;
    private static String serverIsReadyMessage;
    private static boolean configured=false;
    private static int dataTagPosition;
    private static Predicate<String> inputIsDataType;

    /**
     * This class work is to handle all kind of defined-type messages that are used
     * from SocketConnection to getInstance, to check if the connection is still active and
     * other stuff.
     *
     * CSM means ConnectionStatusMessages
     */
    // FIXME: 24/10/2018 if no computation of the input is required, remove this class and just throw the various exceptions
    private static final class ConnectionMessagesHandler{
        private ConnectionMessagesHandler(){
            throw new AssertionError();
        }

        /**
         * This method handles a hello message from the client
         * @param handler which received the message
         */
        static void handleHelloMessage(MessageHandler handler,String input){
            throw new HelloEventException(input);
        }

        /**
         * This method handles the message sent from the server to notify client that
         * it's ready
         * @param handler which received the message
         */
        static void handleServerIsReadyMessage(MessageHandler handler,String input) {
            throw new ServerReadyException(input);
        }

    }


    /**
     * public constructor of MessageHandler. This class is a singleton
     */
    MessageHandler(){
        MessageHandlerConfigurations config= ConfigurationHandler.getInstance().getMessageHandlerConfigurations();
        setupClass(config);
        setupInstance(config);
    }

    /**
     * This method is used to setupClass the whole class
     */
    private static synchronized void setupClass(MessageHandlerConfigurations config) {
        if(!configured){
            getMessages(config);
            setupBehaviour();
            configured=true;
        }
    }

    /**
     * This method is used to set up encryption
     * @param myPrivateKey contains the key that will be used to decrypt remote messages
     * @param foreignPublicKey contains the key used to encrypt data to be sent to the remote host
     * @throws NullKeyException if one of the keys is null
     */
    void setUpEncryption(PrivateKey myPrivateKey, Key foreignPublicKey) throws NullKeyException {
        if(myPrivateKey==null || foreignPublicKey==null) throw new NullKeyException();
        dataFormatter.setUpEncryption(myPrivateKey,foreignPublicKey);
    }


    /**
     * This method is used to setupClass the parameters of a single instance
     */
    private void setupInstance(MessageHandlerConfigurations config) {
        dataFormatter=new DataFormatter(config.getCharset());
    }

    /**
     * This method is used to setupClass strings used to define the type of messages
     */
    private static void getMessages(MessageHandlerConfigurations config) {
        Objects.requireNonNull(config);
        pingMessage=config.getPingMessage();
        dataMessage=config.getDataMessage();
        dataTagPosition=config.getDataTagPosition();
        helloMessage=config.getHelloMessage();
        serverIsReadyMessage=config.getServerIsReadyMessage();
    }

    /**
     * This method is used to define handler behaviour when is asked to handle a message received
     */
    private static void setupBehaviour() {
        behavioursMap.put(pingMessage, Optional.empty());
        behavioursMap.put(helloMessage, Optional.of(ConnectionMessagesHandler::handleHelloMessage));
        behavioursMap.put(serverIsReadyMessage, Optional.of(ConnectionMessagesHandler::handleServerIsReadyMessage));
        inputIsDataType =
                s->s!=null && s.length()>=dataMessage.length() &&
                        s.substring(dataTagPosition,dataMessage.length()+ dataTagPosition).equals(dataMessage);
    }

    /**
     * This method valuate if the input is a ping message or not:
     * ping messages reset TTL of the socket
     * data messages are added to the buffer of the asking connection
     * @param input to be computed
     * @exception UndefinedInputTypeException thrown if the input isn't a data nor a defined-type message
     */
    void computeInput(String input) throws ConnectionEventException{
        String data = dataFormatter.unBox(input);
        if(inputIsDataType.test(data))
            handleDataInput(data);
        else
            handleOthersInputs(data);
    }

    /**
     * This method is used to refactor data messages and add them to buffer
     * @param input to be computed
     * @exception DataReceivedException is thrown to let the respective
     *                                  connection knows about this event
     */
    private void handleDataInput(String input) {
        String data=input.replace(dataMessage,"");
        throw new DataReceivedException(data);
    }

    /**
     * This method is used to handle non data messages
     * @param input to be computed
     * @exception UndefinedInputTypeException thrown if the input isn't a data nor a defined-type message
     * @exception ConnectionEventException can be thrown after a event
     *                                     connection knows about this event
     */
    private void handleOthersInputs(String input) throws ConnectionEventException {
        Optional<Optional<DecodingFunction<MessageHandler,String>>> handler=
                Optional.ofNullable(behavioursMap.getOrDefault(input, null));
        if(handler.isPresent()){
            handler.get().ifPresent(decodingFunction -> decodingFunction.run(this,input));
        } else {
            throw new UndefinedInputTypeException();
        }
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
        return dataFormatter.box(stringBuilder.toString());
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
        return dataFormatter.box(stringBuilder.toString());
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
        return dataFormatter.box(pingMessage);
    }

    /**
     * Getter for helloMessage
     * @return helloMessage
     */
    String getHelloMessage() {
        return dataFormatter.box(helloMessage);
    }

    /**
     * Getter for serverIsReadyMessage
     * @return serverIsReadyMessage
     */
    String getServerIsReadyMessage() {
        return dataFormatter.box(serverIsReadyMessage);
    }

    /**
     * @return used charset
     */
    Charset getUsedCharset(){
        return dataFormatter.getCharset();
    }
}

@FunctionalInterface
interface DecodingFunction<T,R>{
    void run(T t, R r);
}
