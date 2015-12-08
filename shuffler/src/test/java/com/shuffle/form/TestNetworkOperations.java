package com.shuffle.form;

import org.junit.Test;

/**
 * Tests for the functions in NetworkOperations
 * TODO
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
public class TestNetworkOperations  {
    static class opponentSetTestCase {
        int i;
        int N;
        int player;
        int[] expected;

        opponentSetTestCase(int i, int N, int player, int[] expected) {
            this.i = i;
            this.N = N;
            this.player = player;
            this.expected = expected;
        }
    }

    @Test
    public void testOpponentSet() {
        opponentSetTestCase tests[] = new opponentSetTestCase[]{};
    }

    static class determineSenderTestCase {
        int player;
        int[] players;

        determineSenderTestCase(int player, int[] players) {
            this.player = player;
            this.players = players;
        }
    }

    @Test
    public void testDetermineSender() {
        determineSenderTestCase tests[] = new determineSenderTestCase[]{};
    }

    static class broadcastTestCase {
        int[] recipients;
    }

    @Test
    public void testBroadcast() {
        broadcastTestCase tests[] = new broadcastTestCase[]{};
    }

    static class sendToTestCase {

    }

    @Test
    public void testSendTo() {
        sendToTestCase tests[] = new sendToTestCase[]{};
    }

    static class receiveFromTestCase {

    }

    @Test
    public void testReceiveFrom() {
        receiveFromTestCase tests[] = new receiveFromTestCase[]{};
    }

    static class receiveFromMultipleTestCase {

    }

    @Test
    public void testReceiveFromMultiple() {
        receiveFromMultipleTestCase tests[] = new receiveFromMultipleTestCase[]{};
    }
}
