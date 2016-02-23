package com.shuffle.sim;

import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockMarshaller;
import com.shuffle.mock.MockMessageFactory;
import com.shuffle.mock.MockSessionIdentifier;
import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.player.Connect;
import com.shuffle.protocol.CoinShuffle;
import com.shuffle.protocol.Machine;
import com.shuffle.protocol.Network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Implementation using a mock network for testing purposes.
 *
 * Created by Daniel Krawisz on 2/3/16.
 */
public class Player implements Runnable {
    Crypto crypto;
    LinkedBlockingQueue<Machine> msg = new LinkedBlockingQueue<>();

    private Player() {}

    static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            return;
        }

        int players, player;
        players = Integer.parseInt(args[0]);
        player = Integer.parseInt(args[1]);

        if (args.length != players + 2) {
            return;
        }


    }

    @Override
    public void run() {

    }

    public Machine play(InitialState.PlayerInitialState init, Map<InetSocketAddress, VerificationKey> keys) {
        Channel<InetSocketAddress, Bytestring> tcp = null; // Fill this in.

        try {
            return new CoinShuffle(new MockMessageFactory(), crypto, init.coin(crypto)).runProtocol(
                    init.getSession(),
                    init.getAmount(),
                    init.sk,
                    init.keys,
                    null,
                    new Connect<InetSocketAddress>(crypto).connect(tcp, keys, new MockMarshaller(), 1, 3),
                    msg
            );
        } catch (IOException e) {
            // TODO handle these problems appropriately.
            return null;
        }
    }
}
