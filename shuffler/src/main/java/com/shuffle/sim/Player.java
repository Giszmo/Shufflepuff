package com.shuffle.sim;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockMarshaller;
import com.shuffle.mock.MockMessageFactory;
import com.shuffle.mock.MockSessionIdentifier;
import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.TCPChannel;
import com.shuffle.player.Connect;
import com.shuffle.chan.Chan;
import com.shuffle.protocol.CoinShuffle;
import com.shuffle.protocol.Machine;
import com.shuffle.protocol.Phase;

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
public class Player implements Runnable {
    private static class Parameters {
        public final int port;
        public final int threads;
        public final InitialState.PlayerInitialState init;
        public final Map<InetSocketAddress, VerificationKey> keys;
        public final Crypto crypto;

        public Parameters(int port, int threads, InitialState.PlayerInitialState init,
                          Map<InetSocketAddress, VerificationKey> keys, Crypto crypto) {

            this.port = port;
            this.threads = threads;
            this.init = init;
            this.keys = keys;
            this.crypto = crypto;
        }
    }

    final Chan<Machine> msg = new Chan<>();
    final Executor exec;
    final Parameters param;
    final PrintStream stream;

    private Player(Parameters param, PrintStream stream) {
        this.param = param;
        this.exec = Executors.newFixedThreadPool(param.threads);
        this.stream = stream;
    }

    static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    static Map<String, Integer> readOptions(String[] args) {
        if (args.length % 2 != 0) {
            System.out.println("Invalid argument list: " + args.length + " elements found; should be even.");
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

    public static Parameters readParameters(String[] args) {
        Map<String, Integer> options = readOptions(args);
        Map<InetSocketAddress, VerificationKey> keys = new HashMap<>();
        Crypto crypto = new MockCrypto(7777);

        List<InitialState.PlayerInitialState> inits = InitialState.successful(
                new MockSessionIdentifier("tcp test"),
                options.get("-players"),
                options.get("-amount"),
                crypto).getPlayers();

        // Create keys object.
        int port = options.get("-minport");
        try {
            for (InitialState.PlayerInitialState p : inits) {
                    keys.put(new InetSocketAddress(InetAddress.getLocalHost(), port), p.vk);
                port ++;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        int i = options.get("-identity") - 1;
        InitialState.PlayerInitialState pinit = inits.get(i);

        return new Parameters(
                options.get("-minport") + i,
                options.get("-threads"),
                pinit, keys, crypto);
    }

    public static void main(String[] args) {
        try {
            Player player = new Player(readParameters(args), System.out);
            Thread thread = new Thread(player);

            thread.start();

            player.report();
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid arguments.");
            return;
        }

    }

    void report() {
        try {
            Machine machine = msg.receive();
            stream.println("Protocol has started.");
            Phase phase = Phase.Uninitiated;

            while (true) {
                Phase next = machine.phase();
                if (next != phase) {
                    stream.println("Protocol enters phase " + next.toString());
                    phase = next;

                    if (phase == Phase.Completed || phase == Phase.Blame) {
                        break;
                    }
                }
            }

            // Wait until the other thread signals us to finish.
            msg.receive();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Machine play() {
        Channel<InetSocketAddress, Bytestring> tcp = new TCPChannel(param.port, exec);
        Crypto crypto = param.crypto;

        Connect<InetSocketAddress> connect = (Connect<InetSocketAddress>) new Connect<InetSocketAddress>(crypto);

        try {
            return new CoinShuffle(new MockMessageFactory(), crypto, param.init.coin(crypto)).runProtocol(
                    param.init.getSession(),
                    param.init.getAmount(),
                    param.init.sk,
                    param.init.keys,
                    null,
                    connect.connect(tcp, param.keys, new MockMarshaller(), 1, 3),
                    msg
            );
        } catch (IOException | InterruptedException e) {
            // TODO handle these problems appropriately.
            return null;
        } finally {
            connect.shutdown();
        }
    }

    @Override
    public void run() {
        // Run the protocol and signal to the other thread when it's done.
        play();
        msg.close();
    }
}
