package com.shuffle.chan.packet;

import com.shuffle.p2p.Bytestring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * The JavaMarshaller uses java's serializable interface to turn objects into byte arrays.
 *
 * Created by Daniel Krawisz on 1/31/16.
 */
public class JavaMarshaller<X extends Serializable> implements
        Marshaller<X> {

    private static final Logger log = LogManager.getLogger(JavaMarshaller.class);

    @Override
    public Bytestring marshall(X x) {

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try {
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(x);
        } catch (IOException e) {
            log.error("Could not marshall packet " + x + " got error " + e);
            return null;
        }
        return new Bytestring(b.toByteArray());
    }

    @Override
    public X unmarshall(Bytestring string) {

        ByteArrayInputStream b = new ByteArrayInputStream(string.bytes);
        Object obj = null;
        try {
            ObjectInputStream o = new ObjectInputStream(b);
            obj = o.readObject();
        } catch (ClassNotFoundException | IOException e) {
            log.error("Could not unmarshall " + string + " got error " + e);
            return null;
        }

        try {
            return (X)obj;
        } catch (ClassCastException e) {
            return null;
        }
    }
}
