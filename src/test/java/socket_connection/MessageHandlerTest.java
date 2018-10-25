package socket_connection;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import socket_connection.socket_exceptions.runtime_exceptions.UndefinedInputTypeException;
import socket_connection.socket_exceptions.runtime_exceptions.socket_connection_events.ConnectionEventException;
import socket_connection.socket_exceptions.runtime_exceptions.socket_connection_events.DataReceivedException;
import socket_connection.configurations.ConfigurationHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@FunctionalInterface
interface TriFunction<A,B,C,D>{
    A compute(B b,C c,D d);
}

class MessageHandlerTest {

    private static final TriFunction<String,String,Integer,String> computeMessage =
            (input, dataTagPosition, dataTag) -> new StringBuilder().append(input).insert(dataTagPosition, dataTag).toString();
    private static final MessageHandler messageHandler= new MessageHandler();

    /**
     * Before running messageHandler configurations need to be loaded.
     */
    @BeforeAll
    static void setupConfigurations(){
        ConfigurationHandler.getInstance();
    }

    //****************************************************************************************
    //
    //                         TEST: final parameters
    //
    //****************************************************************************************

    /**
     * this test assert that dataTagPosition is equals to 0.
     */
    @Test
    void tagPositionTest(){
        assertEquals(0, messageHandler.getDataTagPosition());
    }

    /**
     * this test assert that the PREDICATE inputIsDataType recognise a data type message
     */
    @Test
    void isDataTypeWithDataMessage(){
        String input="DataMessage";
        String dataTag= messageHandler.getDataTag();
        int dataTagPosition= messageHandler.getDataTagPosition();
        assertTrue(messageHandler.getInputIsDataType().test(computeMessage.compute(input,dataTagPosition,dataTag)));
    }

    /**
     * this test assert that the PREDICATE inputIsDataType recognise a non-data type message
     */
    @Test
    void isDataTypeWithPingMessage(){
        String notDataMessage="NotDataMessage";
        assertFalse(messageHandler.getInputIsDataType().test(notDataMessage));
    }

    /**
     * this test assert that the PREDICATE inputIsDataType never throw a StringOutOfBoundException
     */
    @Test
    void stringOutOfBoundException(){
        String s="";
        //this is the shortest string possible. If a stringOutOfBound exceptions isn't thrown there it couldn't be thrown
        //in any other case.
        assertFalse(messageHandler.getInputIsDataType().test(s));
    }

    /**
     * This test assert that every message type declared isn't null
     */
    @Test
    void notNullMessages(){
        assertNotNull(messageHandler.getDataTag());
        assertNotNull(messageHandler.getPingMessage());
        assertNotNull(messageHandler.getHelloMessage());
        assertNotNull(messageHandler.getServerIsReadyMessage());
    }
    //****************************************************************************************
    //
    //                         TEST: String computeOutput(String string)
    //
    //****************************************************************************************

    /**
     * This test assert that if we run computeOutput on a string the returned String
     * is RECOGNIZED as a DATA TYPE MESSAGE
     */
    @Test
    void comeOutputWithDefaultPositionAndStringParam(){
        String message="Random message";
        assertTrue(messageHandler.getInputIsDataType()
                .test(new String(new Gson().fromJson(messageHandler.computeOutput(message),byte[].class),
                        messageHandler.getUsedCharset())));
    }

    //****************************************************************************************
    //
    //                         TEST: String computeOutput(int number)
    //
    //****************************************************************************************

    /**
     * This test assert that if we run computeOutput on a int the returned string
     * is RECOGNIZED as a DATA TYPE MESSAGE
     */
    @Test
    void computeOutputWithDefaultPositionAndIntParam(){
        int message=10;
        assertTrue(messageHandler.getInputIsDataType()
                .test(new String(new Gson().fromJson(messageHandler.computeOutput(message),byte[].class),
                        messageHandler.getUsedCharset())));
    }

    //****************************************************************************************
    //
    //                         TEST: String computeInput(String string)
    //
    //****************************************************************************************

    /**
     * This test assert that a DATA TYPE MESSAGE is correctly refactored after being computed
     * with compute input
     */
    @Test
    void computeInputWithDefaultComputedDataType() {
        String message="Random message";
        String computedMessage=messageHandler.computeOutput(message);
        //check that the expected exception is thrown
        assertThrows(DataReceivedException.class,
                ()->messageHandler.computeInput(computedMessage));
        try{
            messageHandler.computeInput(computedMessage);
        } catch (DataReceivedException e){
            assertEquals(message,e.getEventData());
        }
    }

    /**
     * This test assert that computing a NOT DECODED MESSAGE cause a thrown of a
     * UndefinedInputTypeException
     */
    @Test
    void undefinedMessage(){
        String undefinedMessage="undefined";
        assertThrows(UndefinedInputTypeException.class,
                ()->messageHandler.computeInput(undefinedMessage));
    }

    /**
     * This test assert that computing a NON-DEFINED TYPE MESSAGE encoded and passed cause a thrown of a
     * UndefinedInputTypeException
     */
    @Test
    void undefinedEncodedMessage(){
        String undefinedMessage=new Gson().toJson(("undefined").getBytes(messageHandler.getUsedCharset()));
        assertThrows(UndefinedInputTypeException.class,
                ()->messageHandler.computeInput(undefinedMessage));
    }

    /**
     * This test check that defined message type are computed correctly.
     */
    @Test
    void checkCorrectComputationNonDataTypeDefinedMessages(){
        Set<String> messagesToTest= new HashSet<>();
        messagesToTest.add(messageHandler.getPingMessage());
        messagesToTest.add(messageHandler.getHelloMessage());
        messagesToTest.add(messageHandler.getServerIsReadyMessage());
        messagesToTest.forEach(message->assertFalse(messageHandler.getInputIsDataType().test(message)));
        messagesToTest.forEach(message->assertNoExceptionThrown(messageHandler::computeInput, ConnectionEventException.class).accept(message));
    }

    //---------------------------------------------------------------------------------------
    //
    //                                 SUPPORT METHODS
    //
    //---------------------------------------------------------------------------------------


    /**
     * This method check if the passed consumer throws an exception.
     * @param filterException avoid throwing of the exception passed.
     */
    @SuppressWarnings("all")
    private <T, E extends Exception> Consumer<T> assertNoExceptionThrown(Consumer<T> methodToCheck, Class<E> filterException) {
        return i -> {
            try{
                methodToCheck.accept(i);
            }catch (Exception e){
                try{
                    filterException.cast(e);
                } catch (ClassCastException e1){
                    throw e;
                }
            }
        };
    }

}
