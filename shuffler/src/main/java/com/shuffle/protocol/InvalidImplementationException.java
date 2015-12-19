package com.shuffle.protocol;

/**
 * This exception is thrown when a check fails that could only result from an invalid implementation
 * of the protocol itself rather than an external problem.
 *
 * All functions which throw this exception need to be implemented by the user and should be
 * caught and dealt with by the user.
 *
 * Created by Daniel Krawisz on 12/6/15.
 *
 */
public class InvalidImplementationException extends Exception {
}
