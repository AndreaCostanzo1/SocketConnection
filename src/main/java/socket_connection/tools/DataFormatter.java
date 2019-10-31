package socket_connection.tools;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import socket_connection.cryptography.*;
import socket_connection.cryptography.exceptions.OperationNotPossibleException;
import socket_connection.socket_exceptions.runtime_exceptions.UndefinedInputTypeException;

import javax.crypto.NoSuchPaddingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataFormatter {


    private final Charset charset;
    private Encrypter encrypter;
    private Decrypter decrypter;
    private Logger logger;


    /**
     * Data formatter is a final class with only static methods.
     * Create an instance of this class isn' allowed
     */
    public DataFormatter(Charset charset){
        this.charset=charset;
        logger=Logger.getLogger(DataFormatter.class.toString()+"%u");
    }

    public void setUpEncryption(Key myPrivateKey, Key foreignPublicKey){
        try {
            encrypter = new RSAEncrypter(foreignPublicKey);
            decrypter = new RSADecrypter(myPrivateKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            logger.log(Level.SEVERE, "ERROR WHILE SETTING UP ENCRYPTION");
        }
    }

    /**
     * @param data containing data to box
     * @return a string containing data as bytes
     */
    public String box(String data){
        Gson gson= new Gson();
        byte[] rawData = data.getBytes(charset);
        byte[] encryptedData;
        try {
            encryptedData = encrypter!=null ? encrypter.encrypt(rawData) : rawData;
        } catch (OperationNotPossibleException e) {
            logger.log(Level.SEVERE, "CAN'T ENCRYPT DATA");
            encryptedData=rawData;
        }
        return gson.toJson(encryptedData);
    }

    /**
     * @param data containing data to box
     * @return a string containing data as bytes
     * @exception UndefinedInputTypeException is launched if the string received isn't a bytes representation of
     * something
     */
    public String unBox(String data){
        Gson gson= new Gson();
        try {
            //get bytes relative to raw data
            byte[] rawData = gson.fromJson(data, byte[].class);
            //decrypt them
            byte[] decryptedData;
            try {
                decryptedData = decrypter!=null? decrypter.decrypt(rawData) : rawData;
            } catch (OperationNotPossibleException e) {
                logger.log(Level.SEVERE, "CAN'T DECRYPT DATA");
                decryptedData=rawData;
            }
            //converting them into a String
            return new String(decryptedData, charset);
        }catch (JsonSyntaxException e){
            throw new UndefinedInputTypeException();
        }
    }

    /**
     * @return the charset used by the data formatter
     */
    public Charset getCharset() {
        return charset;
    }
}
