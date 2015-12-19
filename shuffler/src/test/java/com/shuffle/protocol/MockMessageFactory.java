package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MockMessageFactory implements MessageFactory {

    public MockMessageFactory() {}

    @Override
    public Message make(SessionIdentifier τ, ShufflePhase phase, SigningKey sk) {
        return new MockMessage(τ, phase, sk);
    }

    @Override
    public Message copy(Message message) throws InvalidImplementationException {
        if (!(message instanceof MockMessage)) {
            throw new InvalidImplementationException();
        }

        MockMessage mock = (MockMessage)message;

        return new MockMessage(mock.τ, mock.phase, mock.signer).attach(mock.atoms);
    }
}
