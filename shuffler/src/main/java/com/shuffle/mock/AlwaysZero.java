package com.shuffle.mock;

import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.mock.MockCrypto.Random;
import com.shuffle.protocol.InvalidImplementationError;

/**
 * This is used for certain test cases in which we have to control the shuffling. Effectively
 * it ensures that a player will never change the list of addresses when he shuffles them.
 *
 * Created by Daniel Krawisz on 3/23/16.
 */
public class AlwaysZero implements Random{
    @Override
    public int getRandom(int n) throws CryptographyError, InvalidImplementationError {
        return 0;
    }
}
