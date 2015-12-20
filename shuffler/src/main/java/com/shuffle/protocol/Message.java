package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/19/15.
 */
public interface Message {
    boolean isEmpty();

    Message attach(EncryptionKey ek);
    Message attach(Coin.CoinAddress addr);
    Message attach(CoinSignature sig);

    Message attach(Message message) throws InvalidImplementationError;

    EncryptionKey readEncryptionKey() throws FormatException;
    CoinSignature readCoinSignature() throws FormatException;
    Coin.CoinAddress readCoinAddress() throws FormatException;
}
