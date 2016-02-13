package com.shuffle.sim;

import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.CoinShuffle;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Machine;
import com.shuffle.protocol.Network;
import com.shuffle.protocol.Phase;
import com.shuffle.protocol.SessionIdentifier;
import com.shuffle.protocol.SignedPacket;

import java.util.SortedSet;
import java.util.concurrent.Future;

/**
 * The adversary provides for an environment in which the protocol can run in a separate thread.
 * Messages can be delivered to it as it runs.
 *
 * Created by Daniel Krawisz on 2/8/16.
 */
public class Adversary {

    private final SessionIdentifier session;
    private final CoinShuffle shuffle;
    private final Machine machine;
    private final Network network;
    private final SigningKey sk;
    private final SortedSet<VerificationKey> players;
    private final long amount;

    Adversary(
            SessionIdentifier session,
            long amount,
            SigningKey sk,
            SortedSet<VerificationKey> players,
            Coin coin,
            Transaction t,
            CoinShuffle shuffle,
            Machine machine,
            Network network) {
        this.session = session;
        this.amount = amount;
        this.sk = sk;
        this.network = network;
        this.players = players;
        this.shuffle = shuffle;
        this.machine = machine;
    }

    public SessionIdentifier session() {
        return session;
    }

    public Future<Machine> turnOn() throws InvalidImplementationError {
        return shuffle.runProtocolFuture(session, amount, sk, players, null, network);
    }

    public Phase currentPhase() {
        return machine.phase();
    }

    public SigningKey identity() {
        return sk;
    }
}
