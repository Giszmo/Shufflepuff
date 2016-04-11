package com.shuffle.player;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Connection;
import com.shuffle.p2p.Listener;
import com.shuffle.p2p.Peer;
import com.shuffle.p2p.Receiver;
import com.shuffle.p2p.Session;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Daniel Krawisz on 4/12/16.
 */
public class SignedChannel<Address> implements Channel<VerificationKey, SignedPacket> {
    private final Marshaller<Bytestring> marshaller;
    private final Channel<Address, Bytestring> underlying;
    private final Map<VerificationKey, Address> directory;
    private final Map<Address, VerificationKey> inverseDirectory;
    private final SigningKey me;

    public SignedChannel(
            SigningKey me,
            Marshaller<Bytestring> marshaller,
            Channel<Address, Bytestring> underlying,
            Map<Address, VerificationKey> inverseDirectory
    ) {
        if (me == null || marshaller == null || underlying == null || inverseDirectory == null)
            throw new NullPointerException();

        this.me = me;
        this.marshaller = marshaller;
        this.underlying = underlying;
        this.inverseDirectory = inverseDirectory;
        directory = new HashMap<>();

        for (Map.Entry<Address, VerificationKey> entry : inverseDirectory.entrySet()) {
            if (entry.getValue() == null) throw new IllegalArgumentException();

            if (directory.containsKey(entry.getValue())) {
                throw new IllegalArgumentException();
            }

            directory.put(entry.getValue(), entry.getKey());
        }
    }

    class SignedConnection implements Connection<VerificationKey, SignedPacket> {
        private final Connection<Address, Bytestring> underlying;

        SignedConnection(Connection<Address, Bytestring> underlying) {
            if (underlying == null) {
                throw new NullPointerException();
            }
            this.underlying = underlying;
        }

        @Override
        public void close() {
            underlying.close();
        }
    }

    class SignedReceiver implements Receiver<Bytestring> {
        private final Receiver<SignedPacket> overlaying;
        private final VerificationKey from; // Whom do we expect to be receiving messages from?

        SignedReceiver(Receiver<SignedPacket> overlaying, VerificationKey from) {
            if (overlaying == null || from == null) throw new NullPointerException();

            this.overlaying = overlaying;
            this.from = from;
        }

        @Override
        public void receive(Bytestring bytestring) throws InterruptedException {
            if (bytestring == null) throw new NullPointerException();

            SignedPacket packet = marshaller.unmarshall(bytestring);

            // Ignore messages that do not have the right format.
            // Ignore messages that aren't to me.
            // Ignore messages if they don't have the right signature.
            if (packet == null || !packet.to().equals(me.VerificationKey()) || !from.verify(packet, packet.signature)) return;

            overlaying.receive(packet);
        }
    }

    class SignedPeer implements Peer<VerificationKey, SignedPacket> {
        private final Peer<Address, Bytestring> underlying;
        private final VerificationKey identity;
        private final Address address;

        SignedPeer(Peer<Address, Bytestring> underlying, VerificationKey identity, Address address) {
            if (underlying == null || identity == null || address == null) throw new NullPointerException();

            this.underlying = underlying;
            this.identity = identity;
            this.address = address;
        }

        @Override
        public VerificationKey identity() {
            return identity;
        }

        @Override
        public Session<VerificationKey, SignedPacket> openSession(Receiver<SignedPacket> receiver) throws InterruptedException {
            Session<Address, Bytestring> session =  underlying.openSession(new SignedReceiver(receiver, identity));

            if (session == null) return null;

            return new SignedSession(session, identity, address);
        }

        @Override
        public boolean open() {
            return underlying.open();
        }

        @Override
        public void close() {
            underlying.close();
        }
    }

    class SignedSession implements Session<VerificationKey, SignedPacket> {
        private final Session<Address, Bytestring> underlying;
        private final VerificationKey identity;
        private final Address address;

        SignedSession(Session<Address, Bytestring> underlying, VerificationKey identity, Address address) {
            if (underlying == null || identity == null || address == null) throw new NullPointerException();

            this.underlying = underlying;
            this.identity = identity;
            this.address = address;
        }

        @Override
        public boolean closed() {
            return underlying.closed();
        }

        @Override
        public Peer<VerificationKey, SignedPacket> peer() {
            return new SignedPeer(underlying.peer(), identity, address);
        }

        @Override
        public boolean send(SignedPacket packet) throws InterruptedException {
            Bytestring msg = marshaller.marshall(packet);

            return msg != null && underlying.send(msg);

        }

        @Override
        public void close() {
            underlying.close();
        }
    }

    class SignedListener implements Listener<Address, Bytestring> {
        private final Listener<VerificationKey, SignedPacket> overlaying;

        SignedListener(Listener<VerificationKey, SignedPacket> overlaying) {
            if (overlaying == null) throw new NullPointerException();

            this.overlaying = overlaying;
        }

        @Override
        public Receiver<Bytestring> newSession(Session<Address, Bytestring> session) {
            if (session == null) throw new NullPointerException();

            Address address = session.peer().identity();

            if (address == null) return null;

            VerificationKey identity = inverseDirectory.get(address);

            if (identity == null) return null;

            Receiver<SignedPacket> receiver = overlaying.newSession(new SignedSession(session, identity, address));

            if (receiver == null) return null;

            return new SignedReceiver(receiver, identity);
        }
    }

    @Override
    public Connection<VerificationKey, SignedPacket> open(Listener<VerificationKey, SignedPacket> listener) {
        if (listener == null) throw new NullPointerException();

        Connection<Address, Bytestring> conn = underlying.open(new SignedListener(listener));

        if (conn == null) return null;

        return new SignedConnection(conn);
    }

    @Override
    public Peer<VerificationKey, SignedPacket> getPeer(VerificationKey you) {
        Address address = directory.get(you);

        if (address == null) {
            return null;
        }

        return new SignedPeer(underlying.getPeer(address), you, address);
    }
}
