package socket_connection.tools;

import java.net.InetAddress;

public class Configuration {
    private int connectionPort;
    private String connectionIP;
    private int connectionTTLinSec;

    public Configuration(){
        this.connectionPort=11000;
        this.connectionIP=InetAddress.getLoopbackAddress().getHostAddress();
        this.connectionTTLinSec=2;
    }

    protected int getConnectionPort(){
        return connectionPort;
    }

    protected String getConnectionIP(){
        return connectionIP;
    }

    protected int getConnectionTTL(){
        return connectionTTLinSec;
    }
}
