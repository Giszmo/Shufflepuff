package com.shuffle.form;

import java.util.Queue;

/**
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface Message extends Iterable<Message> {
    EncryptionKey readAsEncryptionKey() throws FormatException;
    CoinSignature readAsSignature() throws FormatException;
    Queue<VerificationKey> readAsVerificationKeyList() throws FormatException;
    VerificationKey from();

    // Messages can be sequences of elements.
    Message append(Message a) throws InvalidImplementationException, FormatException;
    Message remove();
    int size();
    boolean equal(Message m);
}
