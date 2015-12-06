package com.shuffle.core;

import com.shuffle.form.*;
import com.shuffle.form.Message;

import java.util.Map;
import java.util.Set;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public class ShuffleNetwork implements Network {
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
    public void register(SessionIdentifier τ, Set<VerificationKey> keys) {

    }
}
