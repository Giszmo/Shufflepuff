/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.sim;

import com.shuffle.bitcoin.CoinNetworkException;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
import com.shuffle.chan.ReceiveChan;
import com.shuffle.chan.SendChan;
import com.shuffle.mock.InsecureRandom;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockMessageFactory;
import com.shuffle.mock.MockSessionIdentifier;
import com.shuffle.mock.MockSigningKey;
import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.TcpChannel;
import com.shuffle.player.Connect;
import com.shuffle.player.SigningKey;
import com.shuffle.protocol.CoinShuffle;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.InvalidParticipantSetException;
import com.shuffle.protocol.Network;
import com.shuffle.protocol.message.Phase;
import com.shuffle.protocol.WaitingException;
import com.shuffle.protocol.blame.Matrix;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Implementation using a mock network for testing purposes.
 *
 * Created by Daniel Krawisz on 2/3/16.
 */
class Player implements Runnable {
    private static class Parameters {
        public final SigningKey me;
        public final int port;
        public final int threads;
        public final InitialState.PlayerInitialState init;
        public final Map<InetSocketAddress, VerificationKey> identities;

        public Parameters(SigningKey me, int port, int threads,
                          InitialState.PlayerInitialState init,
                          Map<InetSocketAddress, VerificationKey> identities) {

            this.me = me;
            this.port = port;
            this.threads = threads;
            this.init = init;
            this.identities = identities;
        }
    }

    private final SendChan<Phase> msg ;
    private final Executor exec;
    private final Parameters param;

    private Player(Parameters param, SendChan<Phase> msg, Executor exec) {
        this.param = param;
        this.exec = exec;
        this.msg = msg;
    }

    static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    private static Map<String, Integer> readOptions(String[] args) {
        if (args.length % 2 != 0) {
            System.out.println("Invalid argument list: " + args.length
                    + " elements found; should be even.");
            throw new IllegalArgumentException();
        }

        Pattern opt = Pattern.compile("-[a-z]+");
        Pattern val = Pattern.compile("[0-9]+");

        Map<String, Integer> map = new HashMap<>();

        Map<String, Integer> defaults = new HashMap<>();
        defaults.put("-minport", 1803);
        defaults.put("-threads", 3);
        defaults.put("-players", null);
        defaults.put("-identity", null);
        defaults.put("-amount", 20);

        int p = 0;
        while (2 * p < args.length) {
            String optName = args[2 * p];
            String optVal = args[2 * p + 1];

            if (!opt.matcher(optName).matches()) {
                System.out.println("Error: invalid argument type found " + optName);
                throw new IllegalArgumentException();
            }

            if (!val.matcher(optVal).matches()) {
                System.out.println("Error: invalid argument value found " + optVal);
                throw new IllegalArgumentException();
            }

            if (!defaults.containsKey(optName)) {
                System.out.println("Error: unknown argument type found " + optName);
                throw new IllegalArgumentException();
            }

            map.put(optName, Integer.parseInt(optVal));

            p++;
        }

        // Insert default values.
        for (Map.Entry<String, Integer> entry : defaults.entrySet()) {
            if (!map.containsKey(entry.getKey())) {
                if (entry.getValue() == null) {
                    System.out.println("Error: no default value defined for " + entry.getKey());
                    throw new IllegalArgumentException();
                }

                map.put(entry.getKey(), entry.getValue());
            }
        }

        return map;
    }

    private static Parameters readParameters(String[] args) {
        Map<String, Integer> options = readOptions(args);
        Map<InetSocketAddress, VerificationKey> identities = new HashMap<>();
        Crypto crypto = new MockCrypto(new InsecureRandom(7777));

        InitialState init = InitialState.successful(
                new MockSessionIdentifier("tcp test"),
                options.get("-amount"),
                crypto,
                options.get("-players"));
        List<VerificationKey> keys = init.getKeys();

        // Create keys object.
        int port = options.get("-minport");
        try {
            for (VerificationKey vk : keys) {
                identities.put(new InetSocketAddress(InetAddress.getLocalHost(), port), vk);
                port ++;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        int i = options.get("-identity") - 1;
        InitialState.PlayerInitialState pinit = init.getPlayer(i);

        return new Parameters(
                new MockSigningKey(options.get("-key")),
                options.get("-minport") + i,
                options.get("-threads"),
                pinit, identities);
    }

    public static void main(String[] args) {
        Parameters param;

        try {
            param = readParameters(args);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid arguments.");
            return;
        }

        final Chan<Phase> msg = new BasicChan<>();
        Player player = new Player(param, msg, Executors.newFixedThreadPool(param.threads));
        Thread thread = new Thread(player);

        thread.start();

        report(msg, System.out);
    }

    private static void report(ReceiveChan<Phase> msg, PrintStream stream) {

        while (true) {
            Phase next = null;
            try {
                next = msg.receive();
            } catch (InterruptedException e) {
                return; // Should not happen, but whatever.
            }

            if (next == null) return;

            stream.println("Protocol enters phase " + next.toString());
        }
    }
    
    private static Transaction play(Parameters param, SendChan<Phase> msg, Executor exec)
            throws InterruptedException {

        Channel<InetSocketAddress, Bytestring> tcp =
                new TcpChannel(InetSocketAddress.createUnresolved("localhost", param.port), exec);

        Connect<InetSocketAddress> conn = null;
        Network network = null;
        try {
            conn = new Connect<>(param.init.crypto());
            network = conn.connect(param.me, tcp, param.identities, new MockMarshaller(), 1, 3);
        } catch (IOException e) {
            // Indicates that something has gone wrong with the initial connection.
            return null;
        } finally {
            if (conn != null) {
                conn.shutdown();
            }
        }

        try {
            return new CoinShuffle(
                    new MockMessageFactory(), param.init.crypto(), param.init.coin()
            ).runProtocol(
                    param.init.getSession(),
                    param.init.getAmount(),
                    param.init.sk,
                    param.init.keys,
                    null,
                    network,
                    msg
            );
        } catch (IOException // TODO there should be an exception which says that the internet connection failed.
                | CoinNetworkException // Indicates a problem with the Bitcoin network.
                | WaitingException // Indicates a lost
                | FormatException // TODO also all improperly formatted messages are ignored.
                | InvalidParticipantSetException e) {
            // TODO handle these problems appropriately.
            return null;
        } catch (Matrix matrix) {
            // Indicates that someone acted maliciously.
            matrix.printStackTrace();
            return null;
        } finally {
            conn.shutdown();
        }
    }

    @Override
    public void run() {
        // Run the protocol and signal to the other thread when it's done.
        try {
            play(param, msg, exec);
        } catch (InterruptedException e) {
        } finally {
            msg.close();
        }
    }
}
