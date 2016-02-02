package com.shuffle.player;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Peer;
import com.shuffle.p2p.Session;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.SessionIdentifier;
import com.shuffle.protocol.SignedPacket;
import com.shuffle.protocol.TimeoutError;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Daniel Krawisz on 1/29/16.
 */
public abstract class Network<Identity> implements com.shuffle.protocol.Network {
    final private Channel<Identity, Bytestring, SessionIdentifier> channel;
    final private Map<VerificationKey, Identity> players = new HashMap<>();
    final private Map<VerificationKey, Session<Bytestring, SessionIdentifier>> connections = new HashMap<>();
    final private LinkedBlockingQueue<ReceivedMessage> inbox = new LinkedBlockingQueue<>();
    final private Marshaller<Bytestring> marshaller;

    final public int timeoutSeconds;

    private class Receiver implements com.shuffle.p2p.Receiver<Bytestring> {

        @Override
        public void receive(Bytestring bytestring) {
            inbox.add(new ReceivedMessage(this, bytestring));
        }
    }

    private class ReceivedMessage {
        final Receiver receiver;
        final Bytestring message;

        private ReceivedMessage(Receiver receiver, Bytestring message) {
            this.receiver = receiver;
            this.message = message;
        }
    }

    public Network(Channel<Identity, Bytestring, SessionIdentifier> channel, Marshaller<Bytestring> marshaller, int timeoutSeconds) {
        this.channel = channel;
        this.marshaller = marshaller;
        this.timeoutSeconds = timeoutSeconds;
    }

    public void addPeer(VerificationKey key, Identity identity) {
        Peer<Identity, Bytestring, SessionIdentifier> peer = channel.getPeer(identity);
        connections.put(key, peer.openSession(new Receiver()));
    }

    @Override
    public void sendTo(VerificationKey to, SignedPacket packet) throws InvalidImplementationError, TimeoutError {
        Session<Bytestring, SessionIdentifier> session = connections.get(to);

        if (session == null) {
            // Should not happen.
            throw new InvalidImplementationError();
        }

        session.send(marshaller.marshall(packet));
    }

    @Override
    public SignedPacket receive() throws TimeoutError, InvalidImplementationError, InterruptedException, FormatException {
        ReceivedMessage message = inbox.poll(1, TimeUnit.SECONDS);

        if (message == null) {
            throw new TimeoutError();
        }

        return marshaller.unmarshall(message.message);
    }


}
