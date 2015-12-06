package com.shuffle.form;

/**
 * Created by Daniel Krawisz on 12/4/15.
 */
public interface MessageFactory {
    Message make(); // Make an empty message.
    Message make(EncryptionKey ek);
    Message make(VerificationKey vk);
    Message make(CoinSignature s);
    // Remake message with updated information as to the phase and sender of a message.
    Message remake(Message m) throws InvalidImplementationException;
    void register(SessionIdentifier τ, SigningKey sk, ShuffleMachine m);
}
