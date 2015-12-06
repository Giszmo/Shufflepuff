package com.shuffle.core;

import com.shuffle.form.InvalidImplementationException;

import java.util.Iterator;
import java.util.Queue;

/**
 * Represents a message that has been hashed.
 *
 * Created by Daniel Krawisz on 12/6/15.
 */
public class Hash {
    Queue<MessageElement> hashed;

    public boolean equals(Hash $) throws InvalidImplementationException {
        if (hashed == $.hashed) {
            return true;
        }

        if (hashed.size() != $.hashed.size()) {
            return false;
        }

        Iterator<MessageElement> i1 = hashed.iterator();
        Iterator<MessageElement> i2 = $.hashed.iterator();

        while (i1.hasNext()) {
            if(!i1.next().equals(i2.next())) {
                return false;
            }
        }

        return true;
    }
}
