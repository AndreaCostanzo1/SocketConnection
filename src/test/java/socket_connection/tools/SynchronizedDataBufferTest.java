package socket_connection.tools;

import org.junit.jupiter.api.Test;
import socket_connection.socket_exceptions.exceptions.BadMessagesSequenceException;
import socket_connection.socket_exceptions.runtime_exceptions.ShutDownException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;


class SynchronizedDataBufferTest {
    //****************************************************************************************
    //
    //                         TEST: final parameters
    //
    //****************************************************************************************
    @Test
    void firstElementTest() throws IllegalAccessException {
        try {
            SynchronizedDataBuffer buffer= new SynchronizedDataBuffer();
            //getting FIRST_ELEMENT with reflection
            Field field= SynchronizedDataBuffer.class.getDeclaredField("FIRST_ELEMENT");
            field.setAccessible(true);
            //proper cast to expected type
            Integer i= Integer.class.isInstance(field.get(buffer))?
                    Integer.class.cast(field.get(buffer)):
                    null;
            Optional<Integer> firstElementValue= Optional.ofNullable(i);
            firstElementValue.ifPresentOrElse(value -> assertEquals(0, value.intValue()),
                    () -> fail("Unexpected element type"));

        } catch (NoSuchFieldException e) {
            fail("The field doesn't exist. You probably need to modify this test!");
        }
    }

    //****************************************************************************************
    //
    //                         TEST: void put(String string)
    //
    //****************************************************************************************
    @Test @SuppressWarnings("all")
    void bufferDimensionAfterPut() throws IllegalAccessException {
        SynchronizedDataBuffer buffer= new SynchronizedDataBuffer();
        //getting actual buffer
        Optional<List> actualBuffer= getActualBuffer(buffer);
        //assert correct buffer dimention
        assertTrue(actualBuffer.isPresent(),"Unexpected type. You probably need to modify this test!");
        actualBuffer.ifPresent(a->assertTrue(a.isEmpty())); //still empty cause any element wasn't added.
        //insert elements and check new dimension
        buffer.put("Element 1");
        actualBuffer.ifPresent(a-> assertEquals(1, a.size()));
        buffer.put("Element 2");
        actualBuffer.ifPresent(a-> assertEquals(2, a.size()));
        buffer.put("Element 3");
        actualBuffer.ifPresent(a-> assertEquals(3, a.size()));
    }

    @Test
    void testWakeUp(){
        SynchronizedDataBuffer buffer= new SynchronizedDataBuffer();
        try {
            Method method = buffer.getClass().getDeclaredMethod("waitForData");
            method.setAccessible(true);
            Thread thread= new Thread(() -> {
                try {
                    method.invoke(buffer);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    fail(e);
                }
                while (true){
                    if(Thread.currentThread().isInterrupted()) break;
                }
            });
            thread.start();
            await("Waiting for Waiting status of thread").atMost(200, TimeUnit.MILLISECONDS )
                    .until(thread::getState,is(Thread.State.WAITING));
            buffer.put("Element");
            await("Waiting for thread to become runnable again").atMost(200, TimeUnit.MILLISECONDS )
                    .until(thread::getState,is(Thread.State.RUNNABLE));
            thread.interrupt();
            await("Waiting for thread to close properly").atMost(200, TimeUnit.MILLISECONDS )
                    .until(thread::getState,is(Thread.State.TERMINATED)); //just to see if thread is closed properly
        } catch (NoSuchMethodException e) {
            fail("Method not found");
        }

    }

    //****************************************************************************************
    //
    //                         TEST: String popString()
    //
    //****************************************************************************************
    @Test
    void popIntegerTest() throws IllegalAccessException, BadMessagesSequenceException {
        SynchronizedDataBuffer buffer= new SynchronizedDataBuffer();
        //getting actual buffer
        Optional<List> actualBuffer= getActualBuffer(buffer);
        //assert correct buffer dimension
        assertTrue(actualBuffer.isPresent(),"Unexpected type. You probably need to modify this test!");
        actualBuffer.ifPresent(a->assertTrue(a.isEmpty())); //still empty cause any element wasn't added.
        //insert element and check dimension
        buffer.put("1");
        actualBuffer.ifPresent(a-> assertEquals(1, a.size()));
        //pop a string and check buffer is empty again
        buffer.popInt();
        actualBuffer.ifPresent(a->assertTrue(a.isEmpty())); //now buffer should be empty again
    }

