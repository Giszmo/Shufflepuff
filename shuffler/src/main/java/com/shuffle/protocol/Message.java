package com.shuffle.protocol;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.EncryptionKey;
import com.shuffle.cryptocoin.Signature;

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
}
