package com.shuffle.chan;

import java.util.concurrent.TimeUnit;

/**
 * A Chan that can be limited in throughput and maxium latency. 
 *
 * Created by Daniel Krawisz on 4/14/16.
 */
public class RateLimitedSend<X> implements Chan<X> {

    public interface Size<X> {
        double size(X x);
    }

    private class Message {
        final X payload;
        final long releaseTime; // The time that this message may be released.

        private Message(X payload, long releaseTime) {

            this.payload = payload;
            this.releaseTime = releaseTime;
        }
    }

    private final Chan<Message> chan;
    private final Size<X> size;

    // Maximum allowed latency. If inserting a message would exceed this, then the
    // channel is closed. Units are in milliseconds.
    private long maxLatency;

    // Maximum data allowed to be stuck in the channel. If inserting a message would
    // exceed this, then the channel is closed. Units in Size.
    private double maxEnclosedSize;

    // The number of milliseconds that is required for one unit of Size to pass through
    // this channel.
    private long unitDuration;

    private long bookingTime; // The time at which a message, if inserted now, could be accessed.
    private double enclosedSize; // The total size of all messages in the channel.

    Message released = null;

    public RateLimitedSend(int capacity, Size<X> size, long unitDuration, double maxEnclosedSize, long maxLatency) {

        this.chan = new BasicChan<>(capacity);
        this.size = size;
        this.unitDuration = unitDuration;
        this.maxLatency = maxLatency;
        this.maxEnclosedSize = maxEnclosedSize;

    }

    @Override
    public boolean send(X x) throws InterruptedException {
        // Message size and time to send.
        double xSize = size.size(x);

        // This effectively means we can't process more than one message per millisecond.
        // I hope that's ok.
        long xDuration = (long)Math.ceil(xSize * unitDuration);

        // How much size would this channel take up if this message were added?
        double newSize = xSize + enclosedSize;

        // Would this fill up the channel?
        if (newSize > maxEnclosedSize) {
            chan.close();
            return false;
        }

        long now = System.currentTimeMillis();

        // Booking time is reset if we're going slower than the max rate.
        if (now > bookingTime) {
            bookingTime = now;
        }

        // If booking time is going to be too long, also close the channel.
        long newBookingTime = bookingTime + xDuration;

        if (newBookingTime - now > maxLatency) {
            chan.close();
            return false;
        }

        boolean success = chan.send(new Message(x, newBookingTime));

        if (success) {

            bookingTime = newBookingTime;
            enclosedSize = newSize;
        }

        return success;
    }

    @Override
    public synchronized X receive() throws InterruptedException {

        // Take the next message out of the channel.
        if (released == null) {
            released = chan.receive();
        }

        // Wait a bit if the message can't be released yet.
        long now = System.currentTimeMillis();

        if (released.releaseTime > now) {
            Thread.sleep(released.releaseTime - now);
        }

        // Now release it.
        try {
            return released.payload;
        } finally {
            released = null;
        }
    }

    @Override
    public synchronized X receive(long l, TimeUnit u) throws InterruptedException {
        long before = System.currentTimeMillis();
        long waitTime = u.convert(l, TimeUnit.MILLISECONDS);

        // Take the next message out of the channel.
        if (released == null) {
            released = chan.receive(l, u);
        }

        // Wait a bit if the message can't be released yet.
        long now = System.currentTimeMillis();
        waitTime -= (now - before);

        if (waitTime <= 0) {
            return null;
        }

        if (released.releaseTime > now + waitTime) {
            Thread.sleep(waitTime);
            return null;
        } else {
            Thread.sleep(released.releaseTime - now);
        }

        // Now release it.
        try {
            return released.payload;
        } finally {
            released = null;
        }
    }

    @Override
    public void close() throws InterruptedException {
        chan.close();
    }

    @Override
    public boolean closed() {
        return chan.closed();
    }
}
