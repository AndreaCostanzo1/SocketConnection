package socket_connection.tools;

import socket_connection.socket_exceptions.exceptions.FailedToConnectException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SocketStreamsHandler {

    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;
    private final Lock outputStreamLock=new ReentrantLock();
//    private final Lock inputStreamLock=new ReentrantLock();

    /**
     * This constructor is used to create an instance of this class used to
     * handle socket's streams
     * @throws FailedToConnectException if the host/server is unreachable
     */
    public SocketStreamsHandler(Socket socket) throws FailedToConnectException {
        try {
            inputStream= new DataInputStream(socket.getInputStream());
            outputStream=new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new FailedToConnectException();
        }
    }

    /**
     * This method is used to write a string on the output stream
     * @param data the string to be written
     * @throws IOException when the connection is down.
     */
    public void writeUTF(String data) throws IOException {
        try{
            outputStreamLock.lock();
            outputStream.writeUTF(data);
        } catch (IOException e){
            throw new IOException(e);
        } finally {
            outputStreamLock.unlock();
        }
    }

    /**
     * This method is used to read a string from the input stream
     * @throws IOException when the connection is down.
     */
    public String aSyncReadUTF() throws IOException{
        return inputStream.readUTF();
    }

    /**
     * This method is used to check how many bytes are in the
     * input stream.
     * @return the number of bytes present
     * @throws IOException if the connection is down
     */
    public int availableData() throws IOException {
        return inputStream.available();
    }
}
