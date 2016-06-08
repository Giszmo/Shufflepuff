/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.player;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.HistoryReceive;
import com.shuffle.chan.HistorySend;
import com.shuffle.chan.IgnoreSend;
import com.shuffle.chan.Receive;
import com.shuffle.chan.Send;
import com.shuffle.chan.packet.JavaMarshaller;
import com.shuffle.chan.packet.OutgoingPacketSend;
import com.shuffle.chan.packet.Packet;
import com.shuffle.chan.packet.Signed;
import com.shuffle.chan.packet.SessionIdentifier;
import com.shuffle.chan.packet.SigningSend;
import com.shuffle.p2p.Bytestring;
import com.shuffle.chan.Inbox;
import com.shuffle.protocol.message.MessageFactory;
import com.shuffle.protocol.message.Phase;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * All Message handling for CoinShuffle in one convenient package!
 *
 * Created by Daniel Krawisz on 12/9/15.
 */
public class Messages implements MessageFactory {

    /**
     * Represents a packet that has been digitally signed.
     *
     * Created by Daniel Krawisz on 1/22/16.
     */
    public static class SignedPacket implements com.shuffle.protocol.message.Packet, Serializable {

        public final Signed<com.shuffle.chan.packet.Packet<VerificationKey, P>> packet;

        public SignedPacket(
                Signed<com.shuffle.chan.packet.Packet<VerificationKey, P>> packet) {

            if (packet == null) {
                throw new NullPointerException();
            }

            this.packet = packet;
        }

        @Override
        public String toString() {
            return packet.toString() + "[" + packet.signature.toString() + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (!(o instanceof SignedPacket)) {
                return false;
            }

            SignedPacket sp = (SignedPacket)o;

            return packet.equals(sp.packet) && packet.signature.equals(sp.packet.signature);
        }

        @Override
        public int hashCode() {
            return 17 * packet.hashCode();
        }


        @Override
        public com.shuffle.protocol.message.Message payload() {
            return packet.message.payload.message;
        }

        @Override
        public Phase phase() {
            return packet.message.payload.phase;
        }

        @Override
        public VerificationKey from() {
            return packet.message.from;
        }

        @Override
        public VerificationKey to() {
            return packet.message.to;
        }

        @Override
        public Bytestring signature() {
            return packet.signature;
        }
    }

    private class Outgoing {
        public final Send<P> out;
        private final HistorySend<Signed<Packet<VerificationKey, P>>> h;

        Outgoing(Send<P> out, HistorySend<Signed<Packet<VerificationKey, P>>> h, VerificationKey k) {

            if (k == null || out == null || h == null) throw new NullPointerException();

            this.out = out;
            this.h = h;
        }

        public List<Signed<Packet<VerificationKey, P>>> history() {
            return h.history();
        }

        public Signed<com.shuffle.chan.packet.Packet<VerificationKey, P>> last() {
            return h.last();
        }
    }

    // Outgoing channels.
    final Map<VerificationKey, Outgoing> net = new HashMap<>();

    // Where new messages come in. All messages are logged and have their signature
    // checked as they come in.
    private final Receive<Inbox.Envelope<VerificationKey,
            Signed<com.shuffle.chan.packet.Packet<VerificationKey, P>>>> receive;

    final SessionIdentifier session;
    final SigningKey me;

    public Messages(SessionIdentifier session,
                    SigningKey me,
                    Map<VerificationKey,
                            Send<Signed<Packet<VerificationKey, P>>>> net,
                    Receive<Inbox.Envelope<VerificationKey,
                            Signed<Packet<VerificationKey, P>>>> receive) {

        if (session == null || me == null || net == null || receive == null)
            throw new NullPointerException();

        this.session = session;
        this.me = me;
        this.receive = new HistoryReceive<>(receive);

        JavaMarshaller<Packet<VerificationKey, P>> jj = new JavaMarshaller<>();
        VerificationKey vk = me.VerificationKey();

        for (Map.Entry<VerificationKey, Send<Signed<Packet<VerificationKey, P>>>> z : net.entrySet()) {

            VerificationKey k = z.getKey();
            if (vk.equals(k)) continue;

            HistorySend<Signed<Packet<VerificationKey, P>>> h = new HistorySend<>(z.getValue());
            Send<Packet<VerificationKey, P>> signer = new SigningSend<>(h, jj, me);
            Send<P> p = new OutgoingPacketSend<>(signer, session, vk, k);

            this.net.put(k, new Outgoing(p, h, vk));
        }

        // We have a special channel for sending messages to ourselves.
        HistorySend<Signed<Packet<VerificationKey, P>>> h = new HistorySend<>(
                new IgnoreSend<Signed<Packet<VerificationKey, P>>>());

        Send<P> p = new OutgoingPacketSend<>(new SigningSend<>(h, jj, me), session, vk, vk);
        this.net.put(vk, new Outgoing(p, h, vk));
    }

    @Override
    public com.shuffle.protocol.message.Message make() {
        return new Message(session, me.VerificationKey(), this);
    }

    @Override
    public com.shuffle.protocol.message.Packet receive() throws InterruptedException, IOException {

        // TODO make this a parameter.
        Inbox.Envelope<VerificationKey,
                Signed<com.shuffle.chan.packet.Packet<VerificationKey, P>>> e
                = receive.receive(1000, TimeUnit.MILLISECONDS);

        if (e == null) return null;

        return new SignedPacket(e.payload);
    }

    public VerificationKey identity() {
        return me.VerificationKey();
    }

    // Some cleanup after we're done.
    public void close() throws InterruptedException {
        for (Outgoing p : net.values()) {
            p.out.close();
        }

        net.clear();
    }

    public SignedPacket send(Message m, Phase phase, VerificationKey to) throws InterruptedException {

        Outgoing x = m.messages.net.get(to);

        if (x == null) return null;

        // About to send message.
        if (!x.out.send(new P(m, phase))) {
            return null;
        }

        return new SignedPacket(x.last());
    }
}
