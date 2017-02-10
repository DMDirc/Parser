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

import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.events.ChannelPartEvent;
import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.irc.IRCChannelClientInfo;
import com.dmdirc.parser.irc.IRCChannelInfo;
import com.dmdirc.parser.irc.IRCClientInfo;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.TimestampedIRCProcessor;

import java.time.LocalDateTime;

import javax.inject.Inject;

/**
 * Process a channel part.
 */
public class ProcessPart extends TimestampedIRCProcessor {

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     */
    @Inject
    public ProcessPart(final IRCParser parser) {
        super(parser, "PART");
    }

    /**
     * Process a channel part.
     *
     * @param date The LocalDateTime that this event occurred at.
     * @param sParam Type of line to process ("PART")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final LocalDateTime date, final String sParam, final String... token) {
        // :nick!ident@host PART #Channel
        // :nick!ident@host PART #Channel :reason
        if (token.length < 3) {
            return;
        }

        final IRCClientInfo iClient = getClientInfo(token[0]);
        final IRCChannelInfo iChannel = getChannel(token[2]);

        if (iClient == null) {
            return;
        }
        if (IRCParser.ALWAYS_UPDATECLIENT && iClient.getHostname().isEmpty()) {
            // This may seem pointless - updating before they leave - but the formatter needs it!
            iClient.setUserBits(token[0], false);
        }
        if (iChannel == null) {
            if (iClient != parser.getLocalClient()) {
                callErrorInfo(new ParserError(ParserError.ERROR_WARNING, "Got part for channel (" + token[2] + ") that I am not on. [User: " + token[0] +

                        ']', parser.getLastLine()));
            }
        } else {
            String sReason = "";
            if (token.length > 3) {
                sReason = token[token.length - 1];
            }
            final IRCChannelClientInfo iChannelClient = iChannel.getChannelClient(iClient);
            if (iChannelClient == null) {
                // callErrorInfo(new ParserError(ParserError.ERROR_WARNING, "Got part for channel ("+token[2]+") for a non-existant user. [User: "+token[0]+"]", parser.getLastLine()));
                return;
            }
            if (parser.getRemoveAfterCallback()) {
                callChannelPart(date, iChannel, iChannelClient, sReason);
            }
            callDebugInfo(IRCParser.DEBUG_INFO, "Removing %s from %s", iClient.getNickname(), iChannel.getName());
            iChannel.delClient(iClient);
            if (!parser.getRemoveAfterCallback()) {
                callChannelPart(date, iChannel, iChannelClient, sReason);
            }
            if (iClient == parser.getLocalClient()) {
                iChannel.emptyChannel();
                parser.removeChannel(iChannel);
            }
        }
    }

    /**
     * Callback to all objects implementing the ChannelPart Callback.
     *
     * @param date The LocalDateTime that this event occurred at.
     * @param cChannel Channel that the user parted
     * @param cChannelClient Client that parted
     * @param sReason Reason given for parting (May be "")
     */
    protected void callChannelPart(final LocalDateTime date, final ChannelInfo cChannel,
            final ChannelClientInfo cChannelClient, final String sReason) {
        getCallbackManager().publish(
                new ChannelPartEvent(parser, date, cChannel, cChannelClient,
                        sReason));
    }

}
