package com.shuffle.cryptocoin;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.CryptographyError;

/**
 * Created by Daniel Krawisz on 12/4/15.
 */
public interface EncryptionKey {
    Address encrypt(Address m) throws CryptographyError;
}
