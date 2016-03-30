/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Packet;

import java.io.IOException;
import java.io.Serializable;

/**
 * A mock implementation of a VerificationKey.
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
public class MockVerificationKey implements VerificationKey, Serializable {
    public final int index;

    public MockVerificationKey(int index) {
        this.index = index;
    }

    // These functions are not implemented yet.
    @Override
    public boolean verify(Transaction t, Signature sig) throws InvalidImplementationError {
        if (!(sig instanceof MockSignature)) {
            throw new InvalidImplementationError();
        }

        MockSignature mock = (MockSignature)sig;

        return mock.t != null && mock.t.equals(t) && mock.key.equals(this);
    }

    @Override
    public boolean verify(Packet packet, Signature sig) {
        if (!(sig instanceof MockSignature)) {
            throw new InvalidImplementationError();
        }

        MockSignature mock = (MockSignature)sig;

        return mock.packet != null && mock.packet.equals(packet) && mock.key.equals(this);
    }

    @Override
    public boolean equals(Object vk) {
        return vk != null
                && vk instanceof MockVerificationKey && index == ((MockVerificationKey) vk).index;

    }

    @Override
    public Address address() {
        return new MockAddress(index);
    }

    public String toString() {
        return "vk[" + index + "]";
    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof MockVerificationKey)) {
            return -1;
        }

        MockVerificationKey key = ((MockVerificationKey)o);

        if (index == key.index) {
            return 0;
        }

        if (index < key.index) {
            return -1;
        }

        return 1;
    }
}
