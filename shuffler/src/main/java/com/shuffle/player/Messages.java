/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.player;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.Signature;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.HistoryReceiveChan;
import com.shuffle.chan.ReceiveChan;
import com.shuffle.chan.SendChan;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.blame.Blame;
import com.shuffle.protocol.message.MessageFactory;
import com.shuffle.protocol.message.Phase;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * All Message handling for CoinShuffle in one convenient package!
 *
 * Created by Daniel Krawisz on 12/9/15.
 */
public class Messages implements MessageFactory {

    final Map<VerificationKey, SendChan<com.shuffle.protocol.message.Packet>> net;
    private final ReceiveChan<SignedPacket> receive;
    final SessionIdentifier session;
    final VerificationKey me;
    final List<Packet> sent = new LinkedList<>();

    public Messages(SessionIdentifier session,
                    VerificationKey me,
                    Map<VerificationKey, SendChan<com.shuffle.protocol.message.Packet>> net,
                    ReceiveChan<SignedPacket> receive) {

        this.net = net;
        this.session = session;
        this.me = me;
        this.receive = new HistoryReceiveChan<>(receive);

    }

    @Override
    public com.shuffle.protocol.message.Message make() {
        return new Message(session, me);
    }

    @Override
    public com.shuffle.protocol.message.Packet receive() throws InterruptedException, IOException {
        // TODO make this a parameter.
        return receive.receive(1000, TimeUnit.MILLISECONDS);
    }

    public static class SecureHash implements Serializable {
        public final Atom hashed;

        public SecureHash(Atom toHash) {
            hashed = toHash;
        }

        public SecureHash(com.shuffle.protocol.message.Message message) {
            if (!(message instanceof Message)) {
                throw new InvalidImplementationError();
            }

            hashed = ((Message)message).atoms;
        }

        public String toString() {
            return "hashed[" + hashed + "]";
        }

        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (!(o instanceof SecureHash)) {
                return false;
            }

            SecureHash mockHashed = (SecureHash)o;

