/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.bitcoin;

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
    // Generate new signing key.
    // (The key doesn't have to have any bitcoins in it; it can just be new.)
    SigningKey makeSigningKey();

    // Generate new decryption key.
    DecryptionKey makeDecryptionKey();

    // Get a random number between 0 and N inclusive.
    int getRandom(int n);
}
