package socket_connection.tools;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import socket_connection.socket_exceptions.runtime_exceptions.UndefinedInputTypeException;

import java.nio.charset.Charset;

public final class DataFormatter {


    /**
     * Data formatter is a final class with only static methods.
     * Create an instance of this class isn' allowed
     */
    private DataFormatter(){
        throw new AssertionError();
    }

    /**
     * @param data containing data to box
     * @param encode represent the charset used to encode data
     * @return a string containing data as bytes
     */
    public static String box(String data, Charset encode){
        Gson gson= new Gson();
        return gson.toJson(data.getBytes(encode));
    }

    /**
     * @param data containing data to box
     * @param decode represent the charset used to encode data
     * @return a string containing data as bytes
     * @exception UndefinedInputTypeException is launched if the string received isn't a bytes representation of
     * something
     */
    public static String unBox(String data, Charset decode){
        Gson gson= new Gson();
        try {
            return new String(gson.fromJson(data, byte[].class), decode);
        }catch (JsonSyntaxException e){
            throw new UndefinedInputTypeException();
        }
    }
}
