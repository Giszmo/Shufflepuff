package com.shuffle.protocol;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.Signature;
import com.shuffle.protocol.blame.Blame;

/**
 * Created by conta on 21.04.16.
 */
public class MessageImpl implements Message {

   @Override
   public boolean isEmpty() {
      return false;
   }

   @Override
   public Message attach(EncryptionKey ek) {
      return null;
   }

   @Override
   public Message attach(Address addr) {
      return null;
   }

   @Override
   public Message attach(Signature sig) {
      return null;
   }

   @Override
   public Message attach(Blame blame) {
      return null;
   }

   @Override
   public Message attach(Message message) throws InvalidImplementationError {
      return null;
   }

   @Override
   public EncryptionKey readEncryptionKey() throws FormatException {
      return null;
   }

   @Override
   public Signature readSignature() throws FormatException {
      return null;
   }

   @Override
   public Address readAddress() throws FormatException {
      return null;
   }

   @Override
   public Blame readBlame() throws FormatException, CryptographyError {
      return null;
   }

   @Override
   public Message rest() throws FormatException {
      return null;
   }
}
