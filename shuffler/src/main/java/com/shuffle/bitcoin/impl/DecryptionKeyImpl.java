package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.protocol.FormatException;

import org.bitcoinj.core.ECKey;


/**
 * A private key used for decryption.
 */

public class DecryptionKeyImpl implements DecryptionKey {

   final int index;
   final ECKey key;
   final byte[] encryptionKey;


   public DecryptionKeyImpl(org.bitcoinj.core.ECKey key, int index) {
      this.key = key;
      this.index = index;
      this.encryptionKey = key.getPubKey();
   }

   @Override
   public EncryptionKey EncryptionKey() {
      return new EncryptionKeyImpl(key);
   }

   //not sure if that is meant to be passing a Message m?
   @Override
   public Address decrypt(Address m) throws FormatException, CryptographyError {
      return null;
   }
}
