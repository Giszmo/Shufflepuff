package com.shuffle.protocol;

/**
 * A private key used for decryption.
 *
 * Created by Daniel Krawisz on 12/4/15.
 */
public interface DecryptionKey {
    EncryptionKey EncryptionKey();
    Coin.CoinAddress decrypt(Coin.CoinAddress m) throws FormatException, CryptographyException;
}