package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.EncryptionKey;

import org.bitcoinj.core.ECKey;

/**
 * Created by conta on 01.04.16.
 */
public class EncryptionKeyImpl implements EncryptionKey {

   byte[] encryptionKey;

   public EncryptionKeyImpl(ECKey ecKey) {
      this.encryptionKey = ecKey.getPubKey();
   }

   @Override
   public Address encrypt(Address m) throws CryptographyError {
      return null;
   }
}
