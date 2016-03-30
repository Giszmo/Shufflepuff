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
 * A public encryption key.
 *
 * Created by Daniel Krawisz on 12/4/15.
 */
public interface EncryptionKey {
    Address encrypt(Address m) throws CryptographyError;
}
