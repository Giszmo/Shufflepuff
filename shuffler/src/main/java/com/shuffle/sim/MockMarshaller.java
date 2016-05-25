/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.sim;

import com.shuffle.p2p.Bytestring;
import com.shuffle.player.Messages;
import com.shuffle.chan.packet.SigningSend;
import com.shuffle.protocol.message.Packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public class MockMarshaller implements SigningSend.Marshaller<Packet> {
    private static final Logger log = LogManager.getLogger(MockMarshaller.class);

    @Override
    public Bytestring marshall(Packet packet) {

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try {
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(packet);
        } catch (IOException e) {
            log.error("Could not marshall packet " + packet + " got error " + e);
            return null;
        }
        return new Bytestring(b.toByteArray());
    }

    @Override
    public Packet unmarshall(Bytestring string) {

        ByteArrayInputStream b = new ByteArrayInputStream(string.bytes);
        Object obj = null;
        try {
            ObjectInputStream o = new ObjectInputStream(b);
            obj = o.readObject();
        } catch (ClassNotFoundException | IOException e) {
            log.error("Could not unmarshall " + string + " got error " + e);
            return null;
        }

        if (!(obj instanceof Messages.Packet)) {
            return null;
        }

        return (Messages.Packet)obj;
    }
}
