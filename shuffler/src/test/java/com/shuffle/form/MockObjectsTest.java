package com.shuffle.form;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * Created by Daniel Krawisz on 12/10/15.
 */
public class MockObjectsTest {

    @Test
    public void testMockObjects() {
        try {
            Crypto crypto = new MockCrypto();
            SigningKey sk = crypto.SigningKey();
            TestInterface.test(sk, new MockPacketFactory(sk.VerificationKey()), new MockCoin(new HashMap<VerificationKey, MockCoin.BlockchainEntry>()), crypto);
        } catch (InvalidImplementationException | CryptographyException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }
}
