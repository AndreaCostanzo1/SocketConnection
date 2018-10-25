package socket_connection.tools;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import socket_connection.cryptography.Decrypter;
import socket_connection.cryptography.Encrypter;
import socket_connection.socket_exceptions.runtime_exceptions.UndefinedInputTypeException;

import java.nio.charset.Charset;

public class DataFormatter {


    private final Charset charset;
    /* todo implement
    private Encrypter encrypter;
    private Decrypter decrypter;
    */

    /**
     * Data formatter is a final class with only static methods.
     * Create an instance of this class isn' allowed
     */
    public DataFormatter(Charset charset){
        this.charset=charset;
    }

    /**
     * @param data containing data to box
     * @return a string containing data as bytes
     */
    public String box(String data){
        Gson gson= new Gson();
        return gson.toJson(data.getBytes(charset));
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
            return new String(gson.fromJson(data, byte[].class), charset);
        }catch (JsonSyntaxException e){
            throw new UndefinedInputTypeException();
        }
    }

    public Charset getCharset() {
        return charset;
    }
}