    @Test
    void testSleepIfBufferIsEmpty2(){
        SynchronizedDataBuffer buffer= new SynchronizedDataBuffer();
        //this thread will ask to pop an int when the buffer is empty,
        Thread thread= new Thread(() -> {
            try {
                buffer.popInt();
            } catch (BadMessagesSequenceException e) {
                fail("Something went wrong");
            } catch (ShutDownException e){
                await("Waiting for thread to close properly").atMost(200, TimeUnit.MILLISECONDS )
                        .untilAsserted(()->assertTrue(Thread.currentThread().isInterrupted()));
            }
        });
        thread.start();
        await("Waiting for Waiting status of thread").atMost(200, TimeUnit.MILLISECONDS )
                .until(thread::getState,is(Thread.State.WAITING));
        //check if the thread is killed properly
        buffer.closeBuffer();
        await("Waiting for thread to close properly").
                until(thread::getState,is(Thread.State.TERMINATED));
    }

    @Test
    void popIntegerWhenANonIntegerIsInTheFirstPosition() throws IllegalAccessException {
        SynchronizedDataBuffer buffer= new SynchronizedDataBuffer();
        //getting actual buffer
        Optional<List> actualBuffer= getActualBuffer(buffer);
        //assert correct buffer dimension
        assertTrue(actualBuffer.isPresent(),"Unexpected type. You probably need to modify this test!");
        actualBuffer.ifPresent(a->assertTrue(a.isEmpty())); //still empty cause any element wasn't added.
        //insert element and check dimension
        buffer.put("Element");
        actualBuffer.ifPresent(a-> assertEquals(1, a.size()));
        //pop a string and check buffer is empty again
        assertThrows(BadMessagesSequenceException.class, buffer::popInt);
        actualBuffer.ifPresent(a-> assertEquals(1, a.size())); //still empty cause any element wasn't added.
    }


    //****************************************************************************************
    //
    //                         TEST: String popString()
    //
    //****************************************************************************************
    @Test
    void popAString() throws IllegalAccessException {
        SynchronizedDataBuffer buffer= new SynchronizedDataBuffer();
        //getting actual buffer
        Optional<List> actualBuffer= getActualBuffer(buffer);
        //assert correct buffer dimension
        assertTrue(actualBuffer.isPresent(),"Unexpected type. You probably need to modify this test!");
        actualBuffer.ifPresent(a->assertTrue(a.isEmpty())); //still empty cause any element wasn't added.
        //insert element and check dimension
        buffer.put("Elemento");
        actualBuffer.ifPresent(a-> assertEquals(1, a.size()));
        //pop a string and check buffer is empty again
        buffer.popString();
        actualBuffer.ifPresent(a->assertTrue(a.isEmpty())); //empty because the element was removed
    }

    @Test
    void testSleepIfBufferIsEmpty(){
        SynchronizedDataBuffer buffer= new SynchronizedDataBuffer();
        Thread thread= new Thread(buffer::popString);
        thread.start();
        await("Waiting for Waiting status of thread").atMost(200, TimeUnit.MILLISECONDS )
                .until(thread::getState,is(Thread.State.WAITING));
    }


    //****************************************************************************************
    //
    //                         TEST: String size()
    //
    //****************************************************************************************
    @Test
    void getSizeTest() throws IllegalAccessException {
        SynchronizedDataBuffer buffer= new SynchronizedDataBuffer();
        //getting actual buffer
        Optional<List> actualBuffer= getActualBuffer(buffer);
        //assert correct buffer dimension
        assertTrue(actualBuffer.isPresent(),"Unexpected type. You probably need to modify this test!");
        assertEquals(0, buffer.size());
        //insert element and check dimension
        buffer.put("Elemento");
        assertEquals(1, buffer.size());
        //pop a string and check buffer is empty again
        buffer.popString();
        assertEquals(0, buffer.size());
        //insert others two elements
        buffer.put("Elemento 1");
        assertEquals(1, buffer.size());
        buffer.put("Elemento 2");
        assertEquals(2, buffer.size());
    }

    //---------------------------------------------------------------------------------------
    //
    //                                 SUPPORT METHODS
    //
    //---------------------------------------------------------------------------------------
    @SuppressWarnings("all")
    private Optional<List> getActualBuffer(SynchronizedDataBuffer buffer) throws IllegalAccessException {
        Field field=null;
        try {
            //getting buffer with reflection
            field= buffer.getClass().getDeclaredField("buffer");
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            fail("The field doesn't exist. You probably need to modify this test!");
        }
        //operation to cast buffer to a List
        Optional<Object> notNullBuffer=Optional.ofNullable(field.get(buffer));
        assertTrue(notNullBuffer.isPresent(),"Buffer not initialized!");
        return List.class.isInstance(notNullBuffer.get())?
                Optional.ofNullable(List.class.cast(notNullBuffer.get()))
                :Optional.ofNullable(null);
    }
}
