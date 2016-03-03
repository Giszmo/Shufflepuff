package com.shuffle.bitcoin;

import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Message;
import com.shuffle.protocol.Packet;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.DeterministicSeed;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;


public class BitcoinCrypto implements Crypto {
    // Figure out which network we should connect to. Each one gets its own set of files.
    NetworkParameters params = TestNet3Params.get();
    String fileprefix;

   WalletAppKit kit = new WalletAppKit(params,new File("."),fileprefix);
   DeterministicSeed seed = kit.wallet().getKeyChainSeed();
   List<String> mnemonicCode = seed.getMnemonicCode();
   SecureRandom sr = new SecureRandom(seed.getSeedBytes());

   try{
      KeyGenerator keyGenerator = KeyGenerator.getInstance("HMacSP800DRBG");
      final int keylength= 256;
      keyGenerator.init(keylength,sr);
      SecretKey secretKey = keyGenerator.generateKey();

   }
   catch (NoSuchAlgorithmException e){
   }


   ECKey ecKey = new ECKey(sr);

   //using bitcoinj
   Address address = (Address) kit.wallet().freshReceiveKey().toAddress(params);
   //using ECkey
   Address ecaddress = (Address) ecKey.toAddress(params);



   SigningKey sk = new SigningKey() {

       @Override
       public VerificationKey VerificationKey() throws CryptographyError {
          VerificationKey vec = new VerificationKey() {
             @Override
             public boolean verify(Transaction t, Signature sig) throws InvalidImplementationError {
                return false;
             }

             @Override
             public boolean verify(Packet packet, Signature sig) {
                return false;
             }

             @Override
             public Address address() {
                return (Address) ecKey.toAddress(params);
             }

             @Override
             public int compareTo(Object o) {
                return 0;
             }
          };

          return vec;
       }

       @Override
       public Signature makeSignature(Transaction t) throws CryptographyError {
          return null;
       }

       @Override
       public Signature makeSignature(Packet p) throws CryptographyError {
          return null;
       }

       @Override
       public int compareTo(Object o) {
          return 0;
       }

    }




    @Override
    public DecryptionKey makeDecryptionKey() throws CryptographyError {
        return (DecryptionKey)kit.wallet().currentReceiveKey();
    }

    @Override
    public SigningKey makeSigningKey() throws CryptographyError {
        return (SigningKey) kit.wallet().currentKey(K);

    }

    @Override
    public int getRandom(int n) throws CryptographyError, InvalidImplementationError, NoSuchProviderException, NoSuchAlgorithmException {
         return sr.nextInt(n);
    }

    @Override
    public Message hash(Message m) throws CryptographyError, InvalidImplementationError {
        return null;
    }

   //    @Override
//    public long valueHeld(Address addr) throws CoinNetworkError {
//
//
//////        blockexplorer.com api for lookup
//////        validate address
//////        /api/addr-validate/[:addr]
//////        /api/addr/[:addr][?noTxList=1&noCache=1]
//////        /api/addr/19SokJG7fgk8iTjemJ2obfMj14FM16nqzj
//////
//////        Example URL: will return value in satoshis https://blockexplorer.com/api/addr/1DeQV7NiCkUYUdz9yo6Cr4mBNUUxYravfW/balance
////        int vh;
////        OkHttpClient client = new OkHttpClient();
////        String url= "https://blockexplorer.com/api/addr/"+addr+"/balance";
////        private String run(String url) throws IOException {
////            Request request = new Request.Builder()
////                  .url(url)
////                  .build();
////
////        OkHttpClient client;
////        Response response = client.newCall(request).execute();
////      int vh = Integer.parseInt(response.body().string());
////            return  response.body().string();
////        }
////        vh = Integer.parseInt(run);
////        return vh;
//        return 0;
//    }
}
