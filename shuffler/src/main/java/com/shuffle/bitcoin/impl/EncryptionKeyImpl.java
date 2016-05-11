package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.EncryptionKey;

import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.ECKey;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;

/**
 * Created by conta on 01.04.16.
 */
public class EncryptionKeyImpl implements EncryptionKey {

   ECKey encryptionKey;

   public EncryptionKeyImpl(byte[] ecPubKey) {
      this.encryptionKey = ECKey.fromPublicOnly(ecPubKey);
   }

   public String toString() {
      return this.encryptionKey.toString();
   }

   @Override
   public Address encrypt(Address m) {

      try {
         KeyFactory kf = KeyFactory.getInstance("ECIES");
         PublicKey pubKey = kf.generatePublic(kf.getKeySpec((Key) encryptionKey, KeySpec.class));

         //encrypt cipher
         Cipher cipher = Cipher.getInstance("ECIES");
         cipher.init(Cipher.ENCRYPT_MODE, pubKey);
         byte[] bytes = m.toString().getBytes(StandardCharsets.UTF_8);
         byte[] encrypted = cipher.doFinal(bytes);
         return new AddressImpl(Hex.encodeHexString(encrypted));

      } catch (Exception e) {
         e.printStackTrace();

      }
   }
