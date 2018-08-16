package socket_connection;

import socket_connection.socket_exceptions.BadMessagesSequenceException;
import socket_connection.socket_exceptions.ShutDownException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SynchronizedDataBuffer {
    private List<String> buffer;
    private Lock lock;
    private Condition condition;
    private static final int FIRST_ELEMENT=0;

    /**
     * Constructor of SynchronizedDataBuffer.
     * It's package-protected because only SocketConnection can create instances of this
     */
    SynchronizedDataBuffer(){
        buffer=new ArrayList<>();
        lock= new ReentrantLock();
        condition=lock.newCondition();
    }

    /**
     * This method let to insert a string in the buffer
     * @param string to be inserted
     */
    void put(String string){
        lock.lock();
        buffer.add(string);
        condition.signal();
        lock.unlock();
    }

    /**
     * if buffer is empty this method will set calling thread in a wait status
     * @exception ShutDownException launched if Thread.interrupt is called while thread is waiting for an element put in buffer
     */
    private void waitForData() {
        lock.lock();
        while (buffer.isEmpty()){
            try {
                condition.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ShutDownException();
            }
        }
        lock.unlock();
    }

    /**
     * Get the first element of the buffer as an int: if the operation goes well the element is also
     * removed from the buffer
     * @return the first element of the buffer, converted to an integer
     * @throws BadMessagesSequenceException when first element can't be converted in an integer
     * This means something went wrong while projecting message exchanges with remote host
     */
    int popInt() throws BadMessagesSequenceException {
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
     */
    String popString(){
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
    int size() {
        lock.lock();
        int dimension= buffer.size();
        lock.unlock();
        return dimension;
    }
}
