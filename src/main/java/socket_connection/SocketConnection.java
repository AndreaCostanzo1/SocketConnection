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
    private SynchronizedDataBuffer buffer;
    private boolean shutdown;
    private boolean ready;
    private boolean serverSide;
    private Lock statusLock;
    private Lock outputStreamLock;
    private Condition statusCondition;

    private static final long DELAY_IN_MS = 200;

    SocketConnection(){
        this.buffer=new SynchronizedDataBuffer();
        this.statusLock =new ReentrantLock();
        this.statusCondition=statusLock.newCondition();
        this.outputStreamLock=new ReentrantLock();
        shutdown=false;
        ready=false;
    }

    SocketConnection(Socket socket, ServerSocketConnection server) throws FailedToConnectException {
        this();
        this.serverSide=true;
        this.handlingServer=server;
        this.socket=socket;
        getStreams();
        this.start();
    }

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

    private void getStreams() throws FailedToConnectException {
        try {
            inputStream= new DataInputStream(socket.getInputStream());
            outputStream=new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new FailedToConnectException();
        }
    }

    @Override
    public void run(){
        setupConnection();
        handleSession();
        tearDownConnection();
    }

    private void setupConnection() {
        if(serverSide)
            handleServerSideSetup();
        else
            handleClientSideSetup();

    }

    private void handleClientSideSetup() {
        try{
            outputStream.writeUTF(MessageHandler.getHelloMessage());
            waitForServerToBeReady();
        }catch (IOException e){
            shutdown();
        }
    }

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
            statusLock.unlock();
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
        try {
            while (inputStream.available()>0){
                String string= inputStream.readUTF();
                MessageHandler.computeInput(this,string);
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    public void shutdown() {
        statusLock.lock();
        shutdown=true;
        this.interrupt();
        statusLock.unlock();
        Optional<ServerSocketConnection> serverToNotify=Optional.ofNullable(handlingServer);
        serverToNotify.ifPresent(server->server.notifyDisconnection(this));
    }

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

    public void writeInt(int number) throws UnreachableHostException {
        waitToBeReady();
        checkIfShutDown();
        String toSend= MessageHandler.computeOutput(number);
        sendData(toSend);
    }

    /**
     * @return a the first element of the buffer
     * @throws UnreachableHostException when connection is down
     */
    public String readUTF() throws UnreachableHostException{
        waitToBeReady();
        checkIfShutDown();
        try {
            return buffer.popString();
        } catch (ShutDownException e){
            throw new UnreachableHostException();
        }

    }

    /**
     * @return an integer from the buffer
     * @throws UnreachableHostException when connection is down
     * @throws BadMessagesSequenceException when the first element of the buffer isn't an integer
     */
    public int readInt() throws UnreachableHostException, BadMessagesSequenceException {
        waitToBeReady();
        checkIfShutDown();
        try {
            return buffer.popInt();
        } catch (ShutDownException e){
            throw new UnreachableHostException();
        }
    }

    void addToBuffer(String data) {
        buffer.put(data);
    }

    void resetTTL() {
        // TODO: 09/08/2018
    }

    public boolean isSomethingAvailable(){
        return buffer.size()>0;
    }

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
        if(shutdown) throw new UnreachableHostException();
        statusLock.unlock();
    }

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
