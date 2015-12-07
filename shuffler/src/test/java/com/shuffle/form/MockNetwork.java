package com.shuffle.form;

import java.util.Map;
import java.util.Set;

/**
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockNetwork implements Network {

    @Override
    public void sendTo(VerificationKey to, Packet packet) {

    }

    @Override
    public Packet receive() {
        return null;
    }

}
