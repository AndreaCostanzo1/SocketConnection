package socket_connection;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class InputDecoderTest {

    /**
     * This test assert that is impossible to create an instance of InputDecoder
     */
    @Test
    void noInstancesCanBeCreated() {
        assertThrows(AssertionError.class, this::createInputDecoderInstance);
    }

    /**
     * This method try to create a InputDecoder instance. If the constructor launch an exception
     * the exception will be thrown up.
     *
     * In details:
     * Due to {@link Constructor#newInstance(Object...)} if an
     * exception is thrown the method will launch a InvocationTargetException that wrap
     * the exception thrown during construction. The try-catch block is used to re-throw the
     * exception launched by the constructor.
     */
    private void createInputDecoderInstance() throws Throwable {
        Constructor<InputDecoder> constructor= InputDecoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try{
            constructor.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
