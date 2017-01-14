/*
 * Copyright (c) 2006-2017 DMDirc Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dmdirc.parser.irc.processors;

import com.dmdirc.parser.events.WallDesyncEvent;
import com.dmdirc.parser.events.WallopEvent;
import com.dmdirc.parser.events.WalluserEvent;
import com.dmdirc.parser.irc.IRCParser;

import java.time.LocalDateTime;

import javax.inject.Inject;

/**
 * Process a WALLOPS Message.
 */
public class ProcessWallops extends IRCProcessor {

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     */
    @Inject
    public ProcessWallops(final IRCParser parser) {
        super(parser, "WALLOPS");
    }

    /**
     * Process a Wallops Message.
     *
     * @param sParam Type of line to process ("WALLOPS")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String... token) {
        if (token.length < 3) {
            return;
        }

        String user = token[0];
        final String message = token[token.length - 1];
        if (user.charAt(0) == ':' && user.length() > 1) {
            user = user.substring(1);
        }
        final String[] bits = message.split(" ", 2);

        if (bits.length > 1) {
            if (message.charAt(0) == '*') {
                callWallop(bits[1], user);
                return;
            } else if (message.charAt(0) == '$') {
                callWalluser(bits[1], user);
                return;
            }
        }
        callWallDesync(message, user);
    }

    /**
     * Callback to all objects implementing the Wallop Callback.
     *
     * @param message The message
     * @param host Host of the user who sent the wallop
     */
    protected void callWallop(final String message, final String host) {
        getCallbackManager().publish(new WallopEvent(parser, LocalDateTime.now(), message, host));
    }

    /**
     * Callback to all objects implementing the Walluser Callback.
     *
     * @param message The message
     * @param host Host of the user who sent the walluser
     */
    protected void callWalluser(final String message, final String host) {
        getCallbackManager().publish(new WalluserEvent(parser, LocalDateTime.now(), message, host));
    }

    /**
     * Callback to all objects implementing the WallDesync Callback.
     *
     * @param message The message
     * @param host Host of the user who sent the WallDesync
     */
    protected void callWallDesync(final String message, final String host) {
        getCallbackManager().publish(new WallDesyncEvent(
                parser, LocalDateTime.now(), message, host));
    }

}
