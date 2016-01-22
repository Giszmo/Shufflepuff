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
}
