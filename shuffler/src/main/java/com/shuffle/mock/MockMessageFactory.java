package com.shuffle.mock;

import com.shuffle.mock.MockMessage;
import com.shuffle.protocol.Message;
import com.shuffle.protocol.MessageFactory;

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
