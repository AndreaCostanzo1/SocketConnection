package socket_connection;

import org.junit.jupiter.api.Test;
import socket_connection.socket_exceptions.UndefinedInputTypeException;
import socket_connection.socket_exceptions.UnreachableHostException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class SocketCommTemplate extends SocketConnection{
    SocketCommTemplate(){
        super();
        super.setToReady();
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
        String dataTag= MessageHandler.getDataTag();
        int dataTagPosition= MessageHandler.getDataTagPosition();
        String message="Random message";
        assertEquals(MessageHandler.computeOutput(message),computeMessage.compute(message,dataTagPosition,dataTag));
        assertTrue(MessageHandler.getInputIsDataType().test(MessageHandler.computeOutput(message)));
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
        String dataTag= MessageHandler.getDataTag();
        int dataTagPosition= MessageHandler.getDataTagPosition();
        int message=10;
        assertEquals(MessageHandler.computeOutput(message),new StringBuilder().append(message).insert(dataTagPosition, dataTag).toString());
        assertTrue(MessageHandler.getInputIsDataType().test(MessageHandler.computeOutput(message)));
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
        String elaboratedMessage= mockSocket.readUTF();
        assertEquals(message,elaboratedMessage);
    }

    @Test
    void checkNonDataDefinedTypeMessagesComputation(){

    }

    /**
     * This test assert that computing a NON-DEFINED TYPE MESSAGE cause a thrown of a
     * UndefinedInputTypeException
     */
    @Test
    void UndefinedMessage(){
        String undefinedMessage=MessageHandler.getPingMessage()+"undefined"; //like this we are sure that undefinedMessage!=pingMessage
        SocketConnection mockSocket= new SocketCommTemplate();
        assertThrows(UndefinedInputTypeException.class,
                ()->MessageHandler.computeInput(mockSocket,undefinedMessage));
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
}
