package socket_connection;

import socket_connection.socket_exceptions.exceptions.BadMessagesSequenceException;
import socket_connection.socket_exceptions.exceptions.FailedToConnectException;
import socket_connection.socket_exceptions.exceptions.UnreachableHostException;
import socket_connection.socket_exceptions.runtime_exceptions.*;
import socket_connection.socket_exceptions.runtime_exceptions.socket_connection_events.ConnectionEventException;
import socket_connection.socket_exceptions.runtime_exceptions.socket_connection_events.DataReceivedException;
import socket_connection.socket_exceptions.runtime_exceptions.socket_connection_events.HelloEventException;
import socket_connection.socket_exceptions.runtime_exceptions.UndefinedInputTypeException;
import socket_connection.socket_exceptions.runtime_exceptions.socket_connection_events.ServerReadyException;
import socket_connection.tools.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class SocketConnection extends Thread{

    private Socket socket;
    private ServerSocketConnection handlingServer;
    private SynchronizedDataBuffer synchronizedBuffer;
    private SocketStreamsHandler socketStreamsHandler;
    private ConnectionTimer timer;
    private boolean shutdown;
    private boolean active;
    private boolean ready;
    private boolean serverSide;
    private Lock statusLock;
    private Condition statusCondition;
    private MessageHandler messageHandler;
    private static Map<Class<? extends ConnectionEventException>, eventComputer<SocketConnection,ConnectionEventException>> eventAdministrator=
            new HashMap<>();
    static {
        eventAdministrator.put(DataReceivedException.class,EventAdministrator::handleDataReception);
        eventAdministrator.put(HelloEventException.class,EventAdministrator::handleHelloMessage);
        eventAdministrator.put(ServerReadyException.class,EventAdministrator::handleServerIsReadyMessage);
    }
    private long delayInMs;
    private int maxReads;
    private boolean enabledMaxReads;
    private int timeToLive;

    /**
     * Package-private constructor: this is used from others constructors
     * to initialize some fields.
     */
    private SocketConnection(){
        setupConfigurations();
        this.synchronizedBuffer =new SynchronizedDataBuffer();
        this.messageHandler= new MessageHandler();
        this.statusLock =new ReentrantLock();
        this.statusCondition=statusLock.newCondition();
        this.timer=new ConnectionTimer(this);
        shutdown=false;
        ready=false;
    }

    /**
     * Package-private constructor for socket connection. This is called
     * from a ServerSocketConnection to getInstance an incoming connection
     * @param socket is the socket relative to the accepted connection
     * @param server is the server handling the connection
     * @throws FailedToConnectException if the connection is closed before
     * the ending of the getInstance phase
     */
    SocketConnection(Socket socket, ServerSocketConnection server) throws FailedToConnectException {
        this();
        this.serverSide=true;
        this.active=false;
        this.handlingServer=server;
        this.socket=socket;
        this.socketStreamsHandler= new SocketStreamsHandler(socket);
        this.start();
    }

    /**
     * Public constructor used to create a connection towards a server
     * @param ip is the ip-address of the server
     * @param port the port on which the server is listening
     * @throws FailedToConnectException if the server is unreachable
     */
    @SuppressWarnings("WeakerAccess")
    public SocketConnection(String ip, int port) throws FailedToConnectException {
        this();
        serverSide=false;
        this.active=true;
        try {
            socket=new Socket(ip,port);
        } catch (IOException e) {
            throw new FailedToConnectException();
        }
        this.socketStreamsHandler= new SocketStreamsHandler(socket);
        this.start();
    }

    /**
     * This method is used to getInstance
     */
    private void setupConfigurations() {
        SocketConnectionConfigurations config= ConfigurationHandler.getInstance().getSocketConnectionConfigurations();
        this.delayInMs=config.getDelayInMs();
        this.maxReads=config.getMaxReads();
        this.enabledMaxReads=config.isEnabledMaxReads();
        this.timeToLive =config.getTimeToLive();
    }



    /**
     * The thread started to handle the connection
     */
    @Override
    public void run(){
        setupConnection();
        handleSession();
        tearDownConnection();
    }

    /**
     * This method is used to getInstance the connection
     */
    private void setupConnection() {
        if(serverSide)
            handleServerSideSetup();
        else
            handleClientSideSetup();
        timer.launch();
    }

    /**
     * This method is used to getInstance the connection
     * if the socket represent the client side-connection
     */
    private void handleClientSideSetup() {
        try{
            sendHelloToServer();
            waitForServerToBeReady();
        }catch (IOException e){
            shutdown();
        }

    }

    /**
     * This method is used to send a "hello message" to the server.
     * @throws IOException if the server is unreachable
     */
    private void sendHelloToServer() throws IOException {
        socketStreamsHandler.writeUTF(messageHandler.getHelloMessage());
    }

    /**
     * Leave the client in a "only-receiving" mode until the
     * hello response is received
     * @throws IOException if the server is unreachable
     */
    private void waitForServerToBeReady() throws IOException {
        statusLock.lock();
        while (!ready){
            statusLock.unlock();
            String input=socketStreamsHandler.aSyncReadUTF();
            computeRemoteInput(input);
            statusLock.lock();
        }
        statusLock.unlock();
    }


    /**
     * This method handles getInstance for server-side SocketConnection
     */
    private void handleServerSideSetup() {
        waitForServerNotification();
        try {
            String expectedHello=socketStreamsHandler.aSyncReadUTF();
            computeRemoteInput(expectedHello);
            socketStreamsHandler.writeUTF(messageHandler.getServerIsReadyMessage());
        } catch (IOException e) {
            shutdown();
        }
    }

    /**
     * This method computes each input passed.
     * @param remoteInput to be computed
     * @exception UndefinedInputTypeException thrown if an undefined message is received
     * @see MessageHandler#computeInput(String)
     */
    private void computeRemoteInput(String remoteInput){
        try{
            messageHandler.computeInput(remoteInput);
            this.resetTTL();
        } catch (ConnectionEventException e){
            Optional.ofNullable(eventAdministrator.get(e.getClass()))
                    .ifPresent(eventHandler-> eventHandler.computeEvent(this, e));
        }
    }


    /**
     * This method is used to wait that server notify this connection
     * to set to active
     */
    private void waitForServerNotification() {
        statusLock.lock();
        try {
            while (!active) statusCondition.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        statusLock.unlock();
    }

    /**
     * This method is the core of SocketConnection.
     * It sends a ping message to the remote host and than he read messages on the
     * stream received from the remote host.
     */
    private void handleSession() {
        statusLock.lock();
        while (!shutdown){
            statusLock.unlock();
            ping();
            computeInputs();
            delay();
            statusLock.lock();
        }
        statusLock.unlock();
    }

    /**
     * This method is used to close the socket.
     * @exception ShutDownException if the socket is already closed.
     */
    private void tearDownConnection() {
        try {
            timer.stop();
            socket.close();
        } catch (IOException e) {
            throw new ShutDownException();
        }
    }

    /**
     * This method is used to put thread in a sleep status for
     * a defined amount of time: {@link #delayInMs}
     */
    private void delay() {
        try {
            Thread.sleep(delayInMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * This method sends an encoded empty message to let the remote host to know
     * that the connection is still active even if messages aren't exchanged during the session.
     */
    private void ping() {
        try {
            socketStreamsHandler.writeUTF(messageHandler.getPingMessage());
        } catch (IOException e) {
            shutdown();
        }
    }

    /**
     * This method is used to read messages sent from the remote host.
     * It read all messages in the buffer or a defined number {@link #maxReads} of messages
     * depending on the fact that "max reads" are enabled or not {@link #enabledMaxReads}
     */
    private void computeInputs() {
        int currentRead=0;
        try {
            while (socketStreamsHandler.availableData()>0&&(enabledMaxReads &&currentRead< maxReads)){
                currentRead++;
                String string= socketStreamsHandler.aSyncReadUTF();
                computeRemoteInput(string);
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    /**
     * This method shuts down the connection.
     * On server-side, if the server handling this connection isn't still notified,
     * it notifies the server.
     */
    @SuppressWarnings("WeakerAccess")
    public void shutdown() {
        try{
            shutdownConnection();
        }catch (NotifyServerException e){
            /*If we are client side isPresent will be false*/
            Optional<ServerSocketConnection> serverToNotify=Optional.ofNullable(handlingServer);
            serverToNotify.ifPresent(server->server.notifyDisconnection(this));
        }
    }

    /**
     * This method does all the stuff needed to shutdown the connection.
     */
    private void shutdownConnection() {
        statusLock.lock();
        boolean alreadyDown=shutdown;
        shutdown=true;
        synchronizedBuffer.closeBuffer();
        this.interrupt();
        statusLock.unlock();
        if(!alreadyDown) throw new NotifyServerException();
    }

    /**
     * This method can be used to send a string to the remote host
     * @param string to be sent
     * @throws UnreachableHostException if the host is unreachable
     */
    @SuppressWarnings("WeakerAccess")
    public void writeData(String string) throws UnreachableHostException {
        waitToBeReady();
        checkIfShutDown();
        String toSend=messageHandler.computeOutput(string);
        sendData(toSend);
    }

    /**
     * This method is used to send data to the remote host after output-computation
     * typical of this class
     */
    private void sendData(String toSend) throws UnreachableHostException {
        try {
            socketStreamsHandler.writeUTF(toSend);
        } catch (IOException e) {
            throw new UnreachableHostException();
        }
    }

    /**
     * This method can be used to send an integer to the remote host
     * @param number to be sent
     * @throws UnreachableHostException if the host is unreachable
     */
    @SuppressWarnings("WeakerAccess")
    public void writeInt(int number) throws UnreachableHostException {
        waitToBeReady();
        checkIfShutDown();
        String toSend= messageHandler.computeOutput(number);
        sendData(toSend);
    }

    /**
     * @return a the first element of the synchronizedBuffer
     * @throws UnreachableHostException when connection is down
     */
    @SuppressWarnings("WeakerAccess")
    public String readData() throws UnreachableHostException{
        waitToBeReady();
        try {
            return synchronizedBuffer.popString();
        } catch (ShutDownException e){
            throw new UnreachableHostException();
        }

    }

    /**
     * @return an integer from the synchronizedBuffer
     * @throws UnreachableHostException when connection is down
     * @throws BadMessagesSequenceException when the first element of the synchronizedBuffer isn't an integer
     */
    @SuppressWarnings("WeakerAccess")
    public int readInt() throws UnreachableHostException, BadMessagesSequenceException {
        waitToBeReady();
        try {
            return synchronizedBuffer.popInt();
        } catch (ShutDownException e){
            throw new UnreachableHostException();
        }
    }

    /**
     * This method is used to add data-type message to the {@link #synchronizedBuffer}
     * @param data to be added
     */
    private void addToBuffer(String data) {
        synchronizedBuffer.put(data);
    }

    /**
     * Reset the timer checking for timeouts due to disconnections.
     */
    private void resetTTL() {
        timer.resetTTL();
    }

    /**
     * @return true if data are available in the buffer, false in the other case.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isDataAvailable(){
        return synchronizedBuffer.size()>0;
    }


    /**
     * @return the ping.
     * @throws UnreachableHostException if the host is unreachable
     */
    @SuppressWarnings("WeakerAccess")
    public long getPing() throws UnreachableHostException {
        waitToBeReady();
        checkIfShutDown();
        InetAddress ip= socket.getInetAddress();
        long currentTime=System.currentTimeMillis();
        try {
            if(ip.isReachable(2000)) return System.currentTimeMillis()-currentTime;
            else throw new UnreachableHostException();
        } catch (IOException e) {
            throw new UnreachableHostException();
        }
    }

    /**
     * @throws UnreachableHostException if the connection is closed.
     */
    private void checkIfShutDown() throws UnreachableHostException {
        statusLock.lock();
        boolean condition=shutdown;
        statusLock.unlock();
        if(condition) throw new UnreachableHostException();
    }

    /**
     * @return true if the connection is open, false if it's closed
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isConnected() {
        statusLock.lock();
        boolean toReturn= !shutdown;
        statusLock.unlock();
        return toReturn;
    }

    /**
     * @return true if the connection is handled by a server, false in the other case.
     */
    private boolean isServerSide() {
        return serverSide;
    }

    /**
     * @return true if the getInstance phase is ended, false in the other case.
     */
    boolean isReady() {
        statusLock.lock();
        boolean toReturn= ready;
        statusLock.unlock();
        return toReturn;
    }

    /**
     * This method is used to wait the end of the getInstance phase
     */
    private void waitToBeReady(){
        statusLock.lock();
        while (!ready){
            try {
                statusCondition.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        statusLock.unlock();
    }

    /**
     * This method is used to set {@link #ready}== true:
     * this means that the getInstance phase is ended.
     */
    private void setToReady(){
        statusLock.lock();
        ready=true;
        statusCondition.signalAll();
        statusLock.unlock();
    }

    /**
     * this method set the connection to active.
     * A active connection can exchange messages with a remote host.
     */
    void setToActive(){
        statusLock.lock();
        active =true;
        statusCondition.signal();
        statusLock.unlock();
    }

    /**
     * This method is used to get the timeToLive of the connections
     * @return an integer representing the timeToLive in fixme time unit
     */
    public int getTimeToLive() {
        return timeToLive;
    }

    private static class EventAdministrator {
        /**
         * This method handles a server is ready event
         * @param connection is the connection which registered the event
         * @param e the event registered
         */
        private static void handleServerIsReadyMessage(SocketConnection connection, ConnectionEventException e){
            if(connection.isServerSide()|| connection.isReady()) throw new BadSetupException();
            connection.setToReady();
        }

        /**
         * This method handles a data received event
         * @param connection is the connection which registered the event
         * @param e the event registered
         */
        private static void handleDataReception(SocketConnection connection, ConnectionEventException e) {
            connection.addToBuffer(e.getEventData());
        }

        /**
         * This method handles a hello event
         * @param connection is the connection which registered the event
         * @param e the event registered
         * @exception BadSetupException is thrown if:
         *          1-> is received by server
         */
        private static void handleHelloMessage(SocketConnection connection, ConnectionEventException e) {
            if(!connection.isServerSide()|| connection.isReady()) throw new BadSetupException();
            connection.setToReady();
        }
    }
}

@FunctionalInterface
interface eventComputer<T,R>{
    void computeEvent(T t, R r);
}