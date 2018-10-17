package socket_connection.tools;

import org.jetbrains.annotations.Contract;

public class ServerSocketConnectionConfigurations {
    private long sleepInMs;
    private long awaitExecutorInMs;

    ServerSocketConnectionConfigurations(){
        this.sleepInMs=20;
        this.awaitExecutorInMs=5000;
    }

    @Contract(pure = true)
    public long getSleepInMs() {
        return sleepInMs;
    }


    @Contract(pure = true)
    public long getAwaitExecutorInMs() {
        return awaitExecutorInMs;
    }
}
