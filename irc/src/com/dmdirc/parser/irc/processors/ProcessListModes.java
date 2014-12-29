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
package com.dmdirc.parser.irc.processors;

import com.dmdirc.parser.common.ChannelListModeItem;
import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.callbacks.ChannelListModeListener;
import com.dmdirc.parser.irc.IRCChannelInfo;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.ProcessingManager;
import com.dmdirc.parser.irc.ServerType;
import com.dmdirc.parser.irc.ServerTypeGroup;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Queue;

/**
 * Process a List Modes.
 */
public class ProcessListModes extends IRCProcessor {

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param manager ProcessingManager that is in charge of this IRCProcessor
     */
    public ProcessListModes(final IRCParser parser, final ProcessingManager manager) {
        super(parser, manager,
                "367", "368", /* Bans */
                "344", "345", /* Reop list (ircnet) or bad words (euirc) */
                "346", "347", /* Invite List */
                "348", "349", /* Except/Exempt List */
                "386", "387", /* Channel Owner List (swiftirc ) */
                "388", "389", /* Protected User List (swiftirc) */
                "940", "941", /* Censored words list */
                "910", "911", /* INSPIRCD Channel Access List. */
                "954", "953", /* INSPIRCD exemptchanops List. */
                "482",        /* Permission Denied */
                "__LISTMODE__" /* Sensible List Modes */
        );
    }

