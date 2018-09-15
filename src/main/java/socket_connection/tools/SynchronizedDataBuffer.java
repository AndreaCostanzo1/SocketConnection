package socket_connection.tools;

import socket_connection.socket_exceptions.exceptions.BadMessagesSequenceException;
import socket_connection.socket_exceptions.runtime_exceptions.ShutDownException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SynchronizedDataBuffer {
    private List<String> buffer;
    private boolean connectionDown;
    private Lock lock;
    private Condition condition;
    private static final int FIRST_ELEMENT=0;

    /**
     * Constructor of SynchronizedDataBuffer.
     */
    public SynchronizedDataBuffer(){
        buffer=new ArrayList<>();
        lock= new ReentrantLock();
        connectionDown=false;
        condition=lock.newCondition();
    }

    /**
     * This method let to insert a string in the buffer
     * @param string to be inserted
     */
    public void put(String string){
        lock.lock();
        buffer.add(string);
        condition.signal();
        lock.unlock();
    }

    /**
     * if buffer is empty this method will set calling thread in a wait status
     * @exception ShutDownException launched if the connection have been closed while the calling thread is waiting for
     * an element put in buffer
     */
    // FIXME: 15/09/2018 how to grant that messages sent are still held in the buffer?
    @SuppressWarnings("all")
    private void waitForData() {
        boolean throwException=false;
        lock.lock();
        while (buffer.isEmpty()&&!connectionDown){
            try {
                condition.await();
                if(connectionDown)throwException=true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throwException=true;
            }
        }
        lock.unlock();
        if(throwException) throw new ShutDownException();
    }

    /**
     * Get the first element of the buffer as an int: if the operation goes well the element is also
     * removed from the buffer
     * @return the first element of the buffer, converted to an integer
     * @throws BadMessagesSequenceException when first element can't be converted in an integer
     * This means something went wrong while projecting message exchanges with remote host
     * @exception ShutDownException launched if the connection have been closed while the calling thread is waiting for
     * an element put in buffer
     */
    public int popInt() throws BadMessagesSequenceException {
        waitForData();
        String data=popFirstElem();
        try {
            int intToReturn=Integer.parseInt(data);
            removeFirstElem();
            return intToReturn;
        } catch (NumberFormatException e){
            throw new BadMessagesSequenceException(data);
        }
    }

    /**
     * Get the first element of the buffer, if the operation goes well the element is also
     * removed from the buffer
     * @return the first element of the buffer as a String
     * @exception ShutDownException launched if the connection have been closed while the calling thread is waiting for
     * an element put in buffer
     */
    public String popString(){
        waitForData();
        String toReturn=popFirstElem();
        removeFirstElem();
        return toReturn;
    }

    /**
     * @return the firstElement of the buffer
     */
    private String popFirstElem() {
        lock.lock();
        String firstElement=buffer.get(FIRST_ELEMENT);
        lock.unlock();
        return firstElement;
    }

    /**
     * remove the firstElement of the buffer
     */
    private void removeFirstElem(){
        lock.lock();
        buffer.remove(FIRST_ELEMENT);
        lock.unlock();
    }

    /**
     * @return the size of the buffer
     */
    public int size() {
        lock.lock();
        int dimension= buffer.size();
        lock.unlock();
        return dimension;
    }

    /**
     * used to notify the waiting thread that the connection using this buffer is closed.
     */
    public void closeBuffer(){
        lock.lock();
        connectionDown=true;
        condition.signal();
        lock.unlock();
    }
}
