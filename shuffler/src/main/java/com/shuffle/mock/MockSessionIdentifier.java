/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.protocol.SessionIdentifier;

import java.io.Serializable;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public class MockSessionIdentifier implements SessionIdentifier, Serializable {
    static final String version = "CoinShuffle mock implementation for testing.";
    final public String id;

    public MockSessionIdentifier(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof MockSessionIdentifier)) {
            return false;
        }

        MockSessionIdentifier s = (MockSessionIdentifier) o;

        return this == s || id.equals(s.id);
    }

    @Override
    public String toString() {
        return "session[" + id + "]";
    }

    @Override
    public String protocol() {
        return "test protocol";
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public String id() {
        return id;
    }
}
