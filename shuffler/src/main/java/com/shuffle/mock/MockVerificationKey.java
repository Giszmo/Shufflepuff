/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;

import java.io.Serializable;
import java.util.Arrays;

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

    @Override
    public Bytestring[] verify(Bytestring bytestring) {
        if (bytestring.bytes.length < 4) return null;

        Bytestring[] chopped = bytestring.chop(new int[]{bytestring.bytes.length - 4});

        if (verify(chopped[0], chopped[1])) {
            return chopped;
        }

        return null;
    }

    @Override
    public boolean verify(Bytestring payload, Bytestring signature) {
        return Arrays.equals(signature.bytes, new MockSigningKey(index).sign(payload).bytes);
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