    /**
     * Process a ListModes.
     *
     * @param sParam Type of line to process
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String... token) {
        final IRCChannelInfo channel = getChannel(token[3]);
        final ServerType serverType = parser.getServerType();
        if (channel == null) {
            return;
        }

        boolean isItem = true; // true if item listing, false if "end of .." item
        char mode = ' ';
        boolean isCleverMode = false;
        byte tokenStart = 4; // Where do the relevent tokens start?
        if ("367".equals(sParam) || "368".equals(sParam)) {
            // Ban List/Item.
            // (Also used for +d and +q on dancer/hyperion... -_-)
            // (Also used for +q on ircd-seven... -_-)
            mode = 'b';
            isItem = "367".equals(sParam);
        } else if ("348".equals(sParam) || "349".equals(sParam)) {
            // Except / Exempt List etc
            mode = 'e';
            isItem = "348".equals(sParam);
        } else if ("346".equals(sParam) || "347".equals(sParam)) {
            // Invite List
            mode = 'I';
            isItem = "346".equals(sParam);
        } else if ("940".equals(sParam) || "941".equals(sParam)) {
            // Censored words List
            mode = 'g';
            isItem = "941".equals(sParam);
        } else if (serverType == ServerType.INSPIRCD && ("910".equals(sParam) ||
                "911".equals(sParam))) {
            // Channel Access List
            mode = 'w';
            isItem = "910".equals(sParam);
        } else if (serverType == ServerType.INSPIRCD && ("954".equals(sParam) ||
                "953".equals(sParam))) {
            // Channel exemptchanops List
            mode = 'X';
            isItem = "954".equals(sParam);
        } else if ("344".equals(sParam) || "345".equals(sParam)) {
            // Reop List, or bad words list, or quiet list. god damn.
            if (serverType == ServerType.EUIRCD) {
                mode = 'w';
            } else if (serverType == ServerType.OFTC_HYBRID) {
                mode = 'q';
            } else {
                mode = 'R';
            }
            isItem = "344".equals(sParam);
        } else if (ServerTypeGroup.OWNER_386.isMember(serverType) && ("386".equals(sParam) ||
                "387".equals(sParam))) {
            // Channel Owner list
            mode = 'q';
            isItem = "387".equals(sParam);
        } else if (ServerTypeGroup.PROTECTED_388.isMember(serverType) && ("388".equals(sParam) ||
                "389".equals(sParam))) {
            // Protected User list
            mode = 'a';
            isItem = "389".equals(sParam);
        } else if (sParam.equals(parser.h005Info.get("LISTMODE")) || sParam.equals(parser.h005Info.get("LISTMODEEND"))) {
            // Support for potential future decent mode listing in the protocol
            //
            // See my proposal: http://shane.dmdirc.com/listmodes.php
            mode = token[4].charAt(0);
            isItem = sParam.equals(parser.h005Info.get("LISTMODE"));
            tokenStart = 5;
            isCleverMode = true;
        }

        // Unknown mode.
        if (mode == ' ') {
            parser.callDebugInfo(IRCParser.DEBUG_LMQ, "Unknown mode line: " + Arrays.toString(token));
            return;
        }

        final Queue<Character> listModeQueue = channel.getListModeQueue();
        if (!isCleverMode && listModeQueue != null) {
            if ("482".equals(sParam)) {
                parser.callDebugInfo(IRCParser.DEBUG_LMQ, "Dropped LMQ mode " + listModeQueue.poll());
                return;
            } else {
                if (listModeQueue.peek() != null) {
                    final Character oldMode = mode;
                    mode = listModeQueue.peek();
                    parser.callDebugInfo(IRCParser.DEBUG_LMQ, "LMQ says this is " + mode);

                    boolean error = true;

                    // b or q are given in the same list, d is separate.
                    // b and q remove each other from the LMQ.
                    //
                    // Only raise an LMQ error if the lmqmode isn't one of bdq if the
                    // guess is one of bdq
                    if (ServerTypeGroup.FREENODE.isMember(serverType) && (mode == 'b' || mode == 'q' || mode == 'd')) {
                        if (mode == 'b') {
                            error = oldMode != 'q' && oldMode != 'd';
                            listModeQueue.remove('q');
                            parser.callDebugInfo(IRCParser.DEBUG_LMQ, "Dropping q from list");
                        } else if (mode == 'q') {
                            error = oldMode != 'b' && oldMode != 'd';
                            listModeQueue.remove('b');
                            parser.callDebugInfo(IRCParser.DEBUG_LMQ, "Dropping b from list");
                        } else if (mode == 'd') {
                            error = oldMode != 'b' && oldMode != 'q';
                        }
                    } else if (ServerTypeGroup.CHARYBDIS.isMember(serverType) && (mode == 'b' || mode == 'q')) {
                        // Finally freenode appear to have an ircd which isn't completely annoying.
                        // Only error if the LMQ thinks the mode should be
                        // something thats not the other one of these 2 modes.
                        error = oldMode != (mode == 'b' ? 'q' : 'b');
                    }

                    // If the lmq and the actual mode are not the same or the
                    // freenode-specific hacks above think the mode should be
                    // something else, error.
                    if (oldMode != mode && error) {
                        parser.callDebugInfo(IRCParser.DEBUG_LMQ, "LMQ disagrees with guess. LMQ: " + mode + " Guess: " + oldMode);
                    }

                    if (!isItem) {
                        listModeQueue.poll();
                    }
                }
            }
        }

        if (isItem) {
            if (!isCleverMode && listModeQueue == null && ServerTypeGroup.FREENODE.isMember(serverType) && token.length > 4 && mode == 'b') {
                // Assume mode is a 'd' mode
                mode = 'd';
                // Now work out if its not (or attempt to.)
                final int identstart = token[tokenStart].indexOf('!');
                final int hoststart = token[tokenStart].indexOf('@');
                // Check that ! and @ are both in the string - as required by +b and +q
                if (identstart >= 0 && identstart < hoststart) {
                    if (serverType == ServerType.HYPERION && token[tokenStart].charAt(0) == '%') {
                        mode = 'q';
                    } else {
                        mode = 'b';
                    }
                }
            } // End Hyperian stupidness of using the same numeric for 3 different things..

            if (!channel.getAddState(mode)) {
                callDebugInfo(IRCParser.DEBUG_INFO, "New List Mode Batch (" + mode + "): Clearing!");
                final Collection<ChannelListModeItem> list = channel.getListMode(mode);
                if (list == null) {
                    parser.callErrorInfo(new ParserError(ParserError.ERROR_WARNING, "Got list mode: '" + mode + "' - but channel object doesn't agree.", parser.getLastLine()));
                } else {
                    list.clear();
                    if (ServerTypeGroup.FREENODE.isMember(serverType) && (mode == 'b' || mode == 'q')) {
                        // Also clear the other list if b or q.
                        final Character otherMode = mode == 'b' ? 'q' : 'b';

                        if (!channel.getAddState(otherMode)) {
                            callDebugInfo(IRCParser.DEBUG_INFO, "New List Mode Batch (" + mode + "): Clearing!");
                            final Collection<ChannelListModeItem> otherList = channel.getListMode(otherMode);
                            if (otherList == null) {
                                parser.callErrorInfo(new ParserError(ParserError.ERROR_WARNING, "Got list mode: '" + otherMode + "' - but channel object doesn't agree.", parser.getLastLine()));
                            } else {
                                otherList.clear();
                            }
                        }
                    }
                }
                channel.setAddState(mode, true);
            }

            long time = 0;
            if (token.length > tokenStart + 2) {
                try {
                    time = Long.parseLong(token[tokenStart + 2]);
                } catch (NumberFormatException e) {
                    time = 0;
                }
            }
            String owner = "";
            if (token.length > tokenStart + 1) {
                owner = token[tokenStart + 1];
            }
            String item = "";
            if (token.length > tokenStart) {
                item = token[tokenStart];
            }
            if (!item.isEmpty()) {
                final ChannelListModeItem clmi = new ChannelListModeItem(item, owner, time);
                callDebugInfo(IRCParser.DEBUG_INFO, "List Mode: %c [%s/%s/%d]", mode, item, owner, time);
                channel.setListModeParam(mode, clmi, true);
            }
        } else {
            callDebugInfo(IRCParser.DEBUG_INFO, "List Mode Batch over");
            channel.resetAddState();
            if (isCleverMode || listModeQueue == null || listModeQueue.isEmpty()) {
                callDebugInfo(IRCParser.DEBUG_INFO, "Calling GotListModes");
                channel.setHasGotListModes(true);

                if (isCleverMode) {
                    parser.chanModesOther.keySet()
                            .stream()
                            .filter(thisMode -> parser.chanModesOther
                                    .get(thisMode) == IRCParser.MODE_LIST)
                            .forEach(thisMode -> callChannelGotListModes(channel, thisMode));
                } else {
                    callChannelGotListModes(channel, mode);
                }
            }
        }
    }

    /**
     * Callback to all objects implementing the ChannelGotListModes Callback.
     *
     * @see ChannelListModeListener
     * @param cChannel Channel which the ListModes reply is for
     * @param mode the mode that we got list modes for.
     */
    protected void callChannelGotListModes(final ChannelInfo cChannel, final char mode) {
        getCallback(ChannelListModeListener.class)
                .onChannelGotListModes(parser, new Date(), cChannel, mode);
    }
}
