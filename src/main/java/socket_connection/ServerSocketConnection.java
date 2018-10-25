package socket_connection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import socket_connection.socket_exceptions.exceptions.*;
import socket_connection.socket_exceptions.runtime_exceptions.BadSetupException;
import socket_connection.configurations.ConfigurationHandler;
import socket_connection.tools.ConnectionsHandler;
import socket_connection.configurations.ServerSocketConnectionConfigurations;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class ServerSocketConnection extends Thread {

    private int port;
    private Class<? extends SocketUserAgentInterface> agentClassType;
    private ConnectionsHandler connectionsHandler;
    private ReentrantLock serverStatusLock;
    private ExecutorService threadsHandler;
    private Condition serverStatusCondition;
    private Logger logger;
    private Status currentStatus;
    private ServerSocket serverSocket;
    private long sleepInMs;
    private long awaitExecutorInMs;
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
         * but it's still handling active connections
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
     * @param manualStart if set == true you have to start manually (using the start getInstance)
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
        ServerSocketConnectionConfigurations config=ConfigurationHandler.getInstance().getServerSocketConnectionConfigurations();
        this.sleepInMs=config.getSleepInMs();
        this.awaitExecutorInMs=config.getAwaitExecutorInMs();
        this.connectionsHandler=new ConnectionsHandler();
        this.serverStatusLock =new ReentrantLock();
        this.serverStatusCondition =serverStatusLock.newCondition();
        this.threadsHandler= Executors.newCachedThreadPool();
        this.logger=Logger.getLogger(ServerSocketConnection.class.toString()+"%u");
        this.currentStatus=Status.WAITING_LAUNCH;
    }

    /**
     * This thread is waiting for incoming connections
     */
    @Override
    public void run(){
        serverStatusLock.lock();
        while (currentStatus!=Status.SHUT_DOWN){
            serverStatusLock.unlock();
            delay();
            handleIncomingConnections();
            serverStatusLock.lock();
        }
        serverStatusLock.unlock();
        tearDownProtocol();
    }

    /**
     * This getInstance contains all operation to do before server is shut down.
     */
    private void tearDownProtocol() {
        connectionsHandler.shutdownAllConnections();
        try {
            threadsHandler.awaitTermination(awaitExecutorInMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    /**
     * this getInstance will set a delay between every
     * iteration of the methods into the while block of
     * the run getInstance
     */
    private void delay() {
        try {
            Thread.sleep(sleepInMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * This getInstance accept incoming connection, create and getInstance a new instance of
     * a generic user agent class implementing SocketUserAgentInterface.
     */
    private void handleIncomingConnections() {
        SocketUserAgentInterface runningAgent;
        try {
            runningAgent = agentClassType.getConstructor().newInstance();
            setupRunningAgent(runningAgent);
        } catch (NoSuchMethodException |InstantiationException | IllegalAccessException |InvocationTargetException e) {
            throw new BadSetupException();
        }
    }

    /**
     * this getInstance getInstance properly a running agent when a connection request
     * is received
     * @param runningAgent to getInstance
     */
    private void setupRunningAgent(SocketUserAgentInterface runningAgent) {
        try {
            logger.finest("waiting for connection request");
            Socket client=serverSocket.accept();
            logger.finest("Connection request received");
            setup(client, runningAgent);
            logger.finest("Client connected");
        } catch (IOException e) {
            handleThrown();
        } catch (FailedToConnectException e) {
            logger.fine("Client disconnected before ending getInstance phase");
        }
    }

    /**
     * Given a connection bind it to the relative agent.
     * @param client is the connection just accepted
     * @param runningAgent to getInstance
     * @throws FailedToConnectException if can't connect anymore to the connection just accepted
     */
    private void setup(@NotNull Socket client,@NotNull SocketUserAgentInterface runningAgent) throws FailedToConnectException {
        SocketConnection connection=new SocketConnection(client,this);
        connectionsHandler.addConnection(connection, runningAgent);
        connection.setToActive();
        runningAgent.setConnection(connection);
        new Thread(runningAgent).start();
    }

    /**
     * Put server in a wait status. This is launched when serverSocket is temporary
     * closed.
     * @exception BadSetupException is launched when serverSocket launch a IOException
     * without being closed with close or shutdown method.
     */
    private void handleThrown() {
        serverStatusLock.lock();
        if(currentStatus==Status.CLOSED){
            closeServer();
        }else if(currentStatus!=Status.SHUT_DOWN){
            serverStatusLock.unlock();
            throw new BadSetupException();
        }
        serverStatusLock.unlock();
    }

    /**
     * keep server closed until a open or a shutdown command is received.
     * @exception BadSetupException is launched if this method is used
     * without acquire a lock on serverStatusLock
     */
    private void closeServer() {
        if(!serverStatusLock.isHeldByCurrentThread()) throw new BadSetupException();
        while (currentStatus==Status.CLOSED){
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
     * @exception BadSetupException is launched just if there's a severe error due
     * to a bad programming phase.
     */
    @SuppressWarnings("WeakerAccess")
    public void close() throws ServerAlreadyClosedException, ServerShutdownException {
        checkIfShutDown();
        serverStatusLock.lock();
        if(currentStatus==Status.CLOSED){
            serverStatusLock.unlock();
            throw new ServerAlreadyClosedException();
        }else {
            closeServerSocket();
            currentStatus=Status.CLOSED;
            serverStatusLock.unlock();
        }
    }

    /**
     * This method is used to re-open a closed server.
     * After using this method all incoming connection
     * are accepted again.
     * @throws ServerAlreadyOpenedException if server is already opened
     * @throws ServerShutdownException if server is shutdown
     * @exception BadSetupException is launched just if there's a severe error due
     * to a bad programming phase.
     * NOTE: if an application reserve the passed port the exception is launched.
     */
    @SuppressWarnings("WeakerAccess")
    public void open() throws ServerAlreadyOpenedException, ServerShutdownException {
        checkIfShutDown();
        serverStatusLock.lock();
        if (currentStatus==Status.RUNNING) {
            serverStatusLock.unlock();
            throw new ServerAlreadyOpenedException();
        } else {
            openServerSocket();
            currentStatus=Status.RUNNING;
            serverStatusCondition.signal();
            serverStatusLock.unlock();
        }
    }

    /**
     * This getInstance is used to shut down server.
     * After using this getInstance no one can connect anymore
     * and all open connections will be closed.
     * @throws ServerShutdownException if server is already closed
     * @exception BadSetupException is launched just if there's a severe error due
     * to a bad programming phase.
     */
    @SuppressWarnings("WeakerAccess")
    public void shutdown() throws ServerShutdownException {
        serverStatusLock.lock();
        if(currentStatus!=Status.SHUT_DOWN) {
            this.interrupt();
            closeServerSocket();
            currentStatus=Status.SHUT_DOWN;
            serverStatusLock.unlock();
        } else{
            serverStatusLock.unlock();
            throw new ServerShutdownException();
        }

    }

    /**
     * close server socket
     * @exception BadSetupException is launched just if there's a severe error due
     * to a bad programming phase.
     */
    private void closeServerSocket(){
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new BadSetupException();
        }
    }

    /**
     * close server socket
     * @exception BadSetupException is launched just if there's a severe error due
     * to a bad programming phase.
     */
    private void openServerSocket() {
        try{
            serverSocket= new ServerSocket(port);
        } catch (IOException e) {
            throw new BadSetupException();
        }
    }

    /**
     * Check if server is shut down
     * @throws ServerShutdownException if server is shut down.
     */
    private void checkIfShutDown() throws ServerShutdownException {
        serverStatusLock.lock();
        Optional<ServerShutdownException> shutdownNotification=
                Optional.ofNullable(currentStatus==Status.SHUT_DOWN ? new ServerShutdownException() : null);
        serverStatusLock.unlock();
        if (shutdownNotification.isPresent()) throw shutdownNotification.get();
    }

    /**
     * This method is used by server side socketConnections to notify server they are shut down
     * @param connection is the connection who notified the server that it will be closed soon
     */
    void notifyDisconnection(SocketConnection connection) {
        threadsHandler.execute(()-> connectionsHandler.removeConnection(connection));
    }

    /**
     * This method is used to see if server is running
     * @return true if the server is running, false if the server is shut down or still not started
     */
    @SuppressWarnings("WeakerAccess") @Contract(pure = true)
    public Status getStatus() {
        serverStatusLock.lock();
        Status toReturn=currentStatus;
        serverStatusLock.unlock();
        return toReturn;
    }

    /**
     * This method is used to see if server is running
     * @return true if the server is closed, false if it's not
     */
    private boolean isClosed(){
        serverStatusLock.lock();
        boolean closed=(currentStatus==Status.CLOSED);
        serverStatusLock.unlock();
        return closed;
    }

    /**
     * @return the number of active connection
     */
    @SuppressWarnings("WeakerAccess")
    public int activeConnections(){
        return connectionsHandler.activeConnections();
    }


    /**
     * Override of Thread's start.
     * The status of the server is set to {@link Status#RUNNING}
     */
    @Override
    public void start(){
        serverStatusLock.lock();
        super.start();
        this.currentStatus=Status.RUNNING;
        serverStatusLock.unlock();
    }
}
