package socket_connection;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import socket_connection.socket_exceptions.exceptions.*;
import socket_connection.socket_exceptions.runtime_exceptions.UndefinedInputTypeException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static junit.framework.TestCase.assertTrue;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SocketConnectionTest {

    private static ServerSocketConnection commonServer;
    private static ServerSocketConnection commonServer2;
    private static ServerSocketConnection commonServer3;
    private static ServerSocketConnection commonServer4;
    private static ServerSocketConnection commonServer5;
    private static final int PORT1 =40001;
    private static final int PORT2= PORT1 +1;
    private static final int PORT3= PORT1 +2;
    private static final int PORT4 = PORT1 +3;
    private static final int PORT5 = PORT1 +4;


    /**
     * This method open a "common servers" that can be used to accept connections used
     * to a testing purpose
     */
    @BeforeAll
    static void openCommonServers() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException {
        commonServer=new ServerSocketConnection(PORT1, NormalAgent.class);
        commonServer2=new ServerSocketConnection(PORT2, Agent2.class);
        commonServer3=new ServerSocketConnection(PORT3, Agent3.class);
        commonServer4=new ServerSocketConnection(PORT4, Agent4.class);
        commonServer5=new ServerSocketConnection(PORT5,Agent5.class );
        await("Await server to be ready").atMost(1000, TimeUnit.MILLISECONDS).untilAsserted(()->
        assertEquals(commonServer.getStatus(),ServerSocketConnection.Status.RUNNING));
        await("Await server to be ready").atMost(1000, TimeUnit.MILLISECONDS).untilAsserted(()->
                assertEquals(commonServer2.getStatus(),ServerSocketConnection.Status.RUNNING));
    }

    /**
     * this method close all the "common servers"
     */
    @AfterAll
    static void shutdownCommonServer() throws ServerShutdownException {
        commonServer.shutdown();
        commonServer2.shutdown();
        commonServer3.shutdown();
        commonServer4.shutdown();
        commonServer5.shutdown();
    }



    //****************************************************************************************
    //
    //                         TEST: void readData() & writeData()
    //
    //****************************************************************************************
    /**
     * This method is used to test concurrency on outputStream: if not well developed it
     * will cause error during read.
     * If it fails, you should check if all locks are taken properly before using buffer
     */
    @Test
    void outputStreamWorksFine() throws FailedToConnectException {
        final int activatedThreads=5;
        SocketConnection connection=new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(), PORT1);
        //ensure that a not encoded message throws an UndefinedInputTypeException
        assertThrows(UndefinedInputTypeException.class,
                ()->MessageHandler.computeInput(connection, "random not encoded string"));
        ArrayList<Thread> threads=new ArrayList<>();
        for (int i=0; i<activatedThreads;i++) {
            threads.add(new Thread(this::openConnections));
            threads.get(i).start();
        }
        for (int i=0; i<activatedThreads;i++) {
            final int current=i;
            await().atMost(1000,TimeUnit.MILLISECONDS )
                    .untilAsserted(()->assertEquals(Thread.State.TERMINATED, threads.get(current).getState()));
        }


    }

    /**
     * This test uses a server side agent that will send a message and than he will close.
     * {@link Agent2#run()}
     *
     * It checks that the input read is equals to the expected input (sent with writeData from server)
     */
    @Test
    void testReadAndWriteUTF() throws FailedToConnectException, UnreachableHostException {
        SocketConnection connection=new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(), PORT2);
        assertEquals(Agent2.getMessageSent(), connection.readData());
    }


    //****************************************************************************************
    //
    //                         TEST: void readInt() & writeInt()
    //
    //****************************************************************************************

    /**
     * This test uses a server side agent that will send a int and a string and than he will close.
     * {@link Agent3#run()}
     *
     * It checks that the input read is equals to the expected input (sent with writeInt()),
     * and that a readInt called on a string message throws a {@link BadMessagesSequenceException}
     */
    @Test
    void testReadAndWriteInt() throws FailedToConnectException, UnreachableHostException, BadMessagesSequenceException {
        SocketConnection connection=new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(), PORT3);
        assertEquals(Agent3.getMessageSent(), connection.readInt());
        await().atMost(500,TimeUnit.MILLISECONDS)
                .untilAsserted(()->assertThrows(BadMessagesSequenceException.class, connection::readInt));
    }

    //****************************************************************************************
    //
    //                         TEST: isDataAvailable()
    //
    //****************************************************************************************

    /**
     * This test checks that data are available if the host on the other side send something
     */
    @Test
    void testIsDataAvailable() throws FailedToConnectException{
        SocketConnection connection = new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(), PORT3);
        await().atMost(500,TimeUnit.MILLISECONDS )
                .until(connection::isReady,is(true));
        await().atMost(500,TimeUnit.MILLISECONDS )
                .untilAsserted(()->assertTrue(connection.isDataAvailable()));
    }


    /**
     * This test checks that data are not available if the host on the other side doesn't send something
     */
    @Test
    void testIsDataNotAvailable() throws FailedToConnectException{
        SocketConnection connection = new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(), PORT5);
        await().atMost(500,TimeUnit.MILLISECONDS )
                .until(connection::isReady,is(true));
        assertFalse(connection.isDataAvailable());
        //set a casual timeout before checking data are still not available
        await().atLeast(500,TimeUnit.MILLISECONDS);
        assertFalse(connection.isDataAvailable());
    }

    //****************************************************************************************
    //
    //                         TEST: getPing()
    //
    //****************************************************************************************

    /**
     * This test checks that getPing run without problem on an open connection
     */
    @Test
    void getPingTest() throws FailedToConnectException, UnreachableHostException {
        SocketConnection connection= new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(), PORT1);
        connection.getPing();
    }

    /**
     * This test checks that getPing throws an {@link UnreachableHostException} when
     * used on a closed connection
     */
    @Test
    void getPingOnClosedConnectionTest() throws FailedToConnectException {
        SocketConnection connection= new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(),PORT4);
        //wait for connection to be ready
        await().atMost(500,TimeUnit.MILLISECONDS )
                .until(connection::isReady,is(true));
        //wait for connection to shutdown
        await().atMost(500,TimeUnit.MILLISECONDS )
                .untilAsserted(()->assertFalse(connection.isConnected()));
        assertThrows(UnreachableHostException.class, connection::getPing);
    }
    //---------------------------------------------------------------------------------------
    //
    //                                 SUPPORT METHODS
    //
    //---------------------------------------------------------------------------------------

    /**
     * This method open a connection and read inputs sent from a NormalAgent
     * {@link NormalAgent}
     */
    private void openConnections() {
        SocketConnection connection;
        try {
            connection = new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(), PORT1);
            for (int i = 0; i<NormalAgent.getCYCLES(); i++) {
                try {
                    connection.readData();
                } catch (UnreachableHostException e) {
                    e.printStackTrace();
                }
            }
        } catch (FailedToConnectException e) {
            e.printStackTrace();
        }
    }

}


