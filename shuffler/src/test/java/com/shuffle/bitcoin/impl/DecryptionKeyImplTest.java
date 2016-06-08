package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;

import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;

import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Enumeration;

import static org.junit.Assert.assertEquals;

/**
 * Created by conta on 02.06.16.
 */
public class DecryptionKeyImplTest {

   ECKey ecKey;
   BitcoinCrypto bitcoinCrypto;
   DecryptionKey decryptionKey;
   SecureRandom secureRandom;
   EncryptionKey encryptionKey;

   @Before
   public void setUp() throws Exception {
      this.bitcoinCrypto = new BitcoinCrypto();
      this.secureRandom = new SecureRandom();
      this.ecKey = new ECKey(secureRandom);
      this.decryptionKey = new DecryptionKeyImpl(this.ecKey);
      //this.hKey = ECKey.fromPrivate()

   }


   @Test
   public void testToString() throws Exception {
      System.out.println("\nBegin Test toString:");
      String string = this.ecKey.getPrivateKeyAsWiF(bitcoinCrypto.getParams()).toString();
      System.out.println("ECKey: " + this.ecKey);
      System.out.println("String ECKey WIF: " + string);
      System.out.println("String DecryptionKey: " + this.decryptionKey.toString());
      assertEquals("toString Method: ", string, this.decryptionKey.toString());

   }

   @Test
   public void testEncryptionKey() throws Exception {

      System.out.println("\nBegin Test encryptionKey:");
      byte[] pub = ECKey.publicKeyFromPrivate(ecKey.getPrivKey(), ecKey.isCompressed());
      EncryptionKey encryptionKey1 = new EncryptionKeyImpl(ECKey.fromPublicOnly(ecKey.getPubKey()));
//
//      PublicKey publicKey = BitcoinCrypto.loadPublicKey(Base64.getEncoder().encodeToString(ecKey.getPubKey()));
      Provider p[] = Security.getProviders();
      for (int i = 0; i < p.length; i++) {
         System.out.println(p[i]);
         for (Enumeration e = p[i].keys(); e.hasMoreElements(); )
            System.out.println("\t" + e.nextElement());
      }
      System.out.println("ecKey: " + ecKey.toString());
      System.out.println("ecKey priv: " + ecKey.getPrivateKeyAsHex());
      System.out.println("secureRandom: " + secureRandom.toString());
      System.out.println("decryptionKey: " + decryptionKey.toString());
      System.out.println("ASN.1  " + ecKey.toASN1().toString());

      EncryptionKeyImpl encTest = new EncryptionKeyImpl(pub);
      System.out.println("\nencTest: " + encTest);
      System.out.println("encryptionKey: " + encryptionKey1);
      System.out.println("EncKey.toString from ECKeys Pub: " + encTest.toString());
      System.out.println("EncKey from DecKey to string: " + decryptionKey.EncryptionKey().toString());

      encryptionKey = new EncryptionKeyImpl(ecKey.getPubKey());
      assertEquals(encryptionKey.toString(), decryptionKey.EncryptionKey().toString());

   }

   @Test
   public void testDecrypt() throws Exception {


   }
}