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

   int index;
   ECKey key;
   byte[] encryptionKey;


   public DecryptionKeyImpl(org.bitcoinj.core.ECKey key) {
      this.key = key;
      this.encryptionKey = key.getPubKey();
   }

   @Override
   public EncryptionKey EncryptionKey() {
      return (EncryptionKey) key.getPubKey();
   }

   @Override
   public Address decrypt(Address m) throws FormatException, CryptographyError {
      return null;
   }
}
