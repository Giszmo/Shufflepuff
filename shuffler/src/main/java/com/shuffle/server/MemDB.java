package com.shuffle.server;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory version of the database.
 *
 * Created by Daniel Krawisz on 12/25/15.
 */
class MemDB implements Database {
    Set<Mix.MixDB> mixes = Collections.newSetFromMap(new ConcurrentHashMap<Mix.MixDB, Boolean>());
    Map<Mix.MixDB, Set<Registration.RegistrationDB>> players = new ConcurrentHashMap<>();
}
