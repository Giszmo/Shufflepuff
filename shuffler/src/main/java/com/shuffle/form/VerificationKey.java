package com.shuffle.form;


/**
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface VerificationKey {
    boolean verify(CoinTransaction t, CoinSignature sig) throws InvalidImplementationException;
    boolean equals(Object vk);

    // Attempts to read the signature on a packet. If it can verify the signature, the packet
    // is stripped of the signature. If not, then the packet is unchanged.
    boolean readSignature(Packet packet) throws CryptographyException, FormatException, InvalidImplementationException;
}
