package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.EncryptionKey;

/**
 * Created by conta on 01.04.16.
 */
public class EncryptionKeyImpl implements EncryptionKey {

   byte[] ekey;

   public EncryptionKeyImpl() {

   }

   @Override
   public Address encrypt(Address m) throws CryptographyError {
      return null;
   }
}
