package socket_connection.tools;

import org.jetbrains.annotations.Contract;
import java.nio.charset.Charset;

public class MessageHandlerConfigurations {

    private String pingMessage;
    private String dataMessage;
    private int dataTagPosition;
    private String helloMessage;
    private String serverIsReadyMessage;
    private String charset;

    MessageHandlerConfigurations(){
        this.pingMessage ="";
        this.dataMessage ="#DATA#";
        this.helloMessage ="#HELLO#";
        this.serverIsReadyMessage = "#SERVER_READY#";
        this.charset="UTF-8";
        this.dataTagPosition=0;
    }

    @Contract(pure = true)
    public String getPingMessage() {
        return pingMessage;
    }

    @Contract(pure = true)
    public String getDataMessage() {
        return dataMessage;
    }

    @Contract(pure = true)
    public String getHelloMessage() {
        return helloMessage;
    }

    @Contract(pure = true)
    public String getServerIsReadyMessage() {
        return serverIsReadyMessage;
    }

    @Contract(pure = true)
    public Charset getCharset() {
        return Charset.forName(charset);
    }

    @Contract(pure = true)
    public int getDataTagPosition() {
        return dataTagPosition;
    }
}
