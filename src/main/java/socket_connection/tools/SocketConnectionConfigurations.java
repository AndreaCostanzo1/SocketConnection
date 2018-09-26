package socket_connection.tools;

public class SocketConnectionConfigurations {

    private long delayInMs;
    private int maxReads;
    private boolean enabledMaxReads;
    private final int timeToLive;

    SocketConnectionConfigurations(){
        this.delayInMs=200;
        this.maxReads=50;
        this.enabledMaxReads=true;
        this.timeToLive =2;
    }

    public long getDelayInMs() {
        return delayInMs;
    }

    public int getMaxReads() {
        return maxReads;
    }

    public boolean isEnabledMaxReads() {
        return enabledMaxReads;
    }

    public int getTimeToLive() {
        return timeToLive;
    }
}
