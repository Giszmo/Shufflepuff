package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.bitcoin.blockchain.Bitcoin;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by conta on 07.06.16.
 */
public class SigningKeyImplTest {

   ECKey ecKey;
   Bitcoin bitcoin;
   SigningKey signingKey;
   VerificationKey verificationKey;
   BitcoinCrypto bitcoinCrypto;

   @Before
   public void setUp() throws Exception {

      this.ecKey = new ECKey();
      this.signingKey = new SigningKeyImpl(ecKey);

      bitcoinCrypto = new BitcoinCrypto();
   }

   @Test
   public void testToString() throws Exception {
      String ks = ecKey.getPrivateKeyAsWiF(NetworkParameters.fromID(NetworkParameters.ID_TESTNET)).toString();
      assertEquals(ks, signingKey.toString());
   }

   @Test
   public void testVerificationKey() throws Exception {

      verificationKey = new VerificationKeyImpl(this.ecKey.getPubKey());
      assertEquals(verificationKey.toString(), signingKey.VerificationKey().toString());
      System.out.println(signingKey.VerificationKey().address().toString());
      System.out.println(signingKey.toString());


   }

   @Test
   public void testMakeSignature() throws Exception {

   }

   @Test
   public void testMakeSignature1() throws Exception {

   }

   @Test
   public void testCompareTo() throws Exception {

   }
}