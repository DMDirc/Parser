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
import com.dmdirc.parser.common.QueuePriority;
import com.dmdirc.parser.events.ChannelJoinEvent;
import com.dmdirc.parser.events.ChannelSelfJoinEvent;
import com.dmdirc.parser.events.DataOutEvent;
import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.irc.CapabilityState;
import com.dmdirc.parser.irc.IRCChannelClientInfo;
import com.dmdirc.parser.irc.IRCChannelInfo;
import com.dmdirc.parser.irc.IRCClientInfo;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.ModeManager;
import com.dmdirc.parser.irc.PrefixModeManager;
import com.dmdirc.parser.irc.ProcessorNotFoundException;
import com.dmdirc.parser.irc.events.IRCDataOutEvent;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Invoke;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Process a channel join.
 */
public class ProcessJoin extends IRCProcessor {

    /** The manager to use to access prefix modes. */
    private final PrefixModeManager prefixModeManager;
    /** Mode manager to use for user modes. */
    private final ModeManager userModeManager;
    /** Mode manager to use for channel modes. */
    private final ModeManager chanModeManager;
    /** Pending Joins. */
    private final Queue<String> pendingJoins = new LinkedList<>();
    /** Pending Join Keys. */
    private final Queue<String> pendingJoinKeys = new LinkedList<>();

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param prefixModeManager The manager to use to access prefix modes.
     * @param userModeManager Mode manager to use for user modes.
     * @param chanModeManager Mode manager to use for channel modes.
     */
    @Inject
    public ProcessJoin(final IRCParser parser, final PrefixModeManager prefixModeManager,
            @Named("user") final ModeManager userModeManager,
            @Named("channel") final ModeManager chanModeManager) {
        super(parser, "JOIN", "329", "471", "473", "474", "475", "476", "477", "479");
        this.prefixModeManager = prefixModeManager;
        this.userModeManager = userModeManager;
        this.chanModeManager = chanModeManager;

        getCallbackManager().subscribe(this);
    }

    /**
     * Process a channel join.
     *
     * @param sParam Type of line to process ("JOIN")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String... token) {
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
        } else if ("JOIN".equals(sParam)) {
            // :nick!ident@host JOIN (:)#Channel
            if (token.length < 3) {
                return;
            }
            final boolean extendedJoin = parser.getCapabilityState("extended-join") == CapabilityState.ENABLED;

            IRCClientInfo iClient = getClientInfo(token[0]);
            final String realName;
            final String accountName;
            final String channelName;
            if (extendedJoin) {
                // :nick!ident@host JOIN #Channel accountName :Real Name
                channelName = token[2];
                accountName = token.length > 3 ? token[3] : "*";
                realName = token.length > 4 ? token[token.length - 1] : "";
            } else {
                channelName = token[token.length - 1];
                accountName = "*";
                realName = "";
            }
            IRCChannelInfo iChannel = parser.getChannel(token[2]);

            callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: client: %s", iClient);
            callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: channel: %s", iChannel);

            if (iClient == null) {
                iClient = new IRCClientInfo(parser, userModeManager, token[0]);
                parser.addClient(iClient);
                callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: new client.", iClient);
            }

            if (extendedJoin) {
                iClient.setAccountName("*".equals(accountName) ? null : accountName);
                iClient.setRealName(realName);
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
                    final IRCChannelClientInfo iChannelClient = iChannel.addClient(iClient);
                    callChannelJoin(iChannel, iChannelClient);
                    callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: Added client to channel.");
                    return;
                } else {
                    // Client joined channel that we already know of.
                    callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: Not adding client to channel they are already on.");
                    return;
                }
            }

            iChannel = new IRCChannelInfo(parser, prefixModeManager, userModeManager,
                    chanModeManager, channelName);
            // Add ourself to the channel, this will be overridden by the NAMES reply
            iChannel.addClient(iClient);
            parser.addChannel(iChannel);
            sendString("MODE " + iChannel.getName(), QueuePriority.LOW);

            final String pendingJoin = pendingJoins.poll();
            final String pendingJoinKey = pendingJoinKeys.poll();
            if (pendingJoin != null && pendingJoin.equalsIgnoreCase(channelName)) {
                callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: Guessing channel Key: " + pendingJoin + " -> " + pendingJoinKey);
                iChannel.setInternalPassword(pendingJoinKey == null ? "" : pendingJoinKey);
            } else {
                // Out of sync, clear
                callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: pending join keys out of sync (Got: " + pendingJoin + ", Wanted: " + channelName + ") - Clearing.");
                pendingJoins.clear();
                pendingJoinKeys.clear();
            }

            callChannelSelfJoin(iChannel);
        } else {
            // Some kind of failed to join, pop the pending join queues.
            final String pendingJoin = pendingJoins.poll();
            final String pendingJoinKey = pendingJoinKeys.poll();
            callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: Failed to join channel (" + sParam + ") - Skipping " + pendingJoin + " (" + pendingJoinKey + ")");
        }
    }

    @Handler(delivery = Invoke.Synchronously, condition = "msg.action == 'JOIN'")
    public void handleDataOut(final IRCDataOutEvent event) {
        // As long as this is called before the resulting DataIn
        // Processors fire then this will work, otherwise we'll end
        // up with an out-of-sync pendingJoins list.

        final String[] newLine = event.getTokenisedData();
        if (newLine.length > 1) {
            final Queue<String> keys = new LinkedList<>();

            if (newLine.length > 2) {
                keys.addAll(Arrays.asList(newLine[2].split(",")));
            }

            // PendingJoins and PendingJoinKeys should always be the same length (even if no key was given).
            //
            // We don't get any errors for channels we try to join that we are already in
            // But the IRCD will still swallow the key attempt.
            //
            // Make sure that we always have a guessed key for every channel (even if null) and that we
            // don't have guesses for channels we are already in.
            for (final String chan : newLine[1].split(",")) {
                final String key = keys.poll();
                if (getChannel(chan) == null) {
                    callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: Intercepted possible channel Key: " + chan + " -> " + key);
                    pendingJoins.add(chan);
                    pendingJoinKeys.add(key);
                } else {
                    callDebugInfo(IRCParser.DEBUG_INFO, "processJoin: Ignoring possible channel Key for existing channel: " + chan + " -> " + key);
                }
            }
        }
    }

    /**
     * Callback to all objects implementing the ChannelJoin Callback.
     *
     * @param cChannel Channel Object
     * @param cChannelClient ChannelClient object for new person
     */
    protected void callChannelJoin(final ChannelInfo cChannel,
            final ChannelClientInfo cChannelClient) {
        getCallbackManager().publish(
                new ChannelJoinEvent(parser, LocalDateTime.now(), cChannel, cChannelClient));
    }

    /**
     * Callback to all objects implementing the ChannelSelfJoin Callback.
     *
     * @param cChannel Channel Object
     */
    protected void callChannelSelfJoin(final ChannelInfo cChannel) {
        getCallbackManager().publish(new ChannelSelfJoinEvent(
                parser, LocalDateTime.now(), cChannel));
    }

}
