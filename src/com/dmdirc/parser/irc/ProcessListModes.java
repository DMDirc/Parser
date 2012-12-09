/*
 * Copyright (c) 2006-2012 DMDirc Developers
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

import com.dmdirc.parser.common.ChannelListModeItem;
import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.callbacks.ChannelListModeListener;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
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
    protected ProcessListModes(final IRCParser parser, final ProcessingManager manager) {
        super(parser, manager);
    }

    /**
     * Process a ListModes.
     *
     * @param sParam Type of line to process
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String[] token) {
        final IRCChannelInfo channel = getChannel(token[3]);
        final ServerType serverType = parser.getServerType();
        String item = "";
        String owner = "";
        byte tokenStart = 4; // Where do the relevent tokens start?
        boolean isCleverMode = false;
        long time = 0;
        char mode = ' ';
        boolean isItem = true; // true if item listing, false if "end of .." item
        if (channel == null) {
            return;
        }

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
        } else if (sParam.equals("346") || sParam.equals("347")) {
            // Invite List
            mode = 'I';
            isItem = sParam.equals("346");
        } else if (sParam.equals("940") || sParam.equals("941")) {
            // Censored words List
            mode = 'g';
            isItem = sParam.equals("941");
        } else if ((serverType == ServerType.INSPIRCD) && (sParam.equals("910") || sParam.equals("911"))) {
            // Channel Access List
            mode = 'w';
            isItem = sParam.equals("910");
        } else if ((serverType == ServerType.INSPIRCD) && (sParam.equals("954") || sParam.equals("953"))) {
            // Channel exemptchanops List
            mode = 'X';
            isItem = sParam.equals("954");
        } else if (sParam.equals("344") || sParam.equals("345")) {
            // Reop List, or bad words list, or quiet list. god damn.
            if (serverType == ServerType.EUIRCD) {
                mode = 'w';
            } else if (serverType == ServerType.OFTC_HYBRID) {
                mode = 'q';
            } else {
                mode = 'R';
            }
            isItem = sParam.equals("344");
        } else if (ServerTypeGroup.OWNER_386.isMember(serverType) && (sParam.equals("386") || sParam.equals("387"))) {
            // Channel Owner list
            mode = 'q';
            isItem = sParam.equals("387");
        } else if (ServerTypeGroup.PROTECTED_388.isMember(serverType) && (sParam.equals("388") || sParam.equals("389"))) {
            // Protected User list
            mode = 'a';
            isItem = sParam.equals("389");
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
                if ((identstart >= 0) && (identstart < hoststart)) {
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
                        final Character otherMode = (mode == 'b') ? 'q' : 'b';

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

            if (token.length > (tokenStart + 2)) {
                try {
                    time = Long.parseLong(token[tokenStart + 2]);
                } catch (NumberFormatException e) {
                    time = 0;
                }
            }
            if (token.length > (tokenStart + 1)) {
                owner = token[tokenStart + 1];
            }
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
            if (isCleverMode || listModeQueue == null || ((LinkedList<Character>) listModeQueue).size() == 0) {
                callDebugInfo(IRCParser.DEBUG_INFO, "Calling GotListModes");
                channel.setHasGotListModes(true);

                if (isCleverMode) {
                    for (Character thisMode : parser.chanModesOther.keySet()) {
                        if (parser.chanModesOther.get(thisMode) == IRCParser.MODE_LIST) {
                            callChannelGotListModes(channel, thisMode);
                        }
                    }
                } else {
                    callChannelGotListModes(channel, mode);
                }
            }
        }
    }

    /**
     * What does this IRCProcessor handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"367", "368", /* Bans */
                    "344", "345", /* Reop list (ircnet) or bad words (euirc) */
                    "346", "347", /* Invite List */
                    "348", "349", /* Except/Exempt List */
                    "386", "387", /* Channel Owner List (swiftirc ) */
                    "388", "389", /* Protected User List (swiftirc) */
                    "940", "941", /* Censored words list */
                    "910", "911", /* INSPIRCD Channel Access List. */
                    "954", "953", /* INSPIRCD exemptchanops List. */
                    "482", /* Permission Denied */
                    "__LISTMODE__" /* Sensible List Modes */};
    }

    /**
     * Callback to all objects implementing the ChannelGotListModes Callback.
     *
     * @see IChannelGotListModes
     * @param cChannel Channel which the ListModes reply is for
     * @param mode the mode that we got list modes for.
     * @return true if a method was called, false otherwise
     */
    protected boolean callChannelGotListModes(final ChannelInfo cChannel, final char mode) {
        return getCallbackManager().getCallbackType(ChannelListModeListener.class).call(cChannel, mode);
    }
}
