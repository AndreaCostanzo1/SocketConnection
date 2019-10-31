package socket_connection.cryptography;

import socket_connection.cryptography.exceptions.OperationNotPossibleException;

public interface Encrypter {
    byte[] encrypt(byte[] toEncrypt) throws OperationNotPossibleException;
}
