package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/4/15.
 */
public interface EncryptionKey {
    Coin.CoinAddress encrypt(Coin.CoinAddress m) throws CryptographyException;
}
