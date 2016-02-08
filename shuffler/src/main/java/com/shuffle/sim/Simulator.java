package com.shuffle.sim;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.CryptographyError;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.protocol.InvalidImplementationError;
import com.shuffle.protocol.Machine;
import com.shuffle.protocol.MessageFactory;
import com.shuffle.protocol.Packet;
import com.shuffle.protocol.Phase;
import com.shuffle.protocol.SessionIdentifier;
import com.shuffle.protocol.SignedPacket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A simulator for running integration tests on the protocol.
 *
 * Created by Daniel Krawisz on 12/6/15.
 */
public final class Simulator {
    private static Logger log= LogManager.getLogger(Simulator.class);

    final Map<VerificationKey, Adversary> machines = new HashMap<>();
    final MessageFactory messages;
    final Crypto crypto;

    final Map<Phase, Map<VerificationKey, Map<VerificationKey, Packet>>> sent = new HashMap<>();

    public void sendTo(VerificationKey to, SignedPacket packet) throws InvalidImplementationError, InterruptedException {
        Map<VerificationKey, Map<VerificationKey, Packet>> byPacket = sent.get(packet.payload.phase);
        if(byPacket == null) {
            byPacket = new HashMap<>();
            sent.put(packet.payload.phase, byPacket);
        }

        Map<VerificationKey, Packet> bySender = byPacket.get(packet.payload.signer);
        if (bySender == null) {
            bySender = new HashMap<>();
            byPacket.put(packet.payload.signer, bySender);
        }

        bySender.put(packet.payload.recipient, packet.payload);

        machines.get(to).deliver(packet);
    }

    public Simulator(MessageFactory messages, Crypto crypto)  {
        this.messages = messages;
        this.crypto = crypto;
    }

    synchronized Map<SigningKey, Machine> runSimulation(
            List<Adversary> init)  {
        if (init == null ) throw new NullPointerException();

        sent.clear();
        machines.clear();

        Map<SigningKey, Future<Machine>> wait = new HashMap<>();
        Map<SigningKey, Machine> results = new HashMap<>();

        try {
            for (Adversary in : init) {
                machines.put(in.identity().VerificationKey(), in);
            }
        } catch (CryptographyError e) {
            log.error("Some Crypto error happened",e);
        }

        // Start the simulation.
        for (Adversary in : init) {
            wait.put(in.identity(), in.turnOn());
        }

        while (wait.size() != 0) {
            Iterator<Map.Entry<SigningKey, Future<Machine>>> i = wait.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<SigningKey, Future<Machine>> entry = i.next();
                Future<Machine> future = entry.getValue();
                if (future.isDone()) {
                    try {
                        Machine machine = future.get();
                        results.put(entry.getKey(), machine);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                    i.remove();
                }
            }
        }

        return results;
    }

    public Map<SigningKey, Machine> successfulRun(
            SessionIdentifier session,
            int numPlayers,
            long amount,
            MockCoin coin
    ) {

        InitialState init = new InitialState(session, amount).defaultCoin(coin);

        for (int i = 1; i <= numPlayers; i++) {
            init.player().initialFunds(20);
        }

        return init.run(this);
    }

    public Map<SigningKey, Machine> insufficientFundsRun(
            SessionIdentifier session,
            int numPlayers,
            int[] deadbeats, // Players who put no money in their address. 
            int[] poor, // Players who didn't put enough in their address.
            int[] spenders, // Players who don't have enough because they spent it.
            long amount,
            MockCoin coin
    ) {
        InitialState init = new InitialState(session, amount).defaultCoin(coin);

        for (int i = 1; i <= numPlayers; i++) {
            init.player().initialFunds(20);
            for (int deadbeat : deadbeats) {
                if (deadbeat == i) {
                    init.initialFunds(0);
                }
            }
            for (int aPoor : poor) {
                if (aPoor == i) {
                    init.initialFunds(10);
                }
            }
            for (int spender : spenders) {
                if (spender == i) {
                    init.spend(16);
                }
            }
        }

        return init.run(this);
    }

    public Map<SigningKey, Machine> doubleSpendingRun(
            SessionIdentifier session,
            Set<MockCoin> coinNets,
            List<MockCoin> coinNetList,
            int[] doubleSpenders,
            long amount
    ) {
        InitialState init = new InitialState(session, amount);

        int i = 1;
        for (MockCoin coinNet : coinNetList) {
            init.player().initialFunds(20).coin(coinNet);

            for (int doubleSpender : doubleSpenders) {
                if (doubleSpender == i) {
                    init.spend(16);
                }
            }
            i++;
        }

        return init.run(this);
    }
}
