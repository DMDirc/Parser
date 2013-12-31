/*
 * Copyright (c) 2006-2014 DMDirc Developers
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

import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.callbacks.ChannelTopicListener;

/**
 * Process a topic change.
 */
public class ProcessTopic extends IRCProcessor {

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param manager ProcessingManager that is in charge of this IRCProcessor
     */
    protected ProcessTopic(final IRCParser parser, final ProcessingManager manager) {
        super(parser, manager);
    }

    /**
     * Process a topic change.
     *
     * @param sParam Type of line to process ("TOPIC", "332", "333")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String[] token) {
        IRCChannelInfo iChannel;
        switch (sParam) {
            case "332":
                iChannel = getChannel(token[3]);
                if (iChannel == null) {
                    return;
                }   iChannel.setInternalTopic(token[token.length - 1]);
                break;
            case "333":
                if (token.length > 3) {
                    iChannel = getChannel(token[3]);
                    if (iChannel == null) {
                        return;
                    }
                    if (token.length > 4) {
                        iChannel.setTopicUser(token[4]);
                        if (token.length > 5) {
                            iChannel.setTopicTime(Long.parseLong(token[5]));
                        }
                    }
                    callChannelTopic(iChannel, true);
                }   break;
            default:
                if (IRCParser.ALWAYS_UPDATECLIENT) {
                    final IRCClientInfo iClient = getClientInfo(token[0]);
                    if (iClient != null && iClient.getHostname().isEmpty()) {
                        iClient.setUserBits(token[0], false);
                    }
                }
                iChannel = getChannel(token[2]);
                if (iChannel == null) {
                    return;
                }
                iChannel.setTopicTime(System.currentTimeMillis() / 1000);
                if (token[0].charAt(0) == ':') {
                    token[0] = token[0].substring(1);
                }
                iChannel.setTopicUser(token[0]);
                iChannel.setInternalTopic(token[token.length - 1]);
                callChannelTopic(iChannel, false);
                break;
        }
    }

    /**
     * Callback to all objects implementing the ChannelTopic Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.ChannelTopicListener
     * @param cChannel Channel that topic was set on
     * @param bIsJoinTopic True when getting topic on join, false if set by user/server
     * @return true if a method was called, false otherwise
     */
    protected boolean callChannelTopic(final ChannelInfo cChannel, final boolean bIsJoinTopic) {
        ((IRCChannelInfo) cChannel).setHadTopic();
        return getCallbackManager().getCallbackType(ChannelTopicListener.class).call(cChannel, bIsJoinTopic);
    }

    /**
     * What does this IRCProcessor handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"TOPIC", "332", "333"};
    }
}
