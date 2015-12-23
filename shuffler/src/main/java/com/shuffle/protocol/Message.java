package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/19/15.
 */
public interface Message {
    boolean isEmpty();

    Message attach(EncryptionKey ek);
    Message attach(Coin.Address addr);
    Message attach(Coin.Signature sig);
    Message attach(BlameMatrix.Blame blame);

    Message attach(Message message) throws InvalidImplementationError;

    EncryptionKey readEncryptionKey() throws FormatException;
    Coin.Signature readCoinSignature() throws FormatException;
    Coin.Address readCoinAddress() throws FormatException;
    BlameMatrix.Blame readBlame() throws FormatException;
}
