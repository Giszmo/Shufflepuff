/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.player;

import com.shuffle.bitcoin.*;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Network;
import com.shuffle.protocol.message.Packet;
import com.shuffle.protocol.blame.Blame;
import com.shuffle.protocol.message.Phase;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.Serializable;
import java.util.Deque;
import java.util.Iterator;

/**
 */
public class Message implements com.shuffle.protocol.message.Message, Serializable {
    private static final transient Logger log = LogManager.getLogger(Message.class);

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
        public final Packet packet;

        public final Atom next;

        private Atom(
                Address addr,
                EncryptionKey ek,
                Signature sig,
                SecureHash secureHash,
                Blame blame,
                Transaction t,
                Packet packet,
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
                return new Atom(null, null, null, (SecureHash)o, null, null, null, next);
            }
            if (o instanceof Blame) {
                return new Atom(null, null, null, null, (Blame)o, null, null, next);
            }
            if (o instanceof Transaction) {
                return new Atom(null, null, null, null, null, (Transaction)o, null, next);
            }
            if (o instanceof Packet) {
                return new Atom(null, null, null, null, null, null, (Packet)o, next);
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

    public final SessionIdentifier session;
    public final Atom atoms;
    public final VerificationKey from;
    public final transient Network net;

    public Message(SessionIdentifier session, VerificationKey from, Network net) {
        if (net == null || from == null || session == null) throw new NullPointerException();

        this.net = net;
        atoms = null;
        this.session = session;
        this.from = from;
    }

    private Message(SessionIdentifier session, VerificationKey from, Network net, Atom atom) {
        if (net == null || session == null || from == null) throw new NullPointerException();

        this.net = net;
        this.from = from;
        atoms = atom;
        this.session = session;
    }

    public Message(SessionIdentifier session, VerificationKey from, Network net, Deque atoms) {
        if (net == null || session == null || from == null || atoms == null) throw new NullPointerException();

        this.net = net;
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

        Message m = new Message(session, from, net, addrs);

        return new Message(session, from, net, Atom.attach(atoms, m.atoms));
    }

    @Override
    public com.shuffle.protocol.message.Message attach(EncryptionKey ek) {
        if (ek == null) throw new NullPointerException();

        return new Message(session, from, net, Atom.attach(atoms, Atom.make(ek)));
    }

    @Override
    public com.shuffle.protocol.message.Message attach(Address addr) {
        if (addr == null) throw new NullPointerException();

        return new Message(session, from, net, Atom.attach(atoms, Atom.make(addr)));
    }

    @Override
    public com.shuffle.protocol.message.Message attach(Signature sig) {
        if (sig == null) throw new NullPointerException();

        return new Message(session, from, net, Atom.attach(atoms, Atom.make(sig)));
    }

    @Override
    public com.shuffle.protocol.message.Message attach(Blame blame) {
        if (blame == null) throw new NullPointerException();

        return new Message(session, from, net, Atom.attach(atoms, Atom.make(blame)));
    }

    public com.shuffle.protocol.message.Message hashed() {

        return new Message(session, from, net, Atom.make(new SecureHash(this)));
    }

    @Override
    public com.shuffle.protocol.message.Message attach(com.shuffle.protocol.message.Message message) throws InvalidImplementationError {
        if (message == null) throw new NullPointerException();

        if (!(message instanceof Message)) throw new InvalidImplementationError();

        return new Message(session, from, net, Atom.attach(atoms, ((Message)message).atoms));
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
    public Blame readBlame() throws FormatException, CryptographyError {
        if (atoms == null || atoms.blame == null) throw new FormatException();

        Blame blame = atoms.blame;

        return blame;
    }

    @Override
    public Signature readSignature() throws FormatException {
        if (atoms == null || atoms.sig == null) throw new FormatException();

        return atoms.sig;
    }

    @Override
    public com.shuffle.protocol.message.Message rest() throws FormatException {

        if (atoms == null) throw new FormatException();

        return new Message(session, from, net, atoms.next);
    }

    @Override
    public Packet prepare(Phase phase, VerificationKey to, SigningKey from) {
        return new SignedPacket(this, phase, to, from);
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
