/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.p2p.Bytestring;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Daniel Krawisz on 12/9/15.
 */
public class MockSigningKey implements SigningKey {
    final int index;

    public MockSigningKey(int index) {
        this.index = index;
    }

    @Override
    public VerificationKey VerificationKey() {
        return new MockVerificationKey(index);
    }

    @Override
    public Bytestring sign(Bytestring string) {
        return new Bytestring(ByteBuffer.allocate(4).putInt(string.hashCode() + index).array());
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof MockSigningKey && index == ((MockSigningKey) o).index;

    }

    @Override
    public String toString() {
        return "sk[" + index + "]";
    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof MockSigningKey)) {
            return -1;
        }

        MockSigningKey key = ((MockSigningKey)o);

        if (index == key.index) {
            return 0;
        }

        if (index < key.index) {
            return -1;
        }

        return 1;
    }
}
