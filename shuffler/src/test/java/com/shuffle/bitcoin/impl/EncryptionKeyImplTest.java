package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.EncryptionKey;

import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

import static org.junit.Assert.assertEquals;

/**
 * Created by conta on 07.06.16.
 */
public class EncryptionKeyImplTest {
    ECKey ecKey;
    ECKey pub;
    EncryptionKey ek;
    Address address;

    @Before
    public void setUp() throws Exception {
        ecKey = new ECKey();
        pub = ECKey.fromPublicOnly(ecKey.getPubKey());
        ek = new EncryptionKeyImpl(pub);
        address = new AddressImpl("myGgn8UojMsyqn6KGQLEbVbpYSePcKfawG");

        System.out.println("ecKey pubHex   " + ecKey.getPublicKeyAsHex());
        System.out.println("ecKey privHex  " + ecKey.getPrivateKeyAsHex());
        System.out.println("pub from ecKey " + pub);
        System.out.println("EncKey         " + ek);
    }

    @Test
    public void testToString() throws Exception {
        System.out.println("\ntestToString:");
        System.out.println("ecKeyPubAsHex:          " + ecKey.getPublicKeyAsHex());
        System.out.println("pub from ecKey toString " + pub.toString());
        System.out.println("EncKey.toString:        " + ek.toString());

        assertEquals("toString of ecKeyPubHex same as EncryptionKeyToString ", ecKey.getPublicKeyAsHex(), ek.toString());
    }

    @Test
    public void testEncrypt() throws Exception {
        System.out.println("\nTest encrypt");
        System.out.println("address                 " + address);
        System.out.println("ek                      " + ek);
        System.out.println(new AddressImpl(address.toString()));
        System.out.println("address encrypted to ek " + ek.encrypt(address));
    }
}