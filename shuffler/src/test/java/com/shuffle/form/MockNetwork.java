package com.shuffle.form;

import java.util.Map;
import java.util.Set;

/**
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockNetwork implements Network {
    @Override
    public void broadcast(Message σ) throws TimeoutException {

    }

    @Override
    public void sendTo(VerificationKey i, Message σ) throws TimeoutException {

    }

    @Override
    public Message receive() throws TimeoutException, ProtocolAbortedException {
        return null;
    }

    @Override
    public Map<VerificationKey, Message> receive(Set<VerificationKey> from) throws TimeoutException, ProtocolAbortedException {
        return null;
    }

    @Override
    public void keys(Set<VerificationKey> keys) {

    }
}
