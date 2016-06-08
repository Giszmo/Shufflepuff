package com.shuffle.sim;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Inbox;
import com.shuffle.chan.packet.Marshaller;
import com.shuffle.chan.packet.Signed;
import com.shuffle.chan.packet.SigningSend;
import com.shuffle.mock.MockSessionIdentifier;
import com.shuffle.mock.MockSigningKey;
import com.shuffle.p2p.Bytestring;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Daniel Krawisz on 5/21/16.
 */
public class TestInitializer {
    private class IntMarshaller implements Marshaller<Integer> {

        @Override
        public Bytestring marshall(Integer integer) {
            return new Bytestring(ByteBuffer.allocate(4).putInt(integer).array());
        }

        @Override
        public Integer unmarshall(Bytestring string) {
            if (string.bytes.length != 4) return null;

            int i = 0;
            for (byte b : string.bytes) {
                i = (i << 8) + b;
            }

            return i;
        }
    }

    @Test
    public void testInitializer() throws InterruptedException {
        int players = 88;
        IntMarshaller im = new IntMarshaller();

        Initializer<Integer> initializer = new Initializer<Integer>(
                new MockSessionIdentifier("test initializer " + players), 2 * (1 + players ));

        Map<SigningKey, Initializer.Connections<Integer>> connections = new HashMap<>();

        for (int player = 1; player <= players; player ++) {
            SigningKey alice = new MockSigningKey(player);

            // Add new player.
            Initializer.Connections<Integer> c = initializer.connect(alice);
            int message = 0;

            // Should be able to receive from and send to every previous player.
            for (Map.Entry<SigningKey, Initializer.Connections<Integer>> prev : connections.entrySet()) {
                Initializer.Connections<Integer> con = prev.getValue();
                SigningKey bob = prev.getKey();

                // Send a message from bob to alice.
                new SigningSend<>(con.send.get(alice.VerificationKey()), im, bob).send(message);
                Inbox.Envelope<VerificationKey, Signed<Integer>> r
                        = c.receive.receive(10, TimeUnit.MILLISECONDS);

                Assert.assertTrue(r.payload.message.equals(message));
                message++;

                // Send a message from alice to bob.
                new SigningSend<>(c.send.get(bob.VerificationKey()), im, alice).send(message);
                Inbox.Envelope<VerificationKey, Signed<Integer>> re =
                        con.receive.receive();

                Assert.assertTrue(re.payload.message.equals(message));
                message++;
            }

            // Add connections to map of previous players.
            connections.put(alice, c);
        }
    }
}
