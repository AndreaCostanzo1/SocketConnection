package socket_connection;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import socket_connection.socket_exceptions.exceptions.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.BindException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;

class ServerSocketConnectionTest {

    private static int port=14000;
    private static Lock lock=new ReentrantLock();
    private static Lock listLock=new ReentrantLock();
    private static List<ServerSocketConnection> servers=new ArrayList<>();

    /**
     * This method is used to ensure that all opened servers are shut down
     */
    @AfterAll
    static void shutdownAll(){
        listLock.lock();
        servers.stream().filter(server->!server.getStatus().equals(ServerSocketConnection.Status.SHUT_DOWN))
                .collect(Collectors.toList())
                .forEach(server-> {
                    try {
                        server.shutdown();
                    } catch (ServerShutdownException e) {
                        e.printStackTrace();
                    }
                });

    }

    //****************************************************************************************
    //
    //                         TEST: Constructors
    //
    //****************************************************************************************

    /**
     * In this test we will check that after creating a ServerSocketConnection
     * with a proper user agent the server is running.
     */
    @Test
    void serverCreationWithProperUserAgent() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        addServerToList(server);
        await("Avoid eventual time wait").atMost(200, TimeUnit.MILLISECONDS )
                .untilAsserted(()->assertEquals(server.getState(),Thread.State.RUNNABLE));
    }

    /**
     * In this test we will check that trying to open a server on the same port of
     * an opened server will throw an exception
     */
    @Test
    void openTwoServersOnTheSamePort() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        addServerToList(server);
        await("Avoid eventual time wait").atMost(200, TimeUnit.MILLISECONDS )
                .untilAsserted(()->assertEquals(server.getState(),Thread.State.RUNNABLE));
        assertThrows(BindException.class,()->new ServerSocketConnection(localPort,ProperAgent.class ));
    }

    /**
     * In this test we will check that, when manualStart==false, after creating a ServerSocketConnection
     * with a proper user agent, the server is running
     */
    @Test
    void serverCreationWithProperUserAgentAndNotManualStart() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class,false);
        addServerToList(server);
        await("Avoid eventual time wait").untilAsserted(()->assertEquals(server.getState(),Thread.State.RUNNABLE));
    }

    /**
     * In this test we will check that if manualStart==true
     * the thread need to be started manually.
     */
    @Test
    void serverCreationWithProperUserAgentAndManualStart() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class,true);
        addServerToList(server);
        assertEquals(Thread.State.NEW,server.getState());
        server.start();
        await("Avoid eventual time wait").atMost(200, TimeUnit.MILLISECONDS )
                .untilAsserted(()->assertEquals(server.getState(),Thread.State.RUNNABLE));
    }

    /**
     * In this test we will check that creating a ServerSocketCommunication
     * passing a UserAgent with NO DEFAULT CONSTRUCTOR (constructor with no parameters)
     * will THROW a NoDefaultConstructorException
     */
    @Test
    void serverCreationWithNOTProperUserAgent() {
        final int localPort=getPort();
        assertThrows(NoDefaultConstructorException.class,
                ()-> new ServerSocketConnection(localPort, NotProperConstructorAgent.class,true));
    }

    /**
     * In this test we will check that creating a ServerSocketCommunication
     * passing a UserAgent with NOT ACCESSIBLE DEFAULT CONSTRUCTOR (constructor with no parameters)
     * will THROW a NoDefaultConstructorException
     */
    @Test
    void serverCreationWithNOTAccessibleConstructorUserAgent() {
        final int localPort=getPort();
        assertThrows(IllegalAccessException.class,
                ()-> new ServerSocketConnection(localPort, NotAccessibleConstructorAgent.class,true));
    }

    /**
     * In this test we will check that creating a ServerSocketCommunication
     * passing an ABSTRACT CLASS as parameter will THROW a InstantiationException
     */
    @Test
    void serverCreationWithAbstractUserAgent() {
        final int localPort=getPort();
        assertThrows(InstantiationException.class,
                ()-> new ServerSocketConnection(localPort, AbstractAgent.class,true));
    }

    /**
     * In this test we will check that creating a ServerSocketCommunication
     * passing a CLASS that throw for a reason an exception during instantiation
     * will THROW a InvocationTargetException
     */
    @Test
    void serverCreationWithThrowingExceptionUserAgent() {
        final int localPort=getPort();
        assertThrows(InvocationTargetException.class,
                ()-> new ServerSocketConnection(localPort, ThrowingExceptionAgent.class,true));
    }

    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //
    //                         TEST: connection & activeConnections()
    //
    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    /**
     * In this test we check that after server is opened a client can connect without problem
     * and receive message sent from server
     */
    @Test
    void connectionTest() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException, FailedToConnectException {
        final int localPort = getPort();
        ServerSocketConnection server=new ServerSocketConnection(localPort, ProperAgent.class);
        SocketConnection connection = new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(), localPort);
        addServerToList(server);
        ProperAgent.getMessages().forEach(message -> assertIsReceived(message, connection));
    }


    //****************************************************************************************
    //
    //                         TEST: void shutdown()
    //
    //****************************************************************************************

    /**
     * This test check that after running a shutdown command the server shut down properly:
     * ->   Server's thread is terminated.
     * ->   No one can connect after shut down
     * ->   All previous opened connection are now closed.
     */
    @Test
    void serverShutDownProperly() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException, FailedToConnectException, ServerShutdownException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        addServerToList(server);

        //check server is running
        await("Avoid eventual time waiting due to delay").atMost(200, TimeUnit.MILLISECONDS )
                .untilAsserted(()->assertEquals(Thread.State.RUNNABLE,server.getState()));
        List<SocketConnection> openedConnections= new ArrayList<>();
        openedConnections.add(new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(), localPort));
        openedConnections.add(new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(), localPort));

        //check all connections are opened
        openedConnections.forEach(connection ->
                await("Waiting for connection to be ready")
                        .until(connection::isReady, is(true)));

        //Now server is shut down.
        server.shutdown();
        await("Waiting for thread to close properly").atMost(200, TimeUnit.MILLISECONDS )
                .untilAsserted(()->assertEquals(server.getState(),Thread.State.TERMINATED));

        /*  connection previous opened should be now closed:
         *      so if a sent a message to server an
         *      UnreachableHostException should be thrown
         */
        openedConnections.forEach(connection ->
                await("Waiting for connection to close")
                        .atMost(500, TimeUnit.MILLISECONDS)
                        .until(connection::isConnected,is(false)));

        //The server is down so I can't open a connection anymore.
        assertThrows(FailedToConnectException.class,
                ()->new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(), localPort));

    }

    /**
     * This test assure that after closing a server on port X, a new server can be opened
     * on the same port
     */
    @Test
    void portNotBindAfterShutDown() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException, ServerShutdownException {
        final int localPort=getPort();
        final ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        await("Avoid eventual time waiting due to delay").untilAsserted(()->assertEquals(Thread.State.RUNNABLE,server.getState()));
        //now server is shut down.
        server.shutdown();
        await("Waiting for thread to shut down properly").untilAsserted(()->assertEquals(Thread.State.TERMINATED,server.getState()));
        //try to open a new server on the same port
        final ServerSocketConnection server2= new ServerSocketConnection(localPort, ProperAgent.class);
        addServerToList(server2);
        await("Avoid eventual time waiting due to delay").atMost(200, TimeUnit.MILLISECONDS )
                .untilAsserted(()->assertEquals(Thread.State.RUNNABLE,server2.getState()));
    }

    /**
     * This test assure that a server can't be shut down twice
     */
    @Test
    void cannotShutDownServerTwice() throws ServerShutdownException, InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException {
        final int localPort=getPort();
        final ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        addServerToList(server);
        await("Avoid eventual time waiting due to delay").atMost(200, TimeUnit.MILLISECONDS )
                .untilAsserted(()->assertEquals(Thread.State.RUNNABLE,server.getState()));
        //now server is shut down.
        server.shutdown();
        await("Waiting for thread to close properly").atMost(200, TimeUnit.MILLISECONDS )
                .untilAsserted(()->assertEquals(Thread.State.TERMINATED,server.getState()));
        assertThrows(ServerShutdownException.class,
                server::shutdown);
    }

    /**
     * This test ensure that a server closed from at least 50ms shut down without problems
     */
    @Test
    void shutDownAJUSTClosedServer() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException, ServerShutdownException, ServerAlreadyClosedException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        addServerToList(server);
        await("Avoid eventual time waiting due to delay").atMost(200, TimeUnit.MILLISECONDS )
                .untilAsserted(()->assertEquals(Thread.State.RUNNABLE,server.getState()));
        server.close();
        server.shutdown();
        assertEquals(ServerSocketConnection.Status.SHUT_DOWN, server.getStatus());
        await("Waiting for thread to shut down properly").atMost(500, TimeUnit.MILLISECONDS )
                .until(server::getState,is(Thread.State.TERMINATED));
    }

    /**
     * This test ensure that a server closed from at least 10ms shut down without problems
     */
    @Test
    void shutDownAClosedServerFromMAX10MS() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException, ServerShutdownException, ServerAlreadyClosedException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        addServerToList(server);
        await("Avoid eventual time waiting due to delay").untilAsserted(()->assertEquals(Thread.State.RUNNABLE,server.getState()));
        server.close();
        await().atMost(10,TimeUnit.MILLISECONDS);
        server.shutdown();
        assertEquals(ServerSocketConnection.Status.SHUT_DOWN, server.getStatus());
        await("Waiting for thread to shut down properly").atMost(500, TimeUnit.MILLISECONDS )
                .until(server::getState,is(Thread.State.TERMINATED));
    }

    /**
     * This test ensure that a server closed from at least 50ms shut down without problems
     */
    @Test
    void shutDownAClosedServerFromMAX50MS() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException, ServerShutdownException, ServerAlreadyClosedException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        addServerToList(server);
        await("Avoid eventual time waiting due to delay").atMost(200, TimeUnit.MILLISECONDS )
                .untilAsserted(()->assertEquals(Thread.State.RUNNABLE,server.getState()));
        server.close();
        await().atMost(50,TimeUnit.MILLISECONDS);
        server.shutdown();
        assertEquals(ServerSocketConnection.Status.SHUT_DOWN, server.getStatus());
        await("Waiting for thread to shut down properly").atMost(500, TimeUnit.MILLISECONDS )
                .until(server::getState,is(Thread.State.TERMINATED));
    }

    /**
     * This test ensure that a server closed from at least 300ms shut down without problems
     */
    @Test
    void shutDownAClosedServerFromMAX300MS() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException, ServerShutdownException, ServerAlreadyClosedException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        addServerToList(server);
        await("Avoid eventual time waiting due to delay").atMost(400, TimeUnit.MILLISECONDS )
                .untilAsserted(()->assertEquals(Thread.State.RUNNABLE,server.getState()));
        server.close();
        await().atMost(300,TimeUnit.MILLISECONDS);
        server.shutdown();
        assertEquals(ServerSocketConnection.Status.SHUT_DOWN, server.getStatus());
        await("Waiting for thread to shut down properly").atMost(500, TimeUnit.MILLISECONDS )
                .until(server::getState,is(Thread.State.TERMINATED));
    }

    /**
     * This test ensure that a server surely closed shut down without problems
     */
    @Test
    void shutDownASurelyClosed() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException, ServerShutdownException, ServerAlreadyClosedException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        addServerToList(server);
        await("Avoid eventual time waiting due to delay").atMost(200, TimeUnit.MILLISECONDS )
                .untilAsserted(()->assertEquals(Thread.State.RUNNABLE,server.getState()));
        server.close();
        await("Waiting for thread to close properly").atMost(500, TimeUnit.MILLISECONDS )
                .until(server::getState,is(Thread.State.WAITING));
        assertEquals(ServerSocketConnection.Status.CLOSED, server.getStatus());
        server.shutdown();
        assertEquals(ServerSocketConnection.Status.SHUT_DOWN, server.getStatus());
        await("Waiting for thread to shut down properly").atMost(500, TimeUnit.MILLISECONDS )
                .until(server::getState,is(Thread.State.TERMINATED));
    }

    //****************************************************************************************
    //
    //                         TEST: void close()
    //
    //****************************************************************************************

    /**
     * This test assure that the server close properly:
     * -> server goes in a wait status
     * -> connection opened before closing are still available
     * -> new connections can't be opened.
     */
    @Test
    void serverClosedProperly() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException, ServerAlreadyClosedException, ServerShutdownException, FailedToConnectException{
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        //opening a connection before server.close command.
        SocketConnection connection= new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(), localPort);
        await("Waiting for connection to be ready").until(connection::isReady,is(true));
        //closing server
        server.close();
        //check server is in a wait status
        await("Waiting for thread to get in a wait status").atMost(500, TimeUnit.MILLISECONDS )
                .until(server::getState,is(Thread.State.WAITING));
        //The server is closed so I can't open a connection anymore.
        assertThrows(FailedToConnectException.class,
                ()->new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(), localPort));
        //Server is not down so connections opened before using server.close should still be available
        assertTrue(connection.isConnected(),"Host should be reachable");
    }

    /**
     * This test assure that a server can't be closed twice without re-opening it.
     */
    @Test
    void serverCannotBeClosedTwice() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException, ServerAlreadyClosedException, ServerShutdownException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        server.close();
        assertThrows(ServerAlreadyClosedException.class,
                server::close);
    }

    /**
     * This test assure that a shut down server can't be closed
     */
    @Test
    void cannotCloseAShutdownServer() throws ServerShutdownException, InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        server.shutdown();
        assertThrows(ServerShutdownException.class,
                server::close);
    }

    //****************************************************************************************
    //
    //                         TEST: void open()
    //
    //****************************************************************************************

    /**
     * This test will assure that open can't be used without using closed before.
     */
    @Test
    void cannotOpenBeforeClosing() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        assertThrows(ServerAlreadyOpenedException.class,
                server::open);
    }

    /**
     * This test will assure that server is re-opened properly:
     *  ->Server is RUNNABLE again
     *  ->New connections can be opened
     */
    @Test
    void serverReOpenedProperly() throws ServerAlreadyClosedException, ServerShutdownException, InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException, ServerAlreadyOpenedException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        server.close();
        await("Waiting for thread to close properly").atMost(600, TimeUnit.MILLISECONDS )
                .until(server::getState,is(Thread.State.WAITING));

        //Reopen the server
        server.open();
        //check server is RUNNABLE
        await("Waiting for thread to re-open properly").atMost(600, TimeUnit.MILLISECONDS )
                .until(server::getState,is(Thread.State.RUNNABLE));
        //check that new connections can be opened
        try {
            new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(),localPort);
        } catch (FailedToConnectException e) {
            fail("Connection should be opened", e);
        }
    }

    /**
     * This test assure that a shut down server can't be closed
     */
    @Test
    void cannotOpenAShutdownServer() throws ServerShutdownException, InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        server.shutdown();
        assertThrows(ServerShutdownException.class,
                server::open);
    }

    //****************************************************************************************
    //
    //                         TEST: void getStatus()
    //
    //****************************************************************************************

    /**
     * This assert that the method getStatus() return the right state after
     * each operation.
     */
    @Test
    void getStatusTest() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException, ServerShutdownException, ServerAlreadyClosedException, ServerAlreadyOpenedException {
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class,true);
        addServerToList(server);
        //server still not started
        assertEquals(ServerSocketConnection.Status.WAITING_LAUNCH,server.getStatus());
        //start server
        server.start();
        //server started and should be running
        assertEquals(ServerSocketConnection.Status.RUNNING,server.getStatus());
        //close server
        server.close();
        //server should be closed
        assertEquals(ServerSocketConnection.Status.CLOSED,server.getStatus());
        //reopen server
        server.open();
        //server should be running
        assertEquals(ServerSocketConnection.Status.RUNNING,server.getStatus());
        //shut down server
        server.shutdown();
        //server shut down--> isRunning should return false;
        assertEquals(ServerSocketConnection.Status.SHUT_DOWN,server.getStatus());
    }

    //****************************************************************************************
    //
    //                         TEST: int activeConnections()
    //
    //****************************************************************************************

    /**
     * This test assert that each connection/disconnection update the number
     * of activeConnections
     */
    @Test
    void activeConnectionTest() throws InvocationTargetException, NoDefaultConstructorException, InstantiationException, IllegalAccessException, IOException, FailedToConnectException {
        ArrayList<SocketConnection> connections=new ArrayList<>();
        final int localPort=getPort();
        ServerSocketConnection server= new ServerSocketConnection(localPort, ProperAgent.class);
        addServerToList(server);
        //test available connections after each connection
        for (int i=0; i< 1;i++){
            connections.add(new SocketConnection(InetAddress.getLoopbackAddress().getHostAddress(),localPort));
            await().until(server::activeConnections,is(i+1));
        }
        //test available connections after each disconnection
        for (int i=0; i< connections.size();i++){
            connections.get(i).shutdown();
            await().until(server::activeConnections,is(connections.size()-i));
        }

    }

    //---------------------------------------------------------------------------------------
    //
    //                                 SUPPORT METHODS
    //
    //---------------------------------------------------------------------------------------

    /**
     * This method is used to get a not bind port
     * @return a port
     */
    private int getPort() {
        lock.lock();
        final int localPort=port++;
        lock.unlock();
        return localPort;
    }

    /**
     * assert that the message read from the connection is equals to the string passed
     */
    private void assertIsReceived(String message,SocketConnection connection) {
        try {
            assertEquals(message, connection.readString());
        } catch (UnreachableHostException e) {
            fail("connection should be available");
        }
    }

    /**
     * This methods add each opened server to a list
     */
    private void addServerToList(ServerSocketConnection server) {
        listLock.lock();
        servers.add(server);
        listLock.unlock();
    }
}

