/*
 * Copyright (c) 2006-2015 DMDirc Developers
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
import com.dmdirc.parser.events.ChannelKickEvent;
import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.irc.IRCChannelClientInfo;
import com.dmdirc.parser.irc.IRCChannelInfo;
import com.dmdirc.parser.irc.IRCClientInfo;
import com.dmdirc.parser.irc.IRCParser;

import java.util.Arrays;
import java.util.Date;

import javax.inject.Inject;

/**
 * Process a channel kick.
 */
public class ProcessKick extends IRCProcessor {

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     */
    @Inject
    public ProcessKick(final IRCParser parser) {
        super(parser, "KICK");
    }

    /**
     * Process a channel kick.
     *
     * @param sParam Type of line to process ("KICK")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String... token) {
        callDebugInfo(IRCParser.DEBUG_INFO, "processKick: %s | %s", sParam, Arrays.toString(token));

        final IRCClientInfo iClient = getClientInfo(token[3]);
        final IRCClientInfo iKicker = getClientInfo(token[0]);
        final IRCChannelInfo iChannel = getChannel(token[2]);

        if (iClient == null) {
            return;
        }

        if (IRCParser.ALWAYS_UPDATECLIENT && iKicker != null && iKicker.getHostname().isEmpty()) {
            // To facilitate dmdirc formatter, get user information
            iKicker.setUserBits(token[0], false);
        }

        if (iChannel == null) {
            if (iClient != parser.getLocalClient()) {
                callErrorInfo(new ParserError(ParserError.ERROR_WARNING, "Got kick for channel (" + token[2] + ") that I am not on. [User: " + token[3] +

                        ']', parser.getLastLine()));
            }
        } else {
            String sReason = "";
            if (token.length > 4) {
                sReason = token[token.length - 1];
            }
            final IRCChannelClientInfo iChannelClient = iChannel.getChannelClient(iClient);
            if (iChannelClient == null) {
                // callErrorInfo(new ParserError(ParserError.ERROR_WARNING, "Got kick for channel ("+token[2]+") for a non-existant user. [User: "+token[0]+"]", parser.getLastLine()));
                return;
            }
            final IRCChannelClientInfo iChannelKicker = iChannel.getChannelClient(token[0], true);
            if (parser.getRemoveAfterCallback()) {
                callDebugInfo(IRCParser.DEBUG_INFO, "processKick: calling kick before. {%s | %s | %s | %s | %s}", iChannel, iChannelClient, iChannelKicker, sReason, token[0]);
                callChannelKick(iChannel, iChannelClient, iChannelKicker, sReason, token[0]);
            }
            callDebugInfo(IRCParser.DEBUG_INFO, "processKick: removing client from channel { %s | %s }", iChannel, iClient);
            iChannel.delClient(iClient);
            callDebugInfo(IRCParser.DEBUG_INFO, "processKick: removed client from channel { %s | %s }", iChannel, iClient);
            if (!parser.getRemoveAfterCallback()) {
                callDebugInfo(IRCParser.DEBUG_INFO, "processKick: calling kick after. {%s | %s | %s | %s | %s}", iChannel, iChannelClient, iChannelKicker, sReason, token[0]);
                callChannelKick(iChannel, iChannelClient, iChannelKicker, sReason, token[0]);
            }
            if (iClient == parser.getLocalClient()) {
                iChannel.emptyChannel();
                parser.removeChannel(iChannel);
            }
        }
    }

    /**
     * Callback to all objects implementing the ChannelKick Callback.
     *
     * @param cChannel Channel where the kick took place
     * @param cKickedClient ChannelClient that got kicked
     * @param cKickedByClient ChannelClient that did the kicking
     * @param sReason Reason for kick (may be "")
     * @param sKickedByHost Hostname of Kicker (or servername)
     */
    protected void callChannelKick(final ChannelInfo cChannel,
            final ChannelClientInfo cKickedClient, final ChannelClientInfo cKickedByClient,
            final String sReason, final String sKickedByHost) {
        getCallbackManager().publish(
                new ChannelKickEvent(parser, new Date(), cChannel, cKickedClient, cKickedByClient,
                        sReason, sKickedByHost));
    }

}
