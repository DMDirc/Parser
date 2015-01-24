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

import com.dmdirc.parser.events.ChannelQuitEvent;
import com.dmdirc.parser.events.QuitEvent;
import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.ClientInfo;
import com.dmdirc.parser.irc.IRCChannelClientInfo;
import com.dmdirc.parser.irc.IRCChannelInfo;
import com.dmdirc.parser.irc.IRCClientInfo;
import com.dmdirc.parser.irc.IRCParser;

import java.util.ArrayList;
import java.util.Date;

/**
 * Process a Quit message.
 */
public class ProcessQuit extends IRCProcessor {

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     */
    public ProcessQuit(final IRCParser parser) {
        super(parser, "QUIT");
    }

    /**
     * Process a Quit message.
     *
     * @param sParam Type of line to process ("QUIT")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String... token) {
        // :nick!ident@host QUIT
        // :nick!ident@host QUIT :reason
        if (token.length < 2) {
            return;
        }

        final IRCClientInfo iClient = getClientInfo(token[0]);

        if (iClient == null) {
            return;
        }
        if (IRCParser.ALWAYS_UPDATECLIENT && iClient.getHostname().isEmpty()) {
            // This may seem pointless - updating before they leave - but the formatter needs it!
            iClient.setUserBits(token[0], false);
        }
        String sReason = "";
        if (token.length > 2) {
            sReason = token[token.length - 1];
        }

        final Iterable<IRCChannelInfo> channelList = new ArrayList<>(parser.getChannels());
        for (IRCChannelInfo iChannel : channelList) {
            final IRCChannelClientInfo iChannelClient = iChannel.getChannelClient(iClient);
            if (iChannelClient != null) {
                if (parser.getRemoveAfterCallback()) {
                    callChannelQuit(iChannel, iChannelClient, sReason);
                }
                if (iClient == parser.getLocalClient()) {
                    iChannel.emptyChannel();
                    parser.removeChannel(iChannel);
                } else {
                    iChannel.delClient(iClient);
                }
                if (!parser.getRemoveAfterCallback()) {
                    callChannelQuit(iChannel, iChannelClient, sReason);
                }
            }
        }

        if (parser.getRemoveAfterCallback()) {
            callQuit(iClient, sReason);
        }
        if (iClient == parser.getLocalClient()) {
            parser.clearClients();
        } else {
            parser.removeClient(iClient);
        }
        if (!parser.getRemoveAfterCallback()) {
            callQuit(iClient, sReason);
        }
    }

    /**
     * Callback to all objects implementing the ChannelQuit Callback.
     *
     * @param cChannel Channel that user was on
     * @param cChannelClient User thats quitting
     * @param sReason Quit reason
     */
    protected void callChannelQuit(final ChannelInfo cChannel,
            final ChannelClientInfo cChannelClient, final String sReason) {
        getCallbackManager().publish(
                new ChannelQuitEvent(parser, new Date(), cChannel, cChannelClient, sReason));
    }

    /**
     * Callback to all objects implementing the Quit Callback.
     *
     * @param cClient Client Quitting
     * @param sReason Reason for quitting (may be "")
     */
    protected void callQuit(final ClientInfo cClient, final String sReason) {
        getCallbackManager().publish(new QuitEvent(parser, new Date(), cClient, sReason));
    }

}
