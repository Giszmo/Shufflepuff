package com.shuffle.server;

import java.io.Serializable;

/**
 * Created by Daniel Krawisz on 12/25/15.
 */
public class Player {

    // The player as represented in the database.
    public class PlayerDB implements Serializable {
        int id;

        private PlayerDB() {

        }
    }


}
