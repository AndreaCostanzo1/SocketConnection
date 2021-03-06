package socket_connection;


import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import socket_connection.configurations.ConfigurationHandler;
import socket_connection.configurations.MessageHandlerConfigurations;
import socket_connection.configurations.ServerSocketConnectionConfigurations;
import socket_connection.configurations.SocketConnectionConfigurations;
import socket_connection.cryptography.Decrypter;
import socket_connection.cryptography.RSADecrypter;
import socket_connection.cryptography.exceptions.OperationNotPossibleException;
import socket_connection.socket_exceptions.exceptions.BadMessagesSequenceException;
import socket_connection.socket_exceptions.runtime_exceptions.BadSetupException;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class contains minor tests
 */
class MinorTests {

    private static final String ABSOLUTE_PATH = ConfigurationHandler.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    private static final String MESSAGE_HANDLER_CONFIGURATIONS_PATH = ABSOLUTE_PATH +"MessageHandlerConfigurations.json";
    private static final String SOCKET_CONNECTION_CONFIGURATIONS_PATH = ABSOLUTE_PATH +"SocketConnectionConfigurations.json";
    private static final String SERVER_SOCKET_CONNECTION_CONFIGURATIONS_PATH = ABSOLUTE_PATH +"ServerSocketConnectionConfigurations.json";

    /**
     * Test the getter method of the exception BadSequenceMessageException
     */
    @Test
    void testGetterBadSequenceMessageException(){
        String message = "Message";
        BadMessagesSequenceException e =new BadMessagesSequenceException(message);
        //assert
        assertEquals(message,e.getMessage());
    }

    /**
     * Test configuration handler in case of error in getting the file containing configurations
     */
    @Test
    void fileError(){
        File file= new File(MESSAGE_HANDLER_CONFIGURATIONS_PATH);
        File file2= new File(SOCKET_CONNECTION_CONFIGURATIONS_PATH);
        File file3= new File(SERVER_SOCKET_CONNECTION_CONFIGURATIONS_PATH);
        Gson gson = new Gson();
        try{
            //Case file exists
            MessageHandlerConfigurations messageHandlerConfigurations= gson.fromJson(new FileReader(file), MessageHandlerConfigurations.class);
            SocketConnectionConfigurations socketConnectionConfigurations= gson.fromJson(new FileReader(file), SocketConnectionConfigurations.class);
            ServerSocketConnectionConfigurations serverSocketConnectionConfigurations = gson.fromJson(new FileReader(file), ServerSocketConnectionConfigurations.class);
            //Assert that configuration files are deleted
            assertTrue(file.delete());
            assertTrue(file2.delete());
            assertTrue(file3.delete());
            //Create a new configurationHandler
            ConfigurationHandler handler = ConfigurationHandler.getInstance();
            //Assert that it is running default configurations
            assertTrue(handler.isRunningDefaultSSCConfigurations());
            assertTrue(handler.isRunningDefaultSCConfigurations());
            assertTrue(handler.isRunningDefaultMHConfigurations());
            //Rewrite files
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(gson.toJson(messageHandlerConfigurations));
            writer.close();
            BufferedWriter writer2 = new BufferedWriter(new FileWriter(file2));
            writer2.write(gson.toJson(socketConnectionConfigurations));
            writer2.close();
            BufferedWriter writer3 = new BufferedWriter(new FileWriter(file3));
            writer3.write(gson.toJson(serverSocketConnectionConfigurations));
            writer3.close();
        } catch (IOException e) {
            //File don't exist
            ConfigurationHandler handler = ConfigurationHandler.getInstance();
            assertTrue(handler.isRunningDefaultSSCConfigurations());
            assertTrue(handler.isRunningDefaultSCConfigurations());
            assertTrue(handler.isRunningDefaultMHConfigurations());
        }
    }

    /**
     * Test that rsa decrypter don't work with not encrypted data
     */
    @Test
    void rsaDecrypterWithNotEncryptedInput() throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        Decrypter decrypter = new RSADecrypter(keyPair.getPublic());
        byte[] notEncryptedArray=new byte[1];
        notEncryptedArray[0]=1;
        assertThrows(OperationNotPossibleException.class, () -> decrypter.decrypt(notEncryptedArray));
    }

    /**
     * Test BadMessagesSequenceException getter
     */
    @Test
    void badMessagesSequenceExceptionGetter(){
        String message= "message";
        BadMessagesSequenceException exception = new BadMessagesSequenceException(message);
        assertEquals(message,exception.getMessage());
    }

    /**
     * Other coverage tests
     */
    @Test
    void others(){
        RuntimeException exception= new BadSetupException();
        try{
            throw exception;
        }catch (RuntimeException e){
            //ignore
        }
    }
}

