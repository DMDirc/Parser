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

import com.dmdirc.parser.common.ChannelJoinRequest;
import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.irc.IRCParser;

import java.time.LocalDateTime;
import java.util.Collection;

import javax.inject.Inject;

/**
 * Process a 001 message.
 */
public class Process001 extends IRCProcessor {

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     */
    @Inject
    public Process001(final IRCParser parser) {
        super(parser, "001");
    }

    /**
     * Process a 001 message.
     *
     * @param sParam Type of line to process ("001")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final LocalDateTime time, final String sParam, final String... token) {
        parser.got001 = true;
        // << :demon1.uk.quakenet.org 001 Java-Test :Welcome to the QuakeNet IRC Network, Java-Test
        parser.updateServerName(token[0].substring(1, token[0].length()));
        final String sNick = token[2];

        // myself will be fake if we havn't recieved a 001 yet
        if (parser.getLocalClient().isFake()) {
            // Update stored information
            parser.getLocalClient().setUserBits(sNick, true, true);
            parser.getLocalClient().setFake(false);
            parser.addClient(parser.getLocalClient());
        } else {
            // Another 001? if nicknames change then we need to update the hashtable
            if (!parser.getLocalClient().getNickname().equalsIgnoreCase(sNick)) {
                // Nick changed, remove old me
                parser.forceRemoveClient(parser.getLocalClient());
                /// Update stored information
                parser.getLocalClient().setUserBits(sNick, true, true);
                // Check that we don't already know someone by this name
                if (getClientInfo(parser.getLocalClient().getNickname()) == null) {
                    // And add to list
                    parser.addClient(parser.getLocalClient());
                } else {
                    // Someone else already know? this is bad!
                    parser.callErrorInfo(new ParserError(ParserError.ERROR_FATAL, "001 overwrites existing client?", parser.getLastLine()));
                }
            }
        }

        parser.startPingTimer();
        final Collection<? extends ChannelJoinRequest> requests = parser.extractChannels(
                parser.getURI());
        parser.joinChannels(requests.toArray(new ChannelJoinRequest[requests.size()]));
    }

}
