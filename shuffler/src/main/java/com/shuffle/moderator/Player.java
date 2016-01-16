package com.shuffle.moderator;

/**
 * Created by Daniel Krawisz on 12/25/15.
 */
public interface Player {
    // Whether we can send messages to this player.
    boolean connected();

    // Send the player the necessary information about a mix that is ready to begin now.
    void introduce(Mixes.Mix mix);
}
