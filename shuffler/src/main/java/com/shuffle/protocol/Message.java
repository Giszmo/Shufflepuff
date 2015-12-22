package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/19/15.
 */
public interface Message {
    boolean isEmpty();

    Message attach(EncryptionKey ek);
    Message attach(Coin.CoinAddress addr);
    Message attach(Coin.CoinSignature sig);
    Message attach(Coin.CoinTransaction t);

    Message attach(Message message) throws InvalidImplementationError;

    EncryptionKey readEncryptionKey() throws FormatException;
    Coin.CoinSignature readCoinSignature() throws FormatException;
    Coin.CoinAddress readCoinAddress() throws FormatException;
}
