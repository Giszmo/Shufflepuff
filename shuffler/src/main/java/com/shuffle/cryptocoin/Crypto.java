package com.shuffle.cryptocoin;

import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Message;

/**
 *
 * A set of cryptographic functions used in the protocol. According to the original paper,
 * these functions cannot just be anything. Specific functions are required. Refer to the
 * original paper in order to implement this interface.
 *
 * http://crypsys.mmci.uni-saarland.de/projects/CoinShuffle/coinshuffle.pdf
 *
 * Created by Daniel Krawisz on 12/4/15.
 */
public interface Crypto {
    // Generate new decryption key.
    DecryptionKey DecryptionKey() throws CryptographyError;
    // Generate new signing key.
    SigningKey makeSigningKey() throws CryptographyError;
    // Get a random number between 0 and N inclusive.
    int getRandom(int n) throws CryptographyError, InvalidImplementationError;
    // Hash a message.
    Message hash(Message m) throws CryptographyError, InvalidImplementationError;
}
