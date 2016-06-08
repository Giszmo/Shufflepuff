package com.shuffle.bitcoin;

import com.shuffle.bitcoin.impl.DecryptionKeyImpl;
import com.shuffle.bitcoin.impl.SigningKeyImpl;
import com.shuffle.protocol.InvalidImplementationError;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.HDUtils;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.KeyChain;

import java.io.File;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;


public class BitcoinCrypto implements Crypto {

   // Figure out which network we should connect to. Each one gets its own set of files.
   NetworkParameters params = TestNet3Params.get();
   String fileprefix = "_shuffle";
   WalletAppKit kit = null;


   //Alphabet defining valid characters used in address
   private final static String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";


   //Generate Keypair using HMAC
   KeyPairGenerator keyPG = getKeyPGen();
   KeyPair keys = getKeyPair();
   KeyPair keyPair = keyPG.generateKeyPair();
   PrivateKey privKey = keyPair.getPrivate();
   PublicKey pubKey = keyPair.getPublic();

   //DeterministicSeed seed = kit.wallet().getKeyChainSeed();
   // if we generate new keys no need for mnemonic, apparently we don't
   //List<String> mnemonicCode = seed.getMnemonicCode();
   SecureRandom sr = new SecureRandom();

   public NetworkParameters getParams() {
      return params;
   }

   // create derivation path for shuffle keys
   HDUtils hdUtils = new HDUtils();
   String path = HDUtils.formatPath(HDUtils.parsePath("5H/"));
   int decKeyCounter = 0;

   public void initKit() {
      //initialize files and stuff here, add our address to the watched ones
      kit.setAutoSave(true);
      kit.connectToLocalHost();
      // kit.useTor();
      kit.startAsync();
      kit.awaitRunning();
      kit.peerGroup().addPeerDiscovery(new DnsDiscovery(params));
   }

   //Validate addresses function
   public static boolean ValidateBitcoinAddress(String addr) {
      if (addr.length() < 26 || addr.length() > 35) return false;
      byte[] decoded = DecodeBase58(addr, 58, 25);
      if (decoded == null) return false;

      byte[] hash = Sha256(decoded, 0, 21, 2);

      return Arrays.equals(Arrays.copyOfRange(hash, 0, 4), Arrays.copyOfRange(decoded, 21, 25));
   }

   private static byte[] DecodeBase58(String input, int base, int len) {
      byte[] output = new byte[len];
      for (int i = 0; i < input.length(); i++) {
         char t = input.charAt(i);

         int p = ALPHABET.indexOf(t);
         if (p == -1) return null;
         for (int j = len - 1; j > 0; j--, p /= 256) {
            p += base * (output[j] & 0xFF);
            output[j] = (byte) (p % 256);
         }
         if (p != 0) return null;
      }

      return output;
   }

   private static byte[] Sha256(byte[] data, int start, int len, int recursion) {
      if (recursion == 0) return data;

      try {
         MessageDigest md = MessageDigest.getInstance("SHA-256");
         md.update(Arrays.copyOfRange(data, start, start + len));
         return Sha256(md.digest(), 0, 32, recursion - 1);
      } catch (NoSuchAlgorithmException e) {
         return null;
      }
   }



   public boolean isValidAddress(String address) {
      try {
         new Address(params, address);
         return true;
      } catch (AddressFormatException e) {
         return false;
      }
   }

   public static PrivateKey loadPrivateKey(String key64) throws GeneralSecurityException {
      byte[] clear = Base64.getDecoder().decode(key64);
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
      KeyFactory fact = KeyFactory.getInstance("EC");
      PrivateKey priv = fact.generatePrivate(keySpec);
      Arrays.fill(clear, (byte) 0);
      return priv;
   }


   public static PublicKey loadPublicKey(String stored) throws GeneralSecurityException {
      byte[] data = Base64.getDecoder().decode(stored);
      X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
      KeyFactory fact = KeyFactory.getInstance("EC");
      return fact.generatePublic(spec);
   }

   public static String savePrivateKey(PrivateKey priv) throws GeneralSecurityException {
      KeyFactory fact = KeyFactory.getInstance("EC");
      PKCS8EncodedKeySpec spec = fact.getKeySpec(priv,
            PKCS8EncodedKeySpec.class);
      byte[] packed = spec.getEncoded();
      //todo check
      String key64 = Base64.getEncoder().encodeToString(packed);

      Arrays.fill(packed, (byte) 0);
      return key64;
   }


   public static String savePublicKey(PublicKey publ) throws GeneralSecurityException {
      KeyFactory fact = KeyFactory.getInstance("DSA");
      X509EncodedKeySpec spec = fact.getKeySpec(publ,
            X509EncodedKeySpec.class);
      return Base64.getEncoder().encodeToString(spec.getEncoded());
   }



   private KeyPairGenerator getKeyPGen() {
      if (keyPG == null) {
         try {
            keyPG = KeyPairGenerator.getInstance("EC");
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

   public int getDecKeyCounter() {
      return decKeyCounter;
   }

   public Wallet getWallet() {
      if (kit == null) {
         kit = new WalletAppKit(params, new File("."), fileprefix);
         initKit();
      }
      return kit.wallet();
   }

   public String getCurrentPathAsString() {
      System.out.println("Value of path variable: " + path);
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(path);
      stringBuilder.append("/" + getDecKeyCounter());
      String fpath = stringBuilder.toString();
      return fpath;
   }

    @Override
    public DecryptionKey makeDecryptionKey() {
       String ppath = getCurrentPathAsString();
       System.out.println("Current path used by decryption key genereated: " + ppath);
       ECKey newDecKey = getWallet().getKeyByPath(HDUtils.parsePath(ppath));
       decKeyCounter++;
       return new DecryptionKeyImpl(newDecKey);
    }

    @Override
    public SigningKey makeSigningKey() {
       ECKey newSignKey = kit.wallet().freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
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



}
