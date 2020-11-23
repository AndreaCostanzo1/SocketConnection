package socket_connection.cryptography;

import socket_connection.cryptography.exceptions.OperationNotPossibleException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class RSADecrypter implements Decrypter {

    private final Cipher cipher;

    public RSADecrypter(Key privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        cipher= Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
    }
    @Override
    public byte[] decrypt(byte[] toDecrypt) throws OperationNotPossibleException {
        try {
            return cipher.doFinal(toDecrypt);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new OperationNotPossibleException();
        }
    }
}
