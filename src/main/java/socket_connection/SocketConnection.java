package socket_connection;

import socket_connection.socket_exceptions.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class SocketConnection extends Thread{

    private Socket socket;
    private ServerSocketConnection handlingServer;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private SynchronizedDataBuffer synchronizedBuffer;
    private ConnectionTimer timer;
    private boolean shutdown;
    private boolean ready;
    private boolean serverSide;
    private Lock statusLock;
    private Lock outputStreamLock;
    private Condition statusCondition;

    private static final long DELAY_IN_MS = 200;

    /**
     * Package-private constructor: this is used from others constructors
     * to initialize some fields.
     */
    SocketConnection(){
        this.synchronizedBuffer =new SynchronizedDataBuffer();
        this.statusLock =new ReentrantLock();
        this.statusCondition=statusLock.newCondition();
        this.outputStreamLock=new ReentrantLock();
        this.timer=new ConnectionTimer(this);
        shutdown=false;
        ready=false;
    }

    /**
     * Package-private constructor for socket connection. This is called
     * from a ServerSocketConnection to setup an incoming connection
     * @param socket is the socket relative to the accepted connection
     * @param server is the server handling the connection
     * @throws FailedToConnectException if the connection is closed before
     * the ending of the setup phase
     */
    SocketConnection(Socket socket, ServerSocketConnection server) throws FailedToConnectException {
        this();
        this.serverSide=true;
        this.handlingServer=server;
        this.socket=socket;
        getStreams();
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
        try {
            socket=new Socket(ip,port);
        } catch (IOException e) {
            throw new FailedToConnectException();
        }
        getStreams();
        this.start();
    }

    /**
     * This method is used to get the streams relative to
     * the socket where the connection is opened
     * @throws FailedToConnectException if the host/server is unreachable
     */
    private void getStreams() throws FailedToConnectException {
        try {
            inputStream= new DataInputStream(socket.getInputStream());
            outputStream=new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new FailedToConnectException();
        }
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
     * This method is used to setup the connection
     */
    private void setupConnection() {
        if(serverSide)
            handleServerSideSetup();
        else
            handleClientSideSetup();
        timer.launch();
    }

    /**
     * This method is used to setup the connection
     * if the socket represent the client side-connection
     */
    private void handleClientSideSetup() {
        try{
            outputStream.writeUTF(MessageHandler.getHelloMessage());
            waitForServerToBeReady();
        }catch (IOException e){
            shutdown();
        }
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
            String input=inputStream.readUTF();
            checkRemoteMessage(input);
            statusLock.lock();
        }
        statusLock.unlock();
    }

    private void checkRemoteMessage(String input) {
        try {
            MessageHandler.computeInput(this,input);
        } catch (UndefinedInputTypeException e){
            throw new UndefinedInputTypeException();
        }
    }

    private void handleServerSideSetup() {
        try {
            String expectedHello=inputStream.readUTF();
            MessageHandler.computeInput(this, expectedHello);
            outputStream.writeUTF(MessageHandler.getServerIsReadyMessage());
        } catch (IOException e) {
            shutdown();
        }
    }

    private void handleSession() {
        statusLock.lock();
        while (!shutdown){
            statusLock.unlock();
            ping();
            computeInputs();
            waitForDelay();
            statusLock.lock();
        }
        statusLock.unlock();
    }

    private void tearDownConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            throw new ShutDownException();
        }
    }

    private void waitForDelay() {
        try {
            Thread.sleep(DELAY_IN_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void ping() {
        try {
            outputStreamLock.lock();
            outputStream.writeUTF(MessageHandler.getPingMessage());
        } catch (IOException e) {
            shutdown();
        } finally {
            outputStreamLock.unlock();
        }
    }

    private void computeInputs() {
        int currentRead=0;
        final int maxReads=50;
        try {
            while (inputStream.available()>0&&currentRead<maxReads){
                currentRead++;
                String string= inputStream.readUTF();
                MessageHandler.computeInput(this,string);
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void shutdown() {
        shutdownConnection();
        /*If we are client side isPresent will be false*/
        Optional<ServerSocketConnection> serverToNotify=Optional.ofNullable(handlingServer);
        serverToNotify.ifPresent(server->server.notifyDisconnection(this));
    }

    private void shutdownConnection() {
        statusLock.lock();
        shutdown=true;
        this.interrupt();
        statusLock.unlock();
    }

    @SuppressWarnings("WeakerAccess")
    public void writeUTF(String string) throws UnreachableHostException {
        waitToBeReady();
        checkIfShutDown();
        String toSend=MessageHandler.computeOutput(string);
        sendData(toSend);
    }

    private void sendData(String toSend) throws UnreachableHostException {
        try {
            outputStreamLock.lock();
            outputStream.writeUTF(toSend);
        } catch (IOException e) {
            throw new UnreachableHostException();
        } finally {
            outputStreamLock.unlock();
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void writeInt(int number) throws UnreachableHostException {
        waitToBeReady();
        checkIfShutDown();
        String toSend= MessageHandler.computeOutput(number);
        sendData(toSend);
    }

    /**
     * @return a the first element of the synchronizedBuffer
     * @throws UnreachableHostException when connection is down
     */
    @SuppressWarnings("WeakerAccess")
    public String readUTF() throws UnreachableHostException{
        waitToBeReady();
        checkIfShutDown();
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
        checkIfShutDown();
        try {
            return synchronizedBuffer.popInt();
        } catch (ShutDownException e){
            throw new UnreachableHostException();
        }
    }

    void addToBuffer(String data) {
        synchronizedBuffer.put(data);
    }

    void resetTTL() {
        timer.resetTTL();
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isDataAvailable(){
        return synchronizedBuffer.size()>0;
    }

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

    private void checkIfShutDown() throws UnreachableHostException {
        statusLock.lock();
        boolean condition=shutdown;
        statusLock.unlock();
        if(condition) throw new UnreachableHostException();
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isConnected() {
        statusLock.lock();
        boolean toReturn= !shutdown;
        statusLock.unlock();
        return toReturn;
    }

    boolean isServerSide() {
        return serverSide;
    }

    boolean isReady() {
        statusLock.lock();
        boolean toReturn= ready;
        statusLock.unlock();
        return toReturn;
    }
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

    void setToReady(){
        statusLock.lock();
        ready=true;
        statusCondition.signal();
        statusLock.unlock();
    }
}
