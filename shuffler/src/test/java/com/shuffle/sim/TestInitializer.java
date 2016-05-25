package com.shuffle.sim;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Inbox;
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
    private class IntMarshaller implements SigningSend.Marshaller<Integer> {

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
        int players = 8;
        Initializer<Integer> initializer = new Initializer<Integer>(
                new MockSessionIdentifier("test initializer " + players),
                new IntMarshaller(),
                2 * (1 + players ));

        Map<SigningKey, Initializer.Connections<Integer>> connections = new HashMap<>();

        for (int player = 1; player <= players; player ++) {
            SigningKey sk = new MockSigningKey(player);

            // Add new player.
            Initializer.Connections<Integer> c = initializer.connect(sk);
            int message = 0;

            // Should be able to receive from and send to every previous player.
            for (Map.Entry<SigningKey, Initializer.Connections<Integer>> prev : connections.entrySet()) {
                Initializer.Connections<Integer> con = prev.getValue();

                con.send.get(sk.VerificationKey()).send(message);
                Inbox.Envelope<VerificationKey, Signed<Integer>> r
                        = c.receive.receive(10, TimeUnit.MILLISECONDS);

                Assert.assertTrue(r.payload.message.equals(message));
                message++;

                c.send.get(prev.getKey().VerificationKey()).send(message);
                Inbox.Envelope<VerificationKey, Signed<Integer>> re =
                        con.receive.receive();

                Assert.assertTrue(re.payload.message.equals(message));
                message++;
            }

            // Add connections to map of previous players.
            connections.put(sk, c);
        }
    }
}
