package com.shuffle.protocol;

/**
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MockMessageFactory implements MessageFactory {

    public MockMessageFactory() {}

    @Override
    public Message make() {
        return new MockMessage();
    }

    @Override
    public Message copy(Message message) throws InvalidImplementationError {
        if (!(message instanceof MockMessage)) {
            throw new InvalidImplementationError();
        }

        MockMessage mock = (MockMessage)message;

        return mock.copy();
    }
}
