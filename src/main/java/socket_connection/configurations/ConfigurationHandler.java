package socket_connection.configurations;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class ConfigurationHandler {

    private static ConfigurationHandler instance;
    private SocketConnectionConfigurations socketConnectionConfigurations;
    private MessageHandlerConfigurations messageHandlerConfigurations;
    private ServerSocketConnectionConfigurations serverSocketConnectionConfigurations;
    private boolean runningDefaultMHConfigurations; //MH= MessageHandler
    private boolean runningDefaultSCConfigurations; //SC= SocketConnection
    private boolean runningDefaultSSCConfigurations; //SSC= ServerSocketConnection
    private static final String ABSOLUTE_PATH = ConfigurationHandler.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    private static final String MESSAGE_HANDLER_CONFIGURATIONS_PATH = ABSOLUTE_PATH +"MessageHandlerConfigurations.json";
    private static final String SOCKET_CONNECTION_CONFIGURATIONS_PATH = ABSOLUTE_PATH +"SocketConnectionConfigurations.json";
    private static final String SERVER_SOCKET_CONNECTION_CONFIGURATIONS_PATH = ABSOLUTE_PATH +"ServerSocketConnectionConfigurations.json";


    /**
     * This class is a singleton, so the constructor is private
     */
    private ConfigurationHandler(){
        Gson gson=new Gson();
        loadMessageHandlerConfigurations(gson);
        loadSocketConnectionConfigurations(gson);
        loadServerSocketConnectionConfigurations(gson);
    }

    /**
     * @return the instance of this class. If there isn't an existing
     * one it will create a new instance.
     */
    public static ConfigurationHandler getInstance(){
        if(instance==null) instance=new ConfigurationHandler();
        return instance;
    }

    /**
     * This method is used to get configurations of {@link socket_connection.MessageHandler}
     * from file. If a file doesn't exist it will create a new instance of {@link MessageHandlerConfigurations}
     * with default configurations: in this case {@link #runningDefaultMHConfigurations} is set as true,
     * otherwise as false.
     * @param gson used to parse the json file
     */
    private void loadMessageHandlerConfigurations(Gson gson) {
        File file= new File(MESSAGE_HANDLER_CONFIGURATIONS_PATH);
        try {
            messageHandlerConfigurations= gson.fromJson(new FileReader(file), MessageHandlerConfigurations.class);
            runningDefaultMHConfigurations =false;
        } catch (FileNotFoundException e) {
            messageHandlerConfigurations=new MessageHandlerConfigurations();
            runningDefaultMHConfigurations =true;
        }
    }

    /**
     * This method is used to get configurations of {@link socket_connection.SocketConnection}
     * from file. If a file doesn't exist it will create a new instance of {@link SocketConnectionConfigurations}
     * with default configurations: in this case {@link #runningDefaultSCConfigurations} is set as true,
     * otherwise as false.
     * @param gson used to parse the json file
     */
    private void loadSocketConnectionConfigurations(Gson gson) {
        File file= new File(SOCKET_CONNECTION_CONFIGURATIONS_PATH);
        try {
            socketConnectionConfigurations= gson.fromJson(new FileReader(file), SocketConnectionConfigurations.class);
            runningDefaultSCConfigurations= false;
        } catch (FileNotFoundException e) {
            socketConnectionConfigurations= new SocketConnectionConfigurations();
            runningDefaultSCConfigurations= true;
        }
    }

    /**
     * This method is used to get configurations of {@link socket_connection.ServerSocketConnection}
     * from file. If a file doesn't exist it will create a new instance of {@link ServerSocketConnectionConfigurations}
     * with default configurations: in this case {@link #runningDefaultSSCConfigurations} is set as true,
     * otherwise as false.
     * @param gson used to parse the json file
     */
    private void loadServerSocketConnectionConfigurations(Gson gson) {
        File file= new File(SERVER_SOCKET_CONNECTION_CONFIGURATIONS_PATH);
        try {
            serverSocketConnectionConfigurations = gson.fromJson(new FileReader(file), ServerSocketConnectionConfigurations.class);
            runningDefaultSSCConfigurations= false;
        } catch (FileNotFoundException e) {
            serverSocketConnectionConfigurations = new ServerSocketConnectionConfigurations();
            runningDefaultSSCConfigurations= true;
        }
    }

    /**
     * @return true if is running default {@link MessageHandlerConfigurations}
     */
    public boolean isRunningDefaultMHConfigurations(){
        return runningDefaultMHConfigurations;
    }

    /**
     * @return true if is running default {@link SocketConnectionConfigurations}
     */
    public boolean isRunningDefaultSCConfigurations() {
        return runningDefaultSCConfigurations;
    }

    /**
     * @return true if is running default {@link ServerSocketConnectionConfigurations}
     */
    public boolean isRunningDefaultSSCConfigurations() {
        return runningDefaultSSCConfigurations;
    }

    /**
     * @return the current configurations used by {@link socket_connection.MessageHandler}
     */
    public MessageHandlerConfigurations getMessageHandlerConfigurations() {
        return messageHandlerConfigurations;
    }

    /**
     * @return the current configurations of {@link socket_connection.SocketConnection}
     */
    public SocketConnectionConfigurations getSocketConnectionConfigurations() {
        return socketConnectionConfigurations;
    }

    /**
     * @return the current configurations of {@link socket_connection.ServerSocketConnection}
     */
    public ServerSocketConnectionConfigurations getServerSocketConnectionConfigurations() {
        return serverSocketConnectionConfigurations;
    }
}
