package com.shuffle.bitcoin.impl;

import junit.framework.TestCase;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * Created by conta on 31.03.16.
 */
public class AddressImplTest extends TestCase {
   NetworkParameters pnpar = org.bitcoinj.params.MainNetParams.get();
   NetworkParameters tnpar = org.bitcoinj.params.TestNet3Params.get();

   SecureRandom sr = new SecureRandom();
   SecureRandom sr2 = new SecureRandom();
   ECKey ecKey = new ECKey(sr);
   ECKey ecKey2 = new ECKey(sr2);
   org.bitcoinj.core.Address address = new org.bitcoinj.core.Address(tnpar, ecKey.getPubKeyHash());
   org.bitcoinj.core.Address address2 = new org.bitcoinj.core.Address(tnpar, ecKey2.getPubKeyHash());
   AddressImpl addressi = new AddressImpl(address);
   AddressImpl addressi2 = new AddressImpl(address2);

   @Test
   public void testCompareTo() {
      System.out.println("Network Parameters" + tnpar.toString());
      System.out.println("address: " + address.toString());
      System.out.println("address2: " + address2.toString());
      System.out.println("addressi: " + addressi.toString());
      System.out.println("addressi2: " + addressi2.toString());

      assertEquals(1, addressi.compareTo(new AddressImpl(address)));
   }

   @Test
   public void testCompareTo2() {

      assertEquals(0, addressi2.compareTo(addressi2));
   }
}