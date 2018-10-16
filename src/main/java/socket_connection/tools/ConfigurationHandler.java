package socket_connection.tools;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class ConfigurationHandler {

    private static ConfigurationHandler instance;
    private SocketConnectionConfigurations socketConnectionConfigurations;
    private MessageHandlerConfigurations messageHandlerConfigurations;
    private boolean runningDefaultMHConfigurations;
    private boolean runningDefaultSCConfigurations;
    private static final String ABSOLUTE_PATH = ConfigurationHandler.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    private static final String MESSAGE_HANDLER_CONFIGURATIONS_PATH = ABSOLUTE_PATH +"MessageHandlerConfigurations.json";
    private static final String SOCKET_CONNECTION_CONFIGURATIONS_PATH = ABSOLUTE_PATH +"SocketConnectionConfigurations.json";

    private ConfigurationHandler(){
        Gson gson=new Gson();
        loadMessageHandlerConfigurations(gson);
        loadSocketConnectionConfigurations(gson);
    }

    public static ConfigurationHandler getInstance(){
        if(instance==null) instance=new ConfigurationHandler();
        return instance;
    }

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

    public boolean isRunningDefaultMHConfigurations(){
        return runningDefaultMHConfigurations;
    }

    public boolean isRunningDefaultSCConfigurations() {
        return runningDefaultSCConfigurations;
    }

    public MessageHandlerConfigurations getMessageHandlerConfigurations() {
        return messageHandlerConfigurations;
    }

    public SocketConnectionConfigurations getSocketConnectionConfigurations() {
        return socketConnectionConfigurations;
    }
}
