package com.shuffle.protocol;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.Signature;

/**
 * Created by Daniel Krawisz on 12/19/15.
 */
public interface Message {
    boolean isEmpty();

    Message attach(EncryptionKey ek);
    Message attach(Address addr);
    Message attach(Signature sig);
    Message attach(BlameMatrix.Blame blame);

    Message attach(Message message) throws InvalidImplementationError;

    EncryptionKey readEncryptionKey() throws FormatException;
    Signature readSignature() throws FormatException;
    Address readAddress() throws FormatException;
    BlameMatrix.Blame readBlame() throws FormatException;

    Message copy() throws InvalidImplementationError;
}
