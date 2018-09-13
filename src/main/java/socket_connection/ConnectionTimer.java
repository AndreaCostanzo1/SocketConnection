package socket_connection;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class ConnectionTimer {

    private SocketConnection connectionHandled;
    private long timePassedInSec;
    private long timerSet=5; // FIXME: 12/09/2018
    private final Timer timer = new Timer();
    private TimerTask connectionTimerTask;
    private final Lock lock=new ReentrantLock();

    ConnectionTimer(SocketConnection connection){
        this.connectionHandled=connection;
        connectionTimerTask = new TimerTask() {
            @Override
            public void run() {
                lock.lock();
                long actualTime=++timePassedInSec;
                lock.unlock();
                if (actualTime >= timerSet) {
                    timer.cancel();
                    connectionHandled.shutdown();
                }

            }
        };
    }

    void resetTTL(){
        lock.lock();
        timePassedInSec=0;
        lock.unlock();
    }

    void launch(){
        timer.scheduleAtFixedRate(connectionTimerTask,1,1000);
    }
}
