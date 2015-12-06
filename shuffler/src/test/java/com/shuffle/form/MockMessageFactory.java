package com.shuffle.form;

import com.shuffle.form.MessageFactory;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public class MockMessageFactory implements MessageFactory {
    @Override
    public Message make() {
        return new MockMessage(new int[]{});
    }

    @Override
    public Message make(EncryptionKey ek) {
        return null;
    }

    @Override
    public Message make(VerificationKey vk) {
        return null;
    }

    @Override
    public Message make(CoinSignature s) {
        return null;
    }

    @Override
    public Message remake(Message m) throws InvalidImplementationException {
        return null;
    }

    @Override
    public void register(SessionIdentifier τ, SigningKey sk, ShuffleMachine m) {

    }
}
