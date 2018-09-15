package socket_connection;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import socket_connection.socket_exceptions.runtime_exceptions.BadSetupException;
import socket_connection.socket_exceptions.runtime_exceptions.UndefinedInputTypeException;
import socket_connection.socket_exceptions.exceptions.UnreachableHostException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

class SocketCommTemplate extends SocketConnection{
    SocketCommTemplate(){
        super();
        super.setToReady();
    }

    @Override
    void resetTTL(){
    }
}

@FunctionalInterface
interface TriFunction<A,B,C,D>{
    A compute(B b,C c,D d);
}

class MessageHandlerTest {

    private static final TriFunction<String,String,Integer,String> computeMessage =
            (input, dataTagPosition, dataTag) -> new StringBuilder().append(input).insert(dataTagPosition, dataTag).toString();

    //****************************************************************************************
    //
    //                         TEST: constructor
    //
    //****************************************************************************************

    /**
     * This test assert that we can't create instances of MessageHandler:
     * this is due to the fact that MessageHandler is a final class
     */
    @Test
    void cannotExistInstances() throws NoSuchMethodException {
        Constructor<MessageHandler> constructor=MessageHandler.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(AssertionError.class, ()->newInstance(constructor));
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
        assertEquals(0, MessageHandler.getDataTagPosition());
    }

    /**
     * this test assert that the PREDICATE inputIsDataType recognise a data type message
     */
    @Test
    void isDataTypeWithDataMessage(){
        String input="DataMessage";
        String dataTag= MessageHandler.getDataTag();
        int dataTagPosition= MessageHandler.getDataTagPosition();
        assertTrue(MessageHandler.getInputIsDataType().test(computeMessage.compute(input,dataTagPosition,dataTag)));
    }

    /**
     * this test assert that the PREDICATE inputIsDataType recognise a non-data type message
     */
    @Test
    void isDataTypeWithPingMessage(){
        String notDataMessage="NotDataMessage";
        assertFalse(MessageHandler.getInputIsDataType().test(notDataMessage));
    }

    /**
     * this test assert that the PREDICATE inputIsDataType never throw a StringOutOfBoundException
     */
    @Test
    void stringOutOfBoundException(){
        String s="";
        //this is the shortest string possible. If a stringOutOfBound exceptions isn't thrown there it couldn't be thrown
        //in any other case.
        assertFalse(MessageHandler.getInputIsDataType().test(s));
    }

    /**
     * This test assert that every message type declared isn't null
     */
    @Test
    void notNullMessages(){
        assertNotNull(MessageHandler.getDataTag());
        assertNotNull(MessageHandler.getPingMessage());
        assertNotNull(MessageHandler.getHelloMessage());
        assertNotNull(MessageHandler.getServerIsReadyMessage());
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
        assertTrue(MessageHandler.getInputIsDataType()
                .test(new String(new Gson().fromJson(MessageHandler.computeOutput(message),byte[].class), MessageHandler.getUsedCharset())));
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
        assertTrue(MessageHandler.getInputIsDataType()
                .test(new String(new Gson().fromJson(MessageHandler.computeOutput(message),byte[].class), MessageHandler.getUsedCharset())));
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
    void computeInputWithDefaultComputedDataType() throws UnreachableHostException {
        String message="Random message";
        String computedMessage=MessageHandler.computeOutput(message);
        SocketConnection mockSocket= new SocketCommTemplate();
        MessageHandler.computeInput(mockSocket, computedMessage);
        String elaboratedMessage= mockSocket.readData();
        assertEquals(message,elaboratedMessage);
    }

    /**
     * This test assert that computing a NOT DECODED MESSAGE cause a thrown of a
     * UndefinedInputTypeException
     */
    @Test
    void UndefinedMessage(){
        String undefinedMessage="undefined";
        SocketConnection mockSocket= new SocketCommTemplate();
        assertThrows(UndefinedInputTypeException.class,
                ()->MessageHandler.computeInput(mockSocket,undefinedMessage));
    }

    /**
     * This test assert that computing a NON-DEFINED TYPE MESSAGE encoded and passed cause a thrown of a
     * UndefinedInputTypeException
     */
    @Test
    void UndefinedEncodedMessage(){
        String undefinedMessage=new Gson().toJson(("undefined").getBytes(MessageHandler.getUsedCharset()));
        SocketConnection mockSocket= new SocketCommTemplate();
        assertThrows(UndefinedInputTypeException.class,
                ()->MessageHandler.computeInput(mockSocket,undefinedMessage));
    }

    /**
     * This test check that defined message type are computed correctly.
     */
    @Test
    void checkCorrectComputationNonDataTypeDefinedMessages(){
        Map<String,SocketConnection> messagesToTest= new HashMap<>();
        messagesToTest.put(MessageHandler.getPingMessage(), new SocketCommTemplate());
        messagesToTest.put(MessageHandler.getHelloMessage(), new SocketCommTemplate());
        messagesToTest.put(MessageHandler.getServerIsReadyMessage(), new SocketCommTemplate());
        messagesToTest.keySet().forEach(message->assertFalse(MessageHandler.getInputIsDataType().test(message)));
        messagesToTest.forEach((message,client)->assertNoExceptionThrown(MessageHandler::computeInput,BadSetupException.class).accept(client, message));
    }

    //---------------------------------------------------------------------------------------
    //
    //                                 SUPPORT METHODS
    //
    //---------------------------------------------------------------------------------------

    /**
     * This method try to create an instance of MessageHandler
     * @param constructor constructor of MessageHandler
     * @throws Throwable an eventual exception thrown using newInstance (expected AssertionError)
     */
    private void newInstance(Constructor<MessageHandler> constructor) throws Throwable {
        try {
            constructor.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }


    /**
     * This method check if the passed consumer throws an exception.
     * @param filterException avoid throwing of the exception passed.
     */
    @SuppressWarnings("all")
    private <T,R, E extends Exception> BiConsumer<T,R> assertNoExceptionThrown(BiConsumer<T, R> methodToCheck, Class<E> filterException) {
        return (i, j) -> {
            try{
                methodToCheck.accept(i, j);
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
