/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dmdirc.parser.irc;

/**
 * IRC Parser Exception!
 *
 * @author shane
 */
class IRCParserException extends Exception {

    /** Version of this class. */
    private static final long serialVersionUID = 1L;

    /**
     * Create a new IRCParserException
     *
     * @param message Reason for exception
     */
    public IRCParserException(final String message) {
        super(message);
    }

}
