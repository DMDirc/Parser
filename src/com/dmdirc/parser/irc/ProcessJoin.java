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
import com.dmdirc.parser.common.QueuePriority;
import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.callbacks.ChannelJoinListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelSelfJoinListener;

import java.util.Arrays;

/**
 * Process a channel join.
 */
public class ProcessJoin extends IRCProcessor {

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param manager ProcessingManager that is in charge of this IRCProcessor
     */
    protected ProcessJoin(final IRCParser parser, final ProcessingManager manager) {
        super(parser, manager);
    }

    /**
     * Process a channel join.
     *
     * @param sParam Type of line to process ("JOIN")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String[] token) {
        callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: %s | %s", sParam, Arrays.toString(token));

        if ("329".equals(sParam)) {
            if (token.length < 5) {
                return;
            }
            final IRCChannelInfo iChannel = parser.getChannel(token[3]);
            if (iChannel != null) {
                try {
                    iChannel.setCreateTime(Integer.parseInt(token[4]));
                } catch (NumberFormatException nfe) {
                    // Oh well, not a normal ircd I guess
                }
            }
        } else {
            // :nick!ident@host JOIN (:)#Channel
            if (token.length < 3) {
                return;
            }
            IRCClientInfo iClient;
            IRCChannelInfo iChannel;
            IRCChannelClientInfo iChannelClient;

            iClient = getClientInfo(token[0]);
            iChannel = parser.getChannel(token[token.length - 1]);

            callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: client: %s", iClient);
            callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: channel: %s", iChannel);

            if (iClient == null) {
                iClient = new IRCClientInfo(parser, token[0]);
                parser.addClient(iClient);
                callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: new client.", iClient);
            }
            // Check to see if we know the host/ident for this client to facilitate dmdirc Formatter
            if (iClient.getHostname().isEmpty()) {
                iClient.setUserBits(token[0], false);
            }
            if (iChannel != null) {
                if (iClient == parser.getLocalClient()) {
                    try {
                        if (iChannel.getChannelClient(iClient) == null) {
                            // Otherwise we have a channel known, that we are not in?
                            parser.callErrorInfo(new ParserError(ParserError.ERROR_FATAL, "Joined known channel that we wern't already on..", parser.getLastLine()));
                        } else {
                            // If we are joining a channel we are already on, fake a part from
                            // the channel internally, and rejoin.
                            parser.getProcessingManager().process("PART", token);
                        }
                    } catch (ProcessorNotFoundException e) {
                    }
                } else if (iChannel.getChannelClient(iClient) == null) {
                    // This is only done if we are already the channel, and it isn't us that
                    // joined.
                    callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: Adding client to channel.");
                    iChannelClient = iChannel.addClient(iClient);
                    callChannelJoin(iChannel, iChannelClient);
                    callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: Added client to channel.");
                    return;
                } else {
                    // Client joined channel that we already know of.
                    callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: Not adding client to channel they are already on.");
                    return;
                }
            }

            iChannel = new IRCChannelInfo(parser, token[token.length - 1]);
            // Add ourself to the channel, this will be overridden by the NAMES reply
            iChannel.addClient(iClient);
            parser.addChannel(iChannel);
            sendString("MODE " + iChannel.getName(), QueuePriority.LOW);

            callChannelSelfJoin(iChannel);
        }
    }

    /**
     * Callback to all objects implementing the ChannelJoin Callback.
     *
     * @see IChannelJoin
     * @param cChannel Channel Object
     * @param cChannelClient ChannelClient object for new person
     * @return true if a method was called, false otherwise
     */
    protected boolean callChannelJoin(final ChannelInfo cChannel, final ChannelClientInfo cChannelClient) {
        return getCallbackManager().getCallbackType(ChannelJoinListener.class).call(cChannel, cChannelClient);
    }

    /**
     * Callback to all objects implementing the ChannelSelfJoin Callback.
     *
     * @see IChannelSelfJoin
     * @param cChannel Channel Object
     * @return true if a method was called, false otherwise
     */
    protected boolean callChannelSelfJoin(final ChannelInfo cChannel) {
        return getCallbackManager().getCallbackType(ChannelSelfJoinListener.class).call(cChannel);
    }

    /**
     * What does this IRCProcessor handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"JOIN", "329"};
    }
}
