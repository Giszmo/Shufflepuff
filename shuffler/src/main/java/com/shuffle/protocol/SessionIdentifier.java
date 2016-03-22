/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import java.io.Serializable;

/**
 * The session identifier is a unique string given at the outset.
 *
 * Created by Daniel Krawisz on 12/3/15.
 */
public interface SessionIdentifier extends Serializable {
    String protocol();
    String version();
    String id();
}
