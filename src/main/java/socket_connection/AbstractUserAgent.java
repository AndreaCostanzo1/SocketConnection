package socket_connection;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public abstract class AbstractUserAgent implements SocketUserAgentInterface {
    private boolean shutdown;
    private Lock shutdownLock;
    private Lock setUpLock;
    private boolean errorDuringSetup;

    protected AbstractUserAgent(){
        shutdown=false;
        shutdownLock=new ReentrantLock();
        errorDuringSetup=false;
        setUpLock=new ReentrantLock();
    }

    protected void setToShutDown(){
        shutdownLock.lock();
        shutdown=true;
        shutdownLock.unlock();
    }

    protected boolean isShutDown(){
        shutdownLock.lock();
        boolean toReturn=shutdown;
        shutdownLock.unlock();
        return toReturn;
    }

    @Override
    public void notifySetUpError() {
        setUpLock.lock();
        errorDuringSetup=true;
        setUpLock.unlock();
    }

    protected boolean checkSetupError() {
        setUpLock.lock();
        boolean toReturn=errorDuringSetup;
        setUpLock.unlock();
        return toReturn;
    }

    public abstract void handleSetupError();
}