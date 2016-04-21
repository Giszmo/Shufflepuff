/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.sim.InitialState;

import org.junit.Test;

/**
 * During the announcement phase, a player could send different encryption keys to other players.
 * This should be detected during the equivocation check phase.
 *
 * Created by Daniel Krawisz on 3/17/16.
 */
public class TestEquivocateAnnouncement extends TestShuffleMachine{

    // Run a test case for equivocation during phase 1.
    private void EquivocateAnnouncement(
            int numPlayers,
            InitialState.Equivocation[] equivocators
    ) {
        String description = "case " + caseNo + "; announcement equivocation test case.";
        check(new MockTestCase(description).equivocateAnnouncementTestCase(
                numPlayers, equivocators
        ));
    }

    @Test
    public void testEquivocationAnnounce() {
        // A player sends different encryption keys to different players.
        EquivocateAnnouncement(3,
                new InitialState.Equivocation[]{
                        new InitialState.Equivocation(2, new int[]{3})});
        EquivocateAnnouncement(5,
                new InitialState.Equivocation[]{
                        new InitialState.Equivocation(2, new int[]{4, 5})});
        EquivocateAnnouncement(10,
                new InitialState.Equivocation[]{
                        new InitialState.Equivocation(2, new int[]{4, 10}),
                        new InitialState.Equivocation(5, new int[]{7, 8})});
        EquivocateAnnouncement(10,
                new InitialState.Equivocation[]{
                        new InitialState.Equivocation(2, new int[]{3}),
                        new InitialState.Equivocation(4, new int[]{5, 6}),
                        new InitialState.Equivocation(8, new int[]{9})});
    }
}
