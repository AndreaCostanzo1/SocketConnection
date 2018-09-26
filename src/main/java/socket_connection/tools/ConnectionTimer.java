package socket_connection.tools;

import socket_connection.SocketConnection;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionTimer {

    private SocketConnection connectionHandled;
    private long timePassedInSec;
    private long timerSet;
    private final Timer timer = new Timer();
    private boolean running;
    private TimerTask connectionTimerTask;
    private final Lock lock=new ReentrantLock();
    private final Lock timerLock= new ReentrantLock();


    /**
     * Public constructor for the timer
     * @param connection is the connection controlled by the
     *                   instance of the timer just created
     */
    public ConnectionTimer(SocketConnection connection){
        this.connectionHandled=connection;
        this.running=false;
        timerSet=connection.getTimeToLive();
        connectionTimerTask = new TimerTask() {
            @Override
            public void run() {
                lock.lock();
                long actualTime=++timePassedInSec;
                lock.unlock();
                if (actualTime >= timerSet) {
                    timerLock.lock();
                    timer.cancel();
                    timerLock.unlock();
                    connectionHandled.shutdown();
                }
            }
        };
    }

    /**
     * This method resets timer "countdown"
     */
    public void resetTTL(){
        lock.lock();
        timePassedInSec=0;
        lock.unlock();
    }

    /**
     * This method starts timer "countdown".
     */
    public void launch(){
        timerLock.lock();
        timer.scheduleAtFixedRate(connectionTimerTask,1,1000);
        running=true;
    }

    /**
     * This method is used to stop the timer.
     */
    public void stop() {
        timerLock.lock();
        if(running) {
            timer.cancel();
            running=false;
        }
        timerLock.unlock();
    }
}
