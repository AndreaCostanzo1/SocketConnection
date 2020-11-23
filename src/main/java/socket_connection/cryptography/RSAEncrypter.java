package socket_connection.cryptography;

import socket_connection.cryptography.exceptions.OperationNotPossibleException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class RSAEncrypter implements Encrypter {

    private final Cipher cipher;

    public RSAEncrypter(Key publicKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        cipher=Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
    }

    @Override
    public byte[] encrypt(byte[] toEncrypt) throws OperationNotPossibleException {
        try {
            return cipher.doFinal(toEncrypt);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new OperationNotPossibleException();
        }
    }
}
