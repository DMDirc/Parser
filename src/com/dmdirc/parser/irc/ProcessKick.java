/*
 * Copyright (c) 2006-2011 DMDirc Developers
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

package com.dmdirc.parser.irc;

import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.callbacks.ChannelKickListener;

import java.util.Arrays;

/**
 * Process a channel kick.
 */
public class ProcessKick extends IRCProcessor {

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param manager ProcessingManager that is in charge of this IRCProcessor
     */
    protected ProcessKick(final IRCParser parser, final ProcessingManager manager) {
        super(parser, manager);
    }

    /**
     * Process a channel kick.
     *
     * @param sParam Type of line to process ("KICK")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String[] token) {
        callDebugInfo(IRCParser.DEBUG_INFO, "processKick: %s | %s", sParam, Arrays.toString(token));

        IRCChannelClientInfo iChannelClient;
        IRCChannelClientInfo iChannelKicker;
        IRCChannelInfo iChannel;
        IRCClientInfo iClient;
        IRCClientInfo iKicker;
        String sReason = "";

        iClient = getClientInfo(token[3]);
        iKicker = getClientInfo(token[0]);
        iChannel = getChannel(token[2]);

        if (iClient == null) {
            return;
        }

        if ((IRCParser.ALWAYS_UPDATECLIENT && iKicker != null) && iKicker.getHostname().isEmpty()) {
            // To facilitate dmdirc formatter, get user information
            iKicker.setUserBits(token[0], false);
        }

        if (iChannel == null) {
            if (iClient != parser.getLocalClient()) {
                callErrorInfo(new ParserError(ParserError.ERROR_WARNING, "Got kick for channel (" + token[2] + ") that I am not on. [User: " + token[3] + "]", parser.getLastLine()));
            }
            return;
        } else {
            if (token.length > 4) {
                sReason = token[token.length - 1];
            }
            iChannelClient = iChannel.getChannelClient(iClient);
            if (iChannelClient == null) {
                // callErrorInfo(new ParserError(ParserError.ERROR_WARNING, "Got kick for channel ("+token[2]+") for a non-existant user. [User: "+token[0]+"]", parser.getLastLine()));
                return;
            }
            iChannelKicker = iChannel.getChannelClient(token[0]);
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
     * @see IChannelKick
     * @param cChannel Channel where the kick took place
     * @param cKickedClient ChannelClient that got kicked
     * @param cKickedByClient ChannelClient that did the kicking (may be null if server)
     * @param sReason Reason for kick (may be "")
     * @param sKickedByHost Hostname of Kicker (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callChannelKick(final ChannelInfo cChannel, final ChannelClientInfo cKickedClient, final ChannelClientInfo cKickedByClient, final String sReason, final String sKickedByHost) {
        return getCallbackManager().getCallbackType(ChannelKickListener.class).call(cChannel, cKickedClient, cKickedByClient, sReason, sKickedByHost);
    }

    /**
     * What does this IRCProcessor handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"KICK"};
    }
}