class ProperAgent implements SocketUserAgentInterface{

    private SocketConnection connection;
    private boolean shutdown;
    private Lock lock;
    private static final List<String> messages;
    static{
        messages=new ArrayList<>();
        messages.add("Message 1");
        messages.add("Message 2");
        messages.add("Message 3");
        messages.add("Message 4");
        messages.add("Message 5");
    }

    public ProperAgent(){
        shutdown=false;
        lock=new ReentrantLock();
    }

    static List<String> getMessages(){
        synchronized (messages){return new ArrayList<>(messages);}
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
        synchronized (messages){
            messages.forEach(message-> {
                try {
                    connection.writeString(message);
                } catch (UnreachableHostException e) {
                    e.printStackTrace();
                }
            });
        }
        lock.lock();
        while(!shutdown){
            lock.unlock();
            doCasualStuff();
            lock.lock();
        }
        lock.unlock();
    }

    @SuppressWarnings("all")
    private void doCasualStuff() {
        int a=1,b=2;
        int c=a+b;
    }
}

@SuppressWarnings("all")
class NotProperConstructorAgent implements SocketUserAgentInterface{

    public NotProperConstructorAgent(String parameter){

    }

    @Override
    public void setConnection(SocketConnection connection) {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void run() {

    }
}

@SuppressWarnings("all")
class NotAccessibleConstructorAgent implements SocketUserAgentInterface{

    private NotAccessibleConstructorAgent(){

    }

    @Override
    public void setConnection(SocketConnection connection) {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void run() {

    }
}

@SuppressWarnings("all")
abstract class AbstractAgent implements SocketUserAgentInterface{
    public void AbstactAgent(){

    }
}

@SuppressWarnings("all")
class ThrowingExceptionAgent implements SocketUserAgentInterface{

    ThrowingExceptionAgent(){
        throw new RuntimeException();
    }
    @Override
    public void setConnection(SocketConnection connection) {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void run() {

    }
}
