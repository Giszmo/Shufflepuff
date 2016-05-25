package com.shuffle.sim;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Inbox;
import com.shuffle.chan.Receive;
import com.shuffle.chan.Send;
import com.shuffle.chan.packet.Signed;
import com.shuffle.chan.packet.SigningSend;
import com.shuffle.chan.packet.VerifyingSend;
import com.shuffle.chan.packet.SessionIdentifier;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that creates connections between players in a simulation.
 *
 * Created by Daniel Krawisz on 5/21/16.
 */
class Initializer<X> {

    // The set of incoming mailboxes for each player.
    public final Map<SigningKey, Inbox<VerificationKey, Signed<X>>> mailboxes = new HashMap<>();

    // The set of channels that player use to send to other players.
    public final Map<SigningKey, Map<VerificationKey, Send<X>>> networks = new HashMap<>();

    public final SessionIdentifier session;
    public final int capacity;
    private final SigningSend.Marshaller<X> mm;

    Initializer(SessionIdentifier session, SigningSend.Marshaller<X> mm, int capacity) {

        if (session == null || mm == null || capacity == 0) throw new IllegalArgumentException();

        this.session = session;
        this.capacity = capacity;
        this.mm = mm;
    }

    public static class Connections<X> {
        public final VerificationKey identity;
        public final Map<VerificationKey, Send<X>> send;
        public final Receive<Inbox.Envelope<VerificationKey, Signed<X>>> receive;

        public Connections(
                VerificationKey identity,
                Map<VerificationKey, Send<X>> send,
                Receive<Inbox.Envelope<VerificationKey, Signed<X>>> receive) {

            if (identity == null || send == null || receive == null)
                throw new NullPointerException();

            this.identity = identity;
            this.send = send;
            this.receive = receive;
        }
    }

    // This function is called every time a new player is created in a simulation.
    public Connections<X> connect(SigningKey sk) {
        VerificationKey vk = sk.VerificationKey();

        // Create a new map. This will contain the channels from this mailbox to the others.
        Map<VerificationKey, Send<X>> inputs = new HashMap<>();
        networks.put(sk, inputs);

        // Ceate a new mailbox.
        Inbox<VerificationKey, Signed<X>> inbox = new Inbox<>(capacity);

        // Create input channels for this new mailbox that lead to all other mailboxes
        // and create input channels for all the other mailboxes for this new one.
        for (Map.Entry<SigningKey, Inbox<VerificationKey, Signed<X>>> entry : mailboxes.entrySet()) {
            SigningKey ks = entry.getKey();
            VerificationKey kv = ks.VerificationKey();
            Inbox<VerificationKey, Signed<X>> box = entry.getValue();

            // Create a session from the new mailbox to the previous one.
            inputs.put(kv, new SigningSend<>(new VerifyingSend<>(box.receivesFrom(vk), mm, vk), mm, sk));

            // And create a corresponding session the other way.
            networks.get(ks).put(vk, new SigningSend<X>(new VerifyingSend<>(inbox.receivesFrom(kv), mm, kv), mm, ks));
        }

        // Put the mailbox in the set.
        mailboxes.put(sk, inbox);

        return new Connections<X>(vk, inputs, inbox);
    }
}
