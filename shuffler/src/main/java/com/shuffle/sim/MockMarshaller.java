/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.sim;

import com.shuffle.p2p.Bytestring;
import com.shuffle.player.Marshaller;
import com.shuffle.player.Messages;
import com.shuffle.protocol.message.Packet;

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
public class MockMarshaller implements Marshaller {

    @Override
    public Bytestring marshallAndSign(Packet packet) {


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
    public Messages.SignedPacket unmarshall(Bytestring string) {
        ByteArrayInputStream b = new ByteArrayInputStream(string.bytes);
        Object obj = null;
        try {
            ObjectInputStream o = new ObjectInputStream(b);
            obj = o.readObject();
        } catch (ClassNotFoundException | IOException e) {
            return null;
        }

        if (!(obj instanceof Messages.SignedPacket)) {
            return null;
        }

        return (Messages.SignedPacket)obj;
    }
}
