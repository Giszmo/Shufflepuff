package com.shuffle.player;

/**
 * Created by Daniel Krawisz on 4/11/16.
 */
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.message.Packet;

/**
 *
 * Should be comparable according to the lexicographic order of the
 * address corresponding to the keys.
 *
 * Created by Daniel Krawisz on 12/3/15.
 */
public abstract class SigningKey implements com.shuffle.bitcoin.SigningKey {
    public abstract VerificationKey VerificationKey() throws CryptographyError;

    public abstract Signature makeSignature(Transaction t) throws CryptographyError;

    public abstract Signature makeSignature(Packet p) throws CryptographyError;

    public final SignedPacket makeSignedPacket(Packet p) throws CryptographyError {
        return new SignedPacket(p, makeSignature(p));
    }
}
