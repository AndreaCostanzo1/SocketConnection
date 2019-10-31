package socket_connection.cryptography;

import socket_connection.cryptography.exceptions.OperationNotPossibleException;

public interface Decrypter {
    byte[] decrypt(byte[] toDecrypt) throws OperationNotPossibleException;
}
