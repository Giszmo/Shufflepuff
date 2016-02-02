package com.shuffle.mock;

import com.shuffle.p2p.Bytestring;
import com.shuffle.player.Marshaller;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.SignedPacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * The MockMarshaller uses java's serializable interface to turn objects into byte arrays.
 *
 * Created by Daniel Krawisz on 1/31/16.
 */
public class MockMarshaller implements Marshaller<Bytestring> {

    public static Bytestring marshall(Object object) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(object);
        return new Bytestring(b.toByteArray());
    }

    @Override
    public Bytestring marshall(SignedPacket packet) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try {
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(packet);
        } catch (IOException e) {
            return null;
        }
        return new Bytestring(b.toByteArray());
    }

    @Override
    public SignedPacket unmarshall(Bytestring string) throws FormatException {
        ByteArrayInputStream b = new ByteArrayInputStream(string.bytes);
        Object obj = null;
        try {
            ObjectInputStream o = new ObjectInputStream(b);
            obj = o.readObject();
        } catch (ClassNotFoundException | IOException e) {
            throw new FormatException();
        }

        if (!(obj instanceof SignedPacket)) {
            throw new FormatException();
        }

        return (SignedPacket)obj;
    }
}
