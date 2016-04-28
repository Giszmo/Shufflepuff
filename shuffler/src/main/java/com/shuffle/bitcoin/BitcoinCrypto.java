package com.shuffle.bitcoin;

import com.shuffle.bitcoin.impl.DecryptionKeyImpl;
import com.shuffle.bitcoin.impl.SigningKeyImpl;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Message;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;


public class BitcoinCrypto implements Crypto {

   // Figure out which network we should connect to. Each one gets its own set of files.
   NetworkParameters params = TestNet3Params.get();
   String fileprefix = "_cosh";
   WalletAppKit kit;


   // if we generate new keys no need for mnemonic
   //List<String> mnemonicCode = seed.getMnemonicCode();


   //Generate Keypair using HMAC
   KeyPairGenerator keyPG = getKeyPGen();
   KeyPair keys = getKeyPair();
   KeyPair keyPair = keyPG.generateKeyPair();
   PrivateKey privKey = keyPair.getPrivate();
   PublicKey pubKey = keyPair.getPublic();



   DeterministicSeed seed = kit.wallet().getKeyChainSeed();
   SecureRandom sr = new SecureRandom(seed.getSeedBytes());
   ECKey ecKey = new ECKey(sr);
   DeterministicKeyChain keyChain = new DeterministicKeyChain(sr, 256);

   //using bitcoinj
   org.bitcoinj.core.Address pvK = kit.wallet().freshAddress(KeyChain.KeyPurpose.AUTHENTICATION);
   //using ECkey
   org.bitcoinj.core.Address ecaddress = ecKey.toAddress(params);

   public NetworkParameters getParams() {
      return params;
   }

   public void initKit() {
      //initialize files and stuff here, add our address to the watched ones
      kit = new WalletAppKit(params, new File("."), fileprefix);
      kit.setAutoSave(true);
      kit.connectToLocalHost();
      kit.useTor();
      kit.startAsync();
      kit.awaitRunning();
      kit.peerGroup().addPeerDiscovery(new DnsDiscovery(params));
   }

   public boolean isValidAddress(String address) {
      org.bitcoinj.core.Address paddress;
      try {
         paddress = new org.bitcoinj.core.Address(params, address);
      } catch (AddressFormatException e) {
         return false;
      }
      return true;
   }

   private KeyPairGenerator getKeyPGen() {
      if (keyPG == null) {
         try {
            keyPG = KeyPairGenerator.getInstance("HMacSP800DRBG");
         } catch (NoSuchAlgorithmException exception) {
            exception.printStackTrace();
         }
         final int keylength = 256;
         keyPG.initialize(keylength, sr);
         return keyPG;
      }
      return keyPG;
   }

   private KeyPair getKeyPair() {
      if (keys == null || keyPG == null) {
         if (keyPG == null) {
            keyPG = getKeyPGen();
         }

         keys = keyPG.generateKeyPair();
         return keys;
      }
      return keys;
   }



    @Override
    public DecryptionKey makeDecryptionKey() {
       ECKey newDecKey = kit.wallet().freshReceiveKey();
       return new DecryptionKeyImpl(newDecKey);
    }

    @Override
    public SigningKey makeSigningKey() {
       ECKey newSignKey = new ECKey(sr);
       kit.wallet().importKey(newSignKey);
       return new SigningKeyImpl(newSignKey);
    }

    @Override
    public int getRandom(int n) throws InvalidImplementationError {
         return sr.nextInt(n);
    }

   /**
    * Phase 1: Key exchange
    * Each participant (except for Alice) creates an key pair of a public key encryption scheme ( makeDecryptionKey() ), consisting of a public encryption key and a private decryption key. We call the public encryption keys ekB, ekC, and ekD. Each participant announces his public encryption key (EncryptionKey), signed with the signature key (SigningKey -> VerificationKey.Address() ) corresponding to his input address.
    * <p>
    * Phase 2: Shuffling
    * Once everybody knows the public encryption key each other, the shuffling can start:
    * <p>
    * Alice encrypts her output address A' with all the encryption keys, in a layered manner. That is, Alice encrypts A' first for Dave, obtaining enc(ekD, A'). Then this ciphertext is encrypted for Charlie, obtaining enc(ekC, enc(ekD, A')) and so on for Dave. This resulting message is sent to Bob:
    * Alice ⟶ Bob: enc(ekB, enc(ekC, enc(ekD, A')))
    * <p>
    * Bob gets the message, decrypts it, obtaining enc(ekC, enc(ekD, A')).
    * He also creates a nested encryption of his address, obtaining enc(ekC, enc(ekD, B')).
    * Now Bob has a list two ciphertexts, containing A' and B'. Bob shuffles this list randomly, i.e., either exchanges the two entries or leave them. Say we are in the case that they are exchanged. Bob sends the shuffled list to Charlie:
    * Bob ⟶ Charlie: enc(ekC, enc(ekD, B')) ; enc(ekC, enc(ekD, A'))
    * <p>
    * Charlie does the same: He decrypts the two entries in the list, adds his own entry and shuffles the list:
    * Charlie ⟶ Dave: enc(ekD, B') ; enc(ekD, C') ; enc(ekD, A')
    * <p>
    * Dave does the same again: He decrypts all entries, obtaining B', C', A'. He adds his own address D' and
    * shuffles the list. The resulting shuffled list is sent to everybody:
    * Dave ⟶ everybody: D', B', C', A'
    **/

    @Override
    public Message hash(Message m) throws CryptographyError, InvalidImplementationError {
       // use KeyCrypter to encrypt message: encrypt(byte[] plainBytes, org.spongycastle.crypto.params.KeyParameter aesKey)

       /**
           * MessageDigest md = MessageDigest.getInstance("SHA-256");
          String message = m.toString();
          md.update(message.getBytes());
        byte[] mHash = md.digest();
        //TODO
        return null;
        **/

       KeyCrypter keyCrypter = kit.wallet().getKeyCrypter();
       SigningKey sk = makeSigningKey();
       VerificationKey vk = sk.VerificationKey();
       Address address = vk.address();
       KeyParameter keyParameter = keyCrypter.deriveKey(sk.toString());
       String mInputString = m.toString();
       byte[] enc = keyCrypter.encrypt(,keyParameter).encryptedBytes;
       String encString = enc.toString();
       return null;
    }


}