class NormalAgent implements SocketUserAgentInterface{

    private SocketConnection connection;
    private Lock lock;
    private final static int CYCLES1 =7;
    private final static int CYCLES2 =7;

    public NormalAgent(){
        lock=new ReentrantLock();
    }


    @Override
    public void setConnection(SocketConnection connection) {
        this.connection=connection;
    }

    @Override
    public void shutdown() {
        connection.shutdown();
    }

    @Override @SuppressWarnings("all")
    public void run() {
        new Thread(()->sendMessages(connection)).start();
        int i=0;
        lock.lock();
        while(i<CYCLES1){
            i++;
            lock.unlock();
            try {
                connection.writeData("Stuff");
            } catch (UnreachableHostException e) {
                shutdown();
            }
            lock.lock();
        }
        lock.unlock();
    }

    private void sendMessages(SocketConnection connection) {
        try {
            for (int i=0; i<CYCLES2; i++) connection.writeData("Threaded stuff");
        } catch (UnreachableHostException e) {
            e.printStackTrace();
        }
    }


    static int getCYCLES() {
        return CYCLES1+CYCLES2;
    }
}
class Agent2 implements SocketUserAgentInterface{

    private SocketConnection connection;
    private static final String MESSAGE_SENT ="Stuff";

    public Agent2(){
    }


    @Override
    public void setConnection(SocketConnection connection) {
        this.connection=connection;
    }

    @Override
    public void shutdown() {
        connection.shutdown();
    }

    @Override @SuppressWarnings("all")
    public void run() {
        try {
            connection.writeData(MESSAGE_SENT);
        } catch (UnreachableHostException e) {
            e.printStackTrace();
        }
    }

    static String getMessageSent() {
        return MESSAGE_SENT;
    }
}

class Agent3 implements SocketUserAgentInterface{

    private SocketConnection connection;
    private static final int MESSAGE_SENT =2;

    public Agent3(){
    }


    @Override
    public void setConnection(SocketConnection connection) {
        this.connection=connection;
    }

    @Override
    public void shutdown() {
        connection.shutdown();
    }

    @Override @SuppressWarnings("all")
    public void run() {
        try {
            connection.writeInt(MESSAGE_SENT);
            connection.writeData("random message");
        } catch (UnreachableHostException e) {
            e.printStackTrace();
        }
    }

    static int getMessageSent() {
        return MESSAGE_SENT;
    }
}

class Agent4 implements SocketUserAgentInterface{

    private SocketConnection connection;

    public Agent4(){
    }


    @Override
    public void setConnection(SocketConnection connection) {
        this.connection=connection;
    }

    @Override
    public void shutdown() {
        connection.shutdown();
    }

    @Override @SuppressWarnings("all")
    public void run() {
        connection.shutdown();
    }

}

class Agent5 implements SocketUserAgentInterface{

    private SocketConnection connection;
    private boolean shutdown;
    private Lock lock;

    public Agent5(){
        shutdown=false;
        lock=new ReentrantLock();
    }


    @Override
    public void setConnection(SocketConnection connection) {
        this.connection=connection;
    }

    @Override
    public void shutdown() {
        connection.shutdown();
        lock.lock();
        shutdown=true;
        lock.unlock();
    }

    @Override @SuppressWarnings("all")
    public void run() {
        while (shutdown){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}