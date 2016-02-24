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
import com.shuffle.protocol.CoinShuffle;
import com.shuffle.protocol.Machine;
import com.shuffle.protocol.Phase;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
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

    final LinkedBlockingQueue<Machine> msg = new LinkedBlockingQueue<>();
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
                throw new IllegalArgumentException();
            }

            if (!val.matcher(optVal).matches()) {
                throw new IllegalArgumentException();
            }

            if (!defaults.containsKey(optName)) {
                throw new IllegalArgumentException();
            }

            map.put(optName, Integer.parseInt(optVal));

            p++;
        }

        // Insert default values.
        for (Map.Entry<String, Integer> entry : defaults.entrySet()) {
            if (!map.containsKey(entry.getKey())) {
                if (entry.getValue() == null) {
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
        int i = options.get("-identity") - 1;
        Crypto crypto = new MockCrypto(7777);

        InitialState.PlayerInitialState pinit = InitialState.successful(
                new MockSessionIdentifier("tcp test"),
                options.get("-players"),
                options.get("-amount"),
                crypto).getPlayers().get(i);

        return new Parameters(
                options.get("-minport") + i,
                options.get("-threads"),
                pinit,
                keys, crypto);
    }

    public static void main(String[] args) {
        try {
            Player player = new Player(readParameters(args), System.out);
            Thread thread = new Thread(player);

            thread.start();

            player.report();
        } catch (Exception e) {
            return;
        }

    }

    void report() {
        Machine machine = msg.poll();
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
        msg.poll();
    }

    private Machine play() {
        Channel<InetSocketAddress, Bytestring> tcp = new TCPChannel(param.port, exec);
        Crypto crypto = param.crypto;

        try {
            return new CoinShuffle(new MockMessageFactory(), crypto, param.init.coin(crypto)).runProtocol(
                    param.init.getSession(),
                    param.init.getAmount(),
                    param.init.sk,
                    param.init.keys,
                    null,
                    new Connect<InetSocketAddress>(crypto).connect(tcp, param.keys, new MockMarshaller(), 1, 3),
                    msg
            );
        } catch (IOException e) {
            // TODO handle these problems appropriately.
            return null;
        }
    }

    @Override
    public void run() {
        // Run the protocol and signal to the other thread when it's done.
        Machine machine = play();

        try {
            if (machine != null) {
                msg.put(machine);
            }
        } catch (InterruptedException e) {
            return;
        }
    }
}
