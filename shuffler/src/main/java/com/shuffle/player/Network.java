package com.shuffle.player;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Peer;
import com.shuffle.p2p.Session;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.SessionIdentifier;
import com.shuffle.protocol.SignedPacket;
import com.shuffle.protocol.TimeoutError;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * TODO make this into an interface.
 *
 * Created by Daniel Krawisz on 1/29/16.
 */
public class Network<Identity, Format> implements com.shuffle.protocol.Network {
    final private Channel<Identity,Format> channel;
    final private Map<VerificationKey, Identity> players = new HashMap<>();
    final private Map<VerificationKey, Session<Format>> connections = new HashMap<>();
    final private LinkedBlockingQueue<ReceivedMessage> inbox = new LinkedBlockingQueue<>();
    final private Marshaller<Format> marshaller;

    final public int timeoutSeconds;

    private class Receiver implements com.shuffle.p2p.Receiver<Format> {

        @Override
        public void receive(Format bytestring) {
            inbox.add(new ReceivedMessage(this, bytestring));
        }
    }

    private class ReceivedMessage {
        final Receiver receiver;
        final Format message;

        private ReceivedMessage(Receiver receiver, Format message) {
            this.receiver = receiver;
            this.message = message;
        }
    }

    public Network(Channel<Identity, Format> channel, Marshaller<Format> marshaller, int timeoutSeconds) {
        this.channel = channel;
        this.marshaller = marshaller;
        this.timeoutSeconds = timeoutSeconds;
    }

    public void addPeer(VerificationKey key, Identity identity) {
        Peer<Identity, Format> peer = channel.getPeer(identity);
        connections.put(key, peer.openSession(new Receiver()));
    }

    @Override
    public void sendTo(VerificationKey to, SignedPacket packet) throws InvalidImplementationError, TimeoutError {
        Session<Format> session = connections.get(to);

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