            return hashed.equals(mockHashed.hashed);
        }
    }

    private static class Atom implements Serializable {
        public final Address addr;
        public final EncryptionKey ek;
        public final Signature sig;
        public final SecureHash secureHash;
        public final Blame blame;

        public final Transaction t;
        // Sometimes, we have to send whole packets that we previously received.
        public final com.shuffle.protocol.message.Packet packet;

        public final Atom next;

        private Atom(
                Address addr,
                EncryptionKey ek,
                Signature sig,
                SecureHash secureHash,
                Blame blame,
                Transaction t,
                com.shuffle.protocol.message.Packet packet,
                Atom next
        ) {
            // Enforce the correct format.
            format : {
                if (addr != null) {
                    if (ek != null || sig != null || secureHash != null || blame != null || t != null) {
                        throw new IllegalArgumentException();
                    }
                    break format;
                }

                if (ek != null) {
                    if (sig != null || secureHash != null || blame != null || t != null) {
                        throw new IllegalArgumentException();
                    }
                    break format;
                }

                if (sig != null) {
                    if (secureHash != null || blame != null || t != null) {
                        throw new IllegalArgumentException();
                    }
                    break format;
                }

                if (secureHash != null) {
                    if (blame != null || t != null) {
                        throw new IllegalArgumentException();
                    }
                    break format;
                }

                if (blame != null) {
                    if (t != null) {
                        throw new IllegalArgumentException();
                    }
                    break format;
                }

                if (t != null) {
                    break format;
                }

                throw new IllegalArgumentException();
            }

            this.addr = addr;
            this.ek = ek;
            this.sig = sig;
            this.secureHash = secureHash;
            this.blame = blame;
            this.t = t;
            this.packet = packet;
            this.next = next;
        }

        public static Atom make(Object o, Atom next) {
            if (o instanceof Address) {
                return new Atom((Address)o, null, null, null, null, null, null, next);
            }
            if (o instanceof EncryptionKey) {
                return new Atom(null, (EncryptionKey)o, null, null, null, null, null, next);
            }
            if (o instanceof Signature) {
                return new Atom(null, null, (Signature)o, null, null, null, null, next);
            }
            if (o instanceof SecureHash) {
                return new Atom(null, null, null, (Messages.SecureHash)o, null, null, null, next);
            }
            if (o instanceof Blame) {
                return new Atom(null, null, null, null, (Blame)o, null, null, next);
            }
            if (o instanceof Transaction) {
                return new Atom(null, null, null, null, null, (Transaction)o, null, next);
            }
            if (o instanceof com.shuffle.protocol.message.Packet) {
                return new Atom(null, null, null, null, null, null, (com.shuffle.protocol.message.Packet)o, next);
            }

            throw new IllegalArgumentException();
        }

        private static Atom make(Object o) {
            return make(o, null);
        }

        private static Atom attach(Atom a, Atom o) {
            if (a == null) {
                return o;
            }

            return new Atom(a.addr, a.ek, a.sig, a.secureHash, a.blame, a.t, a.packet, attach(a.next, o));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (!(o instanceof Atom)) {
                return false;
            }

            Atom a = (Atom)o;

            return this == a || a.sig == sig
                    && (a.ek == null && ek == null || ek != null && ek.equals(a.ek))
                    && (a.addr == null && addr == null || addr != null && addr.equals(a.addr))
                    && (a.t == null && t == null || t != null && t.equals(a.t))
                    && (a.packet == null && packet == null
                    || packet != null && packet.equals(a.packet))
                    && (a.blame == null && blame == null || blame != null && blame.equals(a.blame))
                    && (a.secureHash == null && secureHash == null || secureHash != null && secureHash.equals(a.secureHash))
                    && (a.next == null && next == null || next != null && next.equals(a.next));
        }

        @Override
        public int hashCode() {
            int hash = addr == null ? 0 : addr.hashCode();
            hash = hash * 15 + (ek == null ? 0 : ek.hashCode());
            hash = hash * 15 + (sig == null ? 0 : sig.hashCode());
            hash = hash * 15 + (this.secureHash == null ? 0 : this.secureHash.hashCode());
            hash = hash * 15 + (blame == null ? 0 : blame.hashCode());
            hash = hash * 15 + (next == null ? 0 : next.hashCode());
            return hash;
        }

        @Override
        public String toString() {
            String str = "";

            if (addr != null) str += addr.toString();

            if (ek != null) str += ek.toString();

            if (sig != null) str += sig.toString();

            if (secureHash != null) str += secureHash.toString();

            if (t != null) str += t.toString();

            if (packet != null) str += packet.toString();

            if (blame != null) str += blame.toString();

            if (next != null) str += "⊕" + next.toString();

            return str;
        }
    }

    class Packet implements com.shuffle.protocol.message.Packet, Serializable {

        public final Message message;
        public final Phase phase;
        public final VerificationKey to;

        Packet(
                Message message,
                Phase phase,
                VerificationKey to
        ) {

            this.to = to;
            this.phase = phase;
            this.message = message;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (!(o instanceof Packet)) {
                return false;
            }

            Packet packet = ((Packet)o);

            return phase == packet.phase && to.equals(packet.to)
                    && message.from.equals(packet.message.from)
                    && message.equals(packet.message);
        }

        @Override
        public int hashCode() {
            int hash = message.hashCode();
            hash = hash * 15 + to.hashCode();
            hash = hash * 15 + message.from.hashCode();
            return hash;
        }

        @Override
        public String toString() {
            return "{" + message.toString() + ", " + phase.toString() + ", "
                    + to.toString() + ", " + message.from + "}";
        }

        @Override
        public Message payload() {
            return message;
        }

        @Override
        public Phase phase() {
            return phase;
        }

        @Override
        public VerificationKey from() {
            return message.from;
        }

        @Override
        public VerificationKey to() {
            return to;
        }

        @Override
        public void send() throws InterruptedException {

            SendChan<com.shuffle.protocol.message.Packet> chan = net.get(to);

            if (chan == null) return;

            if (chan.send(this)) {
                sent.add(this);
            }
        }
    }

    /**
     * Represents a packet that has been digitally signed.
     *
     * Created by Daniel Krawisz on 1/22/16.
     */
    public static class SignedPacket implements com.shuffle.protocol.message.Packet, Serializable {

        public final com.shuffle.protocol.message.Packet packet;
        public final Signature signature;

        public SignedPacket(com.shuffle.protocol.message.Packet packet, Signature signature) {
            if (packet == null || signature == null) {
                throw new NullPointerException();
            }
            this.packet = packet;
            this.signature = signature;
        }

        @Override
        public String toString() {
            return packet.toString() + "[" + signature.toString() + "]";
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

            return packet.equals(sp.packet) && signature.equals(sp.signature);
        }

        @Override
        public int hashCode() {
            return packet.hashCode() + signature.hashCode();
        }


        @Override
        public com.shuffle.protocol.message.Message payload() {
            return packet.payload();
        }

        @Override
        public Phase phase() {
            return packet.phase();
        }

        @Override
        public VerificationKey from() {
            return packet.from();
        }

        @Override
        public VerificationKey to() {
            return packet.to();
        }

        @Override
        public void send() throws InterruptedException {
            // Message cannot be sent because it is not ours.
        }
    }

    public class Message implements com.shuffle.protocol.message.Message, Serializable {

        public final SessionIdentifier session;
        public final Atom atoms;
        public final VerificationKey from;

        public Message(
                SessionIdentifier session,
                VerificationKey from) {

            if (from == null || session == null) throw new NullPointerException();

            atoms = null;
            this.session = session;
            this.from = from;
        }

        private Message(
                SessionIdentifier session,
                VerificationKey from,
                Atom atom) {

            if (session == null || from == null) throw new NullPointerException();

            this.from = from;
            atoms = atom;
            this.session = session;
        }

        public Message(SessionIdentifier session,
                       VerificationKey from,
                       Deque atoms) {

            if (session == null || from == null || atoms == null) throw new NullPointerException();

            Atom atom = null;

            Iterator i = atoms.descendingIterator();

            while (i.hasNext()) {
                atom = Atom.make(i.next(), atom);
            }

            this.atoms = atom;
            this.session = session;
            this.from = from;
        }

        @Override
        public boolean isEmpty() {
            return atoms == null;
        }

        public com.shuffle.protocol.message.Message attachAddrs(Deque<Address> addrs) {
            if (addrs == null) throw new NullPointerException();

            Message m = new Message(session, from, addrs);

            return new Message(session, from, Messages.Atom.attach(atoms, m.atoms));
        }

        @Override
        public com.shuffle.protocol.message.Message attach(EncryptionKey ek) {
            if (ek == null) throw new NullPointerException();

            return new Message(session, from, Messages.Atom.attach(atoms, Messages.Atom.make(ek)));
        }

        @Override
        public com.shuffle.protocol.message.Message attach(Address addr) {
            if (addr == null) throw new NullPointerException();

            return new Message(session, from, Messages.Atom.attach(atoms, Messages.Atom.make(addr)));
        }

        @Override
        public com.shuffle.protocol.message.Message attach(Signature sig) {
            if (sig == null) throw new NullPointerException();

            return new Message(session, from, Messages.Atom.attach(atoms, Messages.Atom.make(sig)));
        }

        @Override
        public com.shuffle.protocol.message.Message attach(Blame blame) {
            if (blame == null) throw new NullPointerException();

            return new Message(session, from, Messages.Atom.attach(atoms, Messages.Atom.make(blame)));
        }

        public com.shuffle.protocol.message.Message hashed() {

            return new Message(session, from, Messages.Atom.make(new Messages.SecureHash(this)));
        }

        @Override
        public EncryptionKey readEncryptionKey() throws FormatException {
            if (atoms == null || atoms.ek == null) throw new FormatException();

            return atoms.ek;
        }

        @Override
        public Address readAddress() throws FormatException {
            if (atoms == null || atoms.addr == null) throw new FormatException();

            return atoms.addr;
        }

        @Override
        public Blame readBlame() throws FormatException {
            if (atoms == null || atoms.blame == null) throw new FormatException();

            return atoms.blame;
        }

        @Override
        public Signature readSignature() throws FormatException {
            if (atoms == null || atoms.sig == null) throw new FormatException();

            return atoms.sig;
        }

        @Override
        public com.shuffle.protocol.message.Message rest() throws FormatException {

            if (atoms == null) throw new FormatException();

            return new Message(session, from, atoms.next);
        }

        @Override
        public com.shuffle.protocol.message.Packet prepare(Phase phase, VerificationKey to) {
            return new Packet(this, phase, to);
        }

        @Override
        public boolean equals(Object o) {

            if (o == null) return false;

            if (!(o instanceof Message)) return false;

            Message mock = (Message)o;

            return session.equals(mock.session)
                    && ((atoms == null && mock.atoms == null)
                        || (atoms != null && atoms.equals(mock.atoms)));
        }

        @Override
        public int hashCode() {
            int hash = 0;
            hash = hash * 15 + session.hashCode();
            hash = hash * 15 + atoms.hashCode();
            return hash;
        }

        @Override
        public String toString() {
            if (atoms == null) return "[]";

            return atoms.toString();
        }
    }
}
