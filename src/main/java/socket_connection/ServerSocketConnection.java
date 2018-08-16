package socket_connection;
import socket_connection.socket_exceptions.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerSocketConnection extends Thread {

    private int port;
    private Class<? extends SocketUserAgentInterface> agentClassType;
    private HashMap<SocketConnection,SocketUserAgentInterface> availableConnections;
    private Lock serverStatusLock;
    private Lock availableConnectionsLock;
    private ExecutorService threadsHandler;
    private Condition serverStatusCondition;
    private boolean shutdown;
    private boolean isClosed;
    private ServerSocket serverSocket;
    private static final long SLEEP_IN_MS = 20;
    private static long awaitExecutorInMS= 5000;
    public enum Status {
        /**
         * if the server is accepting incoming connections
         */
        RUNNING,
        /**
         * if the server need to be launched
         */
        WAITING_LAUNCH,
        /**
         * if the server isn't accepting incoming connections
         */
        CLOSED,
        /**
         * If the server is shut down
         */
        SHUT_DOWN
    }

    /**
     * Constructor of ServerSocketConnection. Automatically
     * start the thread checking for incoming connections
     * @param port where to open the server
     * @param userAgentClass the class implementing SocketUSerAgentInterface.
     *                       This is used to create and run user agents instances.
     * @throws IOException if the port is already in use
     * @throws NoDefaultConstructorException if the implementing class doesn't have a default constructor
     * @throws IllegalAccessException if the constructor of the class passed isn't accessible
     * @throws InvocationTargetException if the underlying constructor throws an exception.
     * @throws InstantiationException if the class that declares the underlying constructor represents an abstract class
     */
    @SuppressWarnings("WeakerAccess")
    public ServerSocketConnection(int port, Class<? extends SocketUserAgentInterface> userAgentClass) throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException, NoDefaultConstructorException {
        this(port,userAgentClass,false);
    }

    /**
     * Constructor of ServerSocketConnection
     * @param port where to open the server
     * @param userAgentClass the class implementing SocketUSerAgentInterface.
     *                       This is used to create and run user agents instances.
     * @param manualStart if set == true you have to start manually (using the start setup)
     *                    the thread checking for connections
     * @throws IOException if the port is already in use
     * @throws NoDefaultConstructorException if the implementing class doesn't have a default constructor
     * @throws IllegalAccessException if the constructor of the class passed isn't accessible
     * @throws InvocationTargetException if the underlying constructor throws an exception
     * @throws InstantiationException if the class that declares the underlying constructor represents an abstract class
     */
    @SuppressWarnings("WeakerAccess")
    public ServerSocketConnection(int port, Class<? extends SocketUserAgentInterface> userAgentClass, boolean manualStart) throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException, NoDefaultConstructorException {
        this();
        agentClassType =userAgentClass;
        this.port=port;
        serverSocket= new ServerSocket(port);
        try {
            agentClassType.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new NoDefaultConstructorException();
        }
        if(!manualStart) this.start();
    }
    /**
     * Private constructor to initialize principal fields
     */
    private ServerSocketConnection(){
        shutdown=false;
        availableConnections= new HashMap<>();
        availableConnectionsLock=new ReentrantLock();
        serverStatusLock =new ReentrantLock();
        isClosed=false;
        serverStatusCondition =serverStatusLock.newCondition();
        threadsHandler= Executors.newCachedThreadPool();
    }

    /**
     * This thread is waiting for incoming connections
     */
    @Override
    public void run(){
        serverStatusLock.lock();
        while (!shutdown){
            serverStatusLock.unlock();
            delay();
            handleIncomingConnections();
            serverStatusLock.lock();
        }
        serverStatusLock.unlock();
        tearDownProtocol();
    }

    /**
     * This setup contains all operation to do before server is shut down.
     */
    private void tearDownProtocol() {
        availableConnectionsLock.lock();
        availableConnections.values().forEach(SocketUserAgentInterface::shutdown);
        availableConnectionsLock.unlock();
        try {
            threadsHandler.awaitTermination(awaitExecutorInMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        availableConnectionsLock.lock();
        availableConnectionsLock.unlock();
    }


    /**
     * this setup will set a delay between every
     * iteration of the methods into the while block of
     * the run setup
     */
    private void delay() {
        try {
            Thread.sleep(SLEEP_IN_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * This setup accept incoming connection, create and setup a new instance of
     * a generic user agent class implementing SocketUserAgentInterface.
     */
    private void handleIncomingConnections() {
        SocketUserAgentInterface runningAgent;
        try {
            runningAgent = agentClassType.getConstructor().newInstance();
            setupRunningAgent(runningAgent);
        } catch (NoSuchMethodException |InstantiationException | IllegalAccessException |InvocationTargetException e) {
            throw new BadServerSetupException();
        }
    }

    /**
     * this setup setup properly a running agent when a connection request
     * is received
     * @param runningAgent to setup
     */
    private void setupRunningAgent(SocketUserAgentInterface runningAgent) {
        try {
            Socket client=serverSocket.accept();
            setup(client, runningAgent);
        } catch (IOException e) {
            handleThrown();
        } catch (FailedToConnectException e) {
            runningAgent.notifySetUpError();
            new Thread(runningAgent).start();
        }
    }

    /**
     * Given a connection bind it to the relative agent.
     * @param client is the connection just accepted
     * @param runningAgent to setup
     * @throws FailedToConnectException if can't connect anymore to the connection just accepted
     */
    private void setup(Socket client, SocketUserAgentInterface runningAgent) throws FailedToConnectException {
        SocketConnection socket=new SocketConnection(client,this);
        availableConnectionsLock.lock();
        availableConnections.put(socket,runningAgent);
        availableConnectionsLock.unlock();
        runningAgent.setConnection(socket);
        new Thread(runningAgent).start();
    }

    /**
     * Put server in a wait status. This is launched when serverSocket is temporary
     * closed.
     * @exception BadServerSetupException is launched when serverSocket launch a IOException
     * without being closed with close or shutdown method.
     */
    private void handleThrown() {
        serverStatusLock.lock();
        if(!shutdown&&isClosed){
            closeServer();
        }else if(!shutdown){
            serverStatusLock.unlock();
            throw new BadServerSetupException();
        }
        serverStatusLock.unlock();
    }

    /**
     * keep server closed until a open or a shutdown command is received.
     */
    private void closeServer() {
        while (isClosed&&!shutdown){
            try {
                serverStatusCondition.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * This method is used to block incoming connections.
     * After using this method no one can connect to the server
     * people connected to server remain connected.
     * @throws ServerAlreadyClosedException if server is already closed
     * @throws ServerShutdownException if server is shutdown
     * @exception BadServerSetupException is launched just if there's a severe error due
     * to a bad programming phase.
     */
    @SuppressWarnings("WeakerAccess")
    public void close() throws ServerAlreadyClosedException, ServerShutdownException {
        checkIfShutDown();
        serverStatusLock.lock();
        if(isClosed){
            serverStatusLock.unlock();
            throw new ServerAlreadyClosedException();
        }else {
            closeServerSocket();
            isClosed=true;
            serverStatusLock.unlock();
        }
    }

    /**
     * This method is used to re-open a closed server.
     * After using this method all incoming connection
     * are accepted again.
     * @throws ServerAlreadyOpenedException if server is already opened
     * @throws ServerShutdownException if server is shutdown
     * @exception BadServerSetupException is launched just if there's a severe error due
     * to a bad programming phase.
     * NOTE: if an application reserve the passed port the exception is launched.
     */
    @SuppressWarnings("WeakerAccess")
    public void open() throws ServerAlreadyOpenedException, ServerShutdownException {
        checkIfShutDown();
        serverStatusLock.lock();
        if (!isClosed) {
            serverStatusLock.unlock();
            throw new ServerAlreadyOpenedException();
        } else {
            openServerSocket();
            serverStatusCondition.signal();
            isClosed = false;
            serverStatusLock.unlock();
        }
    }

    /**
     * This setup is used to shut down server.
     * After using this setup no one can connect anymore
     * and all open connections will be closed.
     * @throws ServerShutdownException if server is already closed
     * @exception BadServerSetupException is launched just if there's a severe error due
     * to a bad programming phase.
     */
    @SuppressWarnings("WeakerAccess")
    public void shutdown() throws ServerShutdownException {
        serverStatusLock.lock();
        if(!shutdown) {
            this.interrupt();
            closeServerSocket();
            shutdown=true;
            serverStatusLock.unlock();
        } else{
            serverStatusLock.lock();
            throw new ServerShutdownException();
        }

    }

    /**
     * close server socket
     * @exception BadServerSetupException is launched just if there's a severe error due
     * to a bad programming phase.
     */
    private void closeServerSocket(){
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new BadServerSetupException();
        }
    }

    /**
     * close server socket
     * @exception BadServerSetupException is launched just if there's a severe error due
     * to a bad programming phase.
     */
    private void openServerSocket() {
        try{
            serverSocket= new ServerSocket(port);
        } catch (IOException e) {
            throw new BadServerSetupException();
        }
    }

    /**
     * Check if server is shut down
     * @throws ServerShutdownException if server is shut down.
     */
    private void checkIfShutDown() throws ServerShutdownException {
        serverStatusLock.lock();
        Optional<ServerShutdownException> shutdownNotification=
                Optional.ofNullable(shutdown ? new ServerShutdownException() : null);
        if (shutdownNotification.isPresent()) throw shutdownNotification.get();
    }

    /**
     * This method is used by server side socketConnections to notify server they are shut down
     * @param connection is the connection who notified the server that it will be closed soon
     */
    void notifyDisconnection(SocketConnection connection) {
        threadsHandler.execute(()-> removeConnection(connection));
    }

    /**
     * Remove the connection passed from connections hash map
     * @param connection to be removed
     * @exception BadServerSetupException if the connection isn't in the hash map
     */
    private void removeConnection(SocketConnection connection) {
        availableConnectionsLock.lock();
        if(!Optional.ofNullable(availableConnections.remove(connection)).isPresent()) throw new BadServerSetupException();
        availableConnectionsLock.unlock();
    }

    /**
     * This method is used to see if server is running
     * @return true if the server is running, false if the server is shut down or still not started
     */
    @SuppressWarnings("WeakerAccess")
    public Status getStatus() {
        if(this.getState().equals(Thread.State.NEW)) return Status.WAITING_LAUNCH;
        try {
            checkIfShutDown();
            serverStatusLock.lock();
            boolean closed=isClosed;
            serverStatusLock.unlock();
            return closed ? Status.CLOSED : Status.RUNNING;
        } catch (ServerShutdownException e) {
            return Status.SHUT_DOWN;
        }
    }
}
