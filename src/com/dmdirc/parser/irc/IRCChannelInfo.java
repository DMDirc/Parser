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

import com.dmdirc.parser.common.ChannelListModeItem;
import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.common.QueuePriority;
import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.ClientInfo;
import com.dmdirc.parser.interfaces.Parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Contains Channel information.
 *
 * @see IRCParser
 */
public class IRCChannelInfo implements ChannelInfo {

    /**
     * Boolean repreenting the status of names requests.
     * When this is false, any new names reply will cause current known channelclients to be removed.
     */
    private boolean addingNames = true;
    /** Unixtimestamp representing time when the channel was created. */
    private long creationTime;
    /** Current known topic in the channel. */
    private String topic = "";
    /** Last known user to set the topic (Full host where possible). */
    private String topicUser = "";
    /** Unixtimestamp representing time when the topic was set. */
    private long topicTime;
    /** Has this channel ever had a topic? */
    private boolean hadTopic;
    /** Known boolean-modes for channel. */
    private String modes;
    /** Reference to the parser object that owns this channel, Used for modes. */
    private final IRCParser parser;
    /** Mode manager to use for user modes. */
    private final ModeManager userModeManager;
    /** Mode manager to use for channel modes. */
    private final ModeManager chanModeManager;
    /** Mode manager to use for prefix mode information. */
    private final PrefixModeManager prefixModeManager;
    /** Channel Name. */
    private final String name;
    /** Hashtable containing references to ChannelClients. */
    private final Map<String, IRCChannelClientInfo> clients = Collections.synchronizedMap(new HashMap<String, IRCChannelClientInfo>());
    /** Hashtable storing values for modes set in the channel that use parameters. */
    private final Map<Character, String> paramModes = new HashMap<>();
    /** Hashtable storing list modes. */
    private final Map<Character, ArrayList<ChannelListModeItem>> listModes = new HashMap<>();
    /**
     * LinkedList storing status of mode adding.
     * if an item is in this list for a mode, we are expecting new items for the list
     */
    private final Collection<Character> addingModes = new LinkedList<>();
    /** Modes waiting to be sent to the server. */
    private final Collection<String> modeQueue = new LinkedList<>();
    /** A Map to allow applications to attach misc data to this object. */
    private final Map<Object, Object> map;
    /** Queue of requested list modes. */
    private final Queue<Character> listModeQueue = new LinkedList<>();
    /** Listmode Queue Time. */
    private long listModeQueueTime = System.currentTimeMillis();
    /** Have we asked the server for the list modes for this channel yet? */
    private boolean askedForListModes;
    /** Has OnChannelGotListModes ever been called for this channel? */
    private boolean hasGotListModes;

    /**
     * Create a new channel object.
     *
     * @param parser Reference to parser that owns this channelclient (used for modes)
     * @param prefixModeManager The manager to use for prefix modes.
     * @param userModeManager Mode manager to use for user modes.
     * @param chanModeManager Mode manager to use for channel modes.
     * @param name Channel name.
     */
    public IRCChannelInfo(final IRCParser parser, final PrefixModeManager prefixModeManager,
            final ModeManager userModeManager, final ModeManager chanModeManager,
            final String name) {
        map = new HashMap<>();
        this.parser = parser;
        this.prefixModeManager = prefixModeManager;
        this.userModeManager = userModeManager;
        this.chanModeManager = chanModeManager;
        this.name = name;
    }

    /**
     * Get the listModeQueue.
     *
     * @return The listModeQueue
     */
    public Queue<Character> getListModeQueue() {
        Queue<Character> result = listModeQueue;
        final long now = System.currentTimeMillis();
        // Incase of breakage, if getListModeQueue() was last called greater than
        // 60 seconds ago, we reset the list.
        if (now - 30 * 1000 > listModeQueueTime) {
            result = new LinkedList<>();
            parser.callDebugInfo(IRCParser.DEBUG_LMQ, "Resetting LMQ");
        }
        listModeQueueTime = now;
        return result;
    }

    /**
     * Ask the server for all the list modes for this channel.
     */
    @Override
    public void requestListModes() {
        final IRCChannelClientInfo me = getChannelClient(parser.getLocalClient());

        if (me == null) {
            // In a normal situation of non bouncer-brokenness this won't happen
            return;
        }

        askedForListModes = true;

        final ServerType serverType = parser.getServerType();

        final boolean isOpped = me.isOpped();

        int modecount = 1;

        if (!ServerTypeGroup.SINGLE_LISTMODE.isMember(serverType) && parser.h005Info.containsKey("MODES")) {
            try {
                modecount = Integer.parseInt(parser.h005Info.get("MODES"));
            } catch (NumberFormatException e) {
                modecount = 1;
            }
        }

        // Support for potential future decent mode listing in the protocol
        //
        // See my proposal: http://shane.dmdirc.com/listmodes.php
        // Add listmode handler
        final boolean supportLISTMODE = parser.h005Info.containsKey("LISTMODE");

        String listmodes = "";
        int i = 0;
        for (Character cTemp : parser.chanModesOther.keySet()) {
            final int nTemp = parser.chanModesOther.get(cTemp);
            if (nTemp == IRCParser.MODE_LIST) {
                if (!isOpped && serverType.isOpOnly(cTemp)) {
                    // IRCD doesn't allow non-ops to ask for these modes.
                    continue;
                } else if (serverType == ServerType.STARCHAT && cTemp == 'H') {
                    // IRCD Denies the mode exists
                    continue;
                }
                i++;
                listmodes = listmodes + cTemp;
                if (i >= modecount && !supportLISTMODE) {
                    parser.sendString("MODE " + getName() + " " + listmodes, QueuePriority.LOW);
                    i = 0;
                    listmodes = "";
                }
            }
        }
        if (i > 0) {
            if (supportLISTMODE) {
                parser.sendString("LISTMODE " + getName() + " " + listmodes, QueuePriority.LOW);
            } else {
                parser.sendString("MODE " + getName() + " " + listmodes, QueuePriority.LOW);
            }
        }
    }

    /**
     * Has this channel ever had a topic? (even an empty one!)
     *
     * @return True if a topic has ever been known for this channel.
     */
    public synchronized boolean hadTopic() {
        return hadTopic;
    }

    /**
     * Change the value of hadTopic to true.
     */
    public synchronized void setHadTopic() {
        this.hadTopic = true;
    }

    /**
     * Have we ever asked the server for this channels listmodes?
     *
     * @return True if requestListModes() has ever been used, else false
     */
    public boolean hasAskedForListModes() {
        return askedForListModes;
    }

    /**
     * Returns true if OnChannelGotListModes ever been called for this channel.
     *
     * @return True if OnChannelGotListModes ever been called for this channel.
     */
    public boolean hasGotListModes() {
        return hasGotListModes;
    }

    /**
     * Set if OnChannelGotListModes ever been called for this channel.
     *
     * @param newValue new value for if OnChannelGotListModes ever been called for this channel.
     */
    public void setHasGotListModes(final boolean newValue) {
        hasGotListModes = newValue;
    }

    @Override
    public Map<Object, Object> getMap() {
        return map;
    }

    /**
     * Set if we are getting a names request or not.
     *
     * @param newValue if false, any new names reply will cause current known channelclients to be removed.
     */
    public void setAddingNames(final boolean newValue) {
        addingNames = newValue;
    }

    /**
     * Get if we are getting a names request or not.
     *
     * @return if false, any new names reply will cause current known channelclients to be removed.
     */
    public boolean isAddingNames() {
        return addingNames;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getChannelClientCount() {
        return clients.size();
    }

    @Override
    public Collection<ChannelClientInfo> getChannelClients() {
        synchronized (clients) {
            return new ArrayList<ChannelClientInfo>(clients.values());
        }
    }

    /**
     * Empty the channel (Remove all known channelclients).
     */
    public void emptyChannel() {
        IRCClientInfo cTemp;
        synchronized (clients) {
            for (IRCChannelClientInfo client : clients.values()) {
                cTemp = client.getClient();
                cTemp.delChannelClientInfo(client);
                if (cTemp != parser.getLocalClient() && !cTemp.checkVisibility()) {
                    parser.removeClient(cTemp);
                }
            }
        }
        clients.clear();
    }

    @Override
    public IRCChannelClientInfo getChannelClient(final String client) {
        return getChannelClient(client, false);
    }

    @Override
    public IRCChannelClientInfo getChannelClient(final String client, final boolean create) {
        final String who = parser.getStringConverter().toLowerCase(IRCClientInfo.parseHost(client));
        if (clients.containsKey(who)) {
            return clients.get(who);
        }
        if (create) {
            return new IRCChannelClientInfo(parser, prefixModeManager,
                    new IRCClientInfo(parser, userModeManager, client).setFake(true), this);
        } else {
            return null;
        }
    }

    @Override
    public IRCChannelClientInfo getChannelClient(final ClientInfo client) {
        synchronized (clients) {
            for (IRCChannelClientInfo target : clients.values()) {
                if (target.getClient() == client) {
                    return target;
                }
            }
        }
        return null;
    }

    /**
     * Get the ChannelClientInfo object associated with a ClientInfo object.
     *
     * @param cClient Client object to be added to channel
     * @return ChannelClientInfo object added, or an existing object if already known on channel
     */
    public IRCChannelClientInfo addClient(final IRCClientInfo cClient) {
        IRCChannelClientInfo cTemp = getChannelClient(cClient);
        if (cTemp == null) {
            cTemp = new IRCChannelClientInfo(parser, prefixModeManager, cClient, this);
            clients.put(parser.getStringConverter().toLowerCase(cTemp.getClient().getNickname()), cTemp);
        }
        return cTemp;
    }

    /**
     * Remove ChannelClientInfo object associated with a ClientInfo object.
     *
     * @param cClient Client object to be removed from channel
     */
    public void delClient(final IRCClientInfo cClient) {
        final IRCChannelClientInfo cTemp = getChannelClient(cClient);
        if (cTemp != null) {
            final IRCClientInfo clTemp = cTemp.getClient();
            clTemp.delChannelClientInfo(cTemp);
            if (clTemp != parser.getLocalClient() && !clTemp.checkVisibility()) {
                parser.removeClient(clTemp);
            }
            clients.remove(parser.getStringConverter().toLowerCase(cTemp.getClient().getNickname()));
        }
    }

    /**
     * Rename a channelClient.
     *
     * @param oldNickname Nickname client used to be known as
     * @param cChannelClient ChannelClient object with updated client object
     */
    public void renameClient(final String oldNickname, final IRCChannelClientInfo cChannelClient) {
        if (clients.containsKey(oldNickname)) {
            final IRCChannelClientInfo cTemp = clients.get(oldNickname);
            if (cTemp == cChannelClient) {
                // Remove the old key
                clients.remove(oldNickname);
                // Add with the new key. (getNickname will return the new name not the
                // old one)
                clients.put(parser.getStringConverter().toLowerCase(cTemp.getClient().getNickname()), cTemp);
            }
        }
    }

    /**
     * Set the create time.
     *
     * @param nNewTime New unixtimestamp time for the channel creation (Seconds since epoch, not milliseconds)
     */
    public void setCreateTime(final long nNewTime) {
        creationTime = nNewTime;
    }

    /**
     * Get the Create time.
     *
     * @return Unixtimestamp time for the channel creation (Seconds since epoch, not milliseconds)
     */
    public long getCreateTime() {
        return creationTime;
    }

    /**
     * Set the topic time.
     *
     * @param nNewTime New unixtimestamp time for the topic (Seconds since epoch, not milliseconds)
     */
    public void setTopicTime(final long nNewTime) {
        topicTime = nNewTime;
    }

    @Override
    public long getTopicTime() {
        return topicTime;
    }

    /**
     * Set the topic.
     *
     * @param sNewTopic New contents of topic
     */
    public void setInternalTopic(final String sNewTopic) {
        topic = sNewTopic;
    }

    @Override
    public String getTopic() {
        return topic;
    }

    /**
     * Set the topic creator.
     *
     * @param sNewUser New user who set the topic (nickname if gotten on connect, full host if seen by parser)
     */
    public void setTopicUser(final String sNewUser) {
        topicUser = sNewUser;
    }

    @Override
    public String getTopicSetter() {
        return topicUser;
    }

    /**
     * Set the channel modes.
     *
     * @param nNewMode new boolean channel modes
     */
    public void setMode(final String nNewMode) {
        modes = nNewMode;
    }

    /**
     * Get the channel modes.
     *
     * @return the boolean channel modes.
     */
    public String getMode() {
        return modes;
    }

    @Override
    public String getModes() {
        final StringBuilder sModes = new StringBuilder("+");
        final StringBuilder sModeParams = new StringBuilder();
        sModes.append(modes);
        for (char cTemp : paramModes.keySet()) {
            final String sTemp = paramModes.get(cTemp);
            if (!sTemp.isEmpty()) {
                sModes.append(cTemp);
                sModeParams.append(' ').append(this.getMode(cTemp));
            }
        }

        return sModes.append(sModeParams).toString();
    }

    /**
     * Set a channel mode that requires a parameter.
     *
     * @param cMode Character representing mode
     * @param sValue String repreenting value (if "" mode is unset)
     */
    public void setModeParam(final Character cMode, final String sValue) {
        if (sValue.isEmpty()) {
            if (paramModes.containsKey(cMode)) {
                paramModes.remove(cMode);
            }
        } else {
            paramModes.put(cMode, sValue);
        }
    }

    @Override
    public String getMode(final char mode) {
        if (paramModes.containsKey(mode)) {
            return paramModes.get(mode);
        }
        return "";
    }

    /**
     * Add/Remove a value to a channel list.
     *
     * @param givenMode Character representing mode
     * @param givenItem ChannelListModeItem representing the item
     * @param bAdd Add or remove the value. (true for add, false for remove)
     */
    public void setListModeParam(final Character givenMode, final ChannelListModeItem givenItem,
            final boolean bAdd) {
        Character cMode = givenMode;
        ChannelListModeItem newItem = givenItem;
        if (!parser.chanModesOther.containsKey(cMode) || parser.chanModesOther.get(cMode) != IRCParser.MODE_LIST) {
            return;
        }

        // Hyperion sucks.
        if (cMode == 'b' || cMode == 'q') {
            final ServerType serverType = parser.getServerType();
            if (ServerTypeGroup.FREENODE.isMember(serverType)) {
                if (cMode == 'b' && givenItem.getItem().charAt(0) == '%') {
                    cMode = 'q';
                } else if (cMode == 'q' && givenItem.getItem().charAt(0) != '%') {
                    cMode = 'b';
                }
                if (givenItem.getItem().charAt(0) == '%') {
                    newItem = new ChannelListModeItem(givenItem.getItem().substring(1), givenItem.getOwner(), givenItem.getTime());
                }
            }
        }

        if (!listModes.containsKey(cMode)) {
            listModes.put(cMode, new ArrayList<ChannelListModeItem>());
        }

        final List<ChannelListModeItem> lModes = listModes.get(cMode);
        for (int i = 0; i < lModes.size(); i++) {
            if (parser.getStringConverter().equalsIgnoreCase(lModes.get(i).getItem(), newItem.getItem())) {
                if (bAdd) {
                    return;
                } else {
                    lModes.remove(i);
                    break;
                }
            }
        }
        if (bAdd) {
            lModes.add(newItem);
        }
    }

    @Override
    public Collection<ChannelListModeItem> getListMode(final char mode) {
        if (!parser.chanModesOther.containsKey(mode) || parser.chanModesOther.get(mode) != IRCParser.MODE_LIST) {
            return null;
        }

        if (!listModes.containsKey(mode)) {
            listModes.put(mode, new ArrayList<ChannelListModeItem>());
        }
        return listModes.get(mode);
    }

    /**
     * Get the "adding state" of a list mode.
     *
     * @param cMode Character representing mode
     * @return false if we are not expecting a 367 etc, else true.
     */
    public boolean getAddState(final Character cMode) {
        synchronized (addingModes) {
            return addingModes.contains(cMode);
        }
    }

    /**
     * Get the "adding state" of a list mode.
     *
     * @param cMode Character representing mode
     * @param newState change the value returned by getAddState
     */
    public void setAddState(final Character cMode, final boolean newState) {
        synchronized (addingModes) {
            if (newState) {
                addingModes.add(cMode);
            } else {
                if (addingModes.contains(cMode)) {
                    addingModes.remove(cMode);
                }
            }
        }
    }

    /**
     * Reset the "adding state" of *all* list modes.
     */
    public void resetAddState() {
        synchronized (addingModes) {
            addingModes.clear();
        }
    }

    @Override
    public void alterMode(final boolean add, final Character mode, final String parameter) {
        int modecount = 1;
        final int modeint;
        String modestr;
        if (parser.h005Info.containsKey("MODES")) {
            try {
                modecount = Integer.parseInt(parser.h005Info.get("MODES"));
            } catch (NumberFormatException e) {
                if (parser.getServerType() == ServerType.OTHERNET) {
                    modecount = 6;
                } else {
                    modecount = 1;
                }
            }
        }
        if (!parser.isUserSettable(mode)) {
            return;
        }

        modestr = (add ? "+" : "-") + mode;
        if (chanModeManager.isMode(mode)) {
            final String teststr = (add ? "-" : "+") + mode;
            if (modeQueue.contains(teststr)) {
                modeQueue.remove(teststr);
                return;
            } else if (modeQueue.contains(modestr)) {
                return;
            }
        } else {
            // May need a param
            if (prefixModeManager.isPrefixMode(mode)) {
                modestr = modestr + ' ' + parameter;
            } else if (parser.chanModesOther.containsKey(mode)) {
                modeint = parser.chanModesOther.get(mode);
                if ((modeint & IRCParser.MODE_LIST) == IRCParser.MODE_LIST) {
                    modestr = modestr + " " + parameter;
                } else if (!add && (modeint & IRCParser.MODE_UNSET) == IRCParser.MODE_UNSET) {
                    modestr = modestr + " " + parameter;
                } else if (add && (modeint & IRCParser.MODE_SET) == IRCParser.MODE_SET) {
                    // Does mode require a param to unset aswell?
                    // We might need to queue an unset first
                    if ((modeint & IRCParser.MODE_UNSET) == IRCParser.MODE_UNSET) {
                        final String existingParam = getMode(mode);
                        if (!existingParam.isEmpty()) {
                            final String reverseModeStr = "-" + mode + " " + existingParam;

                            parser.callDebugInfo(IRCParser.DEBUG_INFO, "Queueing mode: %s", reverseModeStr);
                            modeQueue.add(reverseModeStr);
                            if (modeQueue.size() == modecount) {
                                flushModes();
                            }
                        }
                    }
                    modestr = modestr + " " + parameter;
                }
            } else {
                parser.callErrorInfo(new ParserError(ParserError.ERROR_WARNING, "Trying to alter unknown mode.  positive: '" + add + "' | mode: '" + mode + "' | parameter: '" + parameter + "' ", ""));
            }
        }
        parser.callDebugInfo(IRCParser.DEBUG_INFO, "Queueing mode: %s", modestr);
        modeQueue.add(modestr);
        if (modeQueue.size() == modecount) {
            flushModes();
        }
    }

    @Override
    public void flushModes() {
        if (modeQueue.isEmpty()) {
            return;
        }
        final StringBuilder positivemode = new StringBuilder();
        final StringBuilder positiveparam = new StringBuilder();
        final StringBuilder negativemode = new StringBuilder();
        final StringBuilder negativeparam = new StringBuilder();
        final StringBuilder sendModeStr = new StringBuilder();
        String modestr;
        String[] modeparam;
        boolean positive;
        for (String aModeQueue : modeQueue) {
            modeparam = aModeQueue.split(" ");
            modestr = modeparam[0];
            positive = modestr.charAt(0) == '+';
            if (positive) {
                positivemode.append(modestr.charAt(1));
                if (modeparam.length > 1) {
                    positiveparam.append(' ').append(modeparam[1]);
                }
            } else {
                negativemode.append(modestr.charAt(1));
                if (modeparam.length > 1) {
                    negativeparam.append(' ').append(modeparam[1]);
                }
            }
        }
        if (negativemode.length() > 0) {
            sendModeStr.append('-').append(negativemode);
        }
        if (positivemode.length() > 0) {
            sendModeStr.append('+').append(positivemode);
        }
        if (negativeparam.length() > 0) {
            sendModeStr.append(negativeparam);
        }
        if (positiveparam.length() > 0) {
            sendModeStr.append(positiveparam);
        }
        parser.callDebugInfo(IRCParser.DEBUG_INFO, "Sending mode: %s", sendModeStr.toString());
        parser.sendRawMessage("MODE " + name + ' ' + sendModeStr);
        clearModeQueue();
    }

    /**
     * This function will clear the mode queue (WITHOUT Sending).
     */
    public void clearModeQueue() {
        modeQueue.clear();
    }

    @Override
    public void sendMessage(final String message) {
        if (message.isEmpty()) {
            return;
        }

        parser.sendString("PRIVMSG " + name, message);
    }

    /**
     * Send a notice message to a target.
     *
     * @param sMessage Message to send
     */
    public void sendNotice(final String sMessage) {
        if (sMessage.isEmpty()) {
            return;
        }

        parser.sendString("NOTICE " + name, sMessage);
    }

    @Override
    public void sendAction(final String action) {
        if (action.isEmpty()) {
            return;
        }
        sendCTCP("ACTION", action);
    }

    /**
     * Send a CTCP to a target.
     *
     * @param sType Type of CTCP
     * @param sMessage Optional Additional Parameters
     */
    public void sendCTCP(final String sType, final String sMessage) {
        if (sType.isEmpty()) {
            return;
        }
        final char char1 = (char) 1;
        if (sMessage.isEmpty()) {
            sendMessage(char1 + sType.toUpperCase() + sMessage + char1);
        } else {
            sendMessage(char1 + sType.toUpperCase() + ' ' + sMessage + char1);
        }
    }

    /**
     * Send a CTCPReply to a target.
     *
     * @param sType Type of CTCP
     * @param sMessage Optional Additional Parameters
     */
    public void sendCTCPReply(final String sType, final String sMessage) {
        if (sType.isEmpty()) {
            return;
        }
        final char char1 = (char) 1;
        if (sMessage.isEmpty()) {
            sendNotice(char1 + sType.toUpperCase() + sMessage + char1);
        } else {
            sendNotice(char1 + sType.toUpperCase() + ' ' + sMessage + char1);
        }
    }

    /**
     * Get a string representation of the Channel.
     *
     * @return String representation of the Channel.
     */
    @Override
    public String toString() {
        return name;
    }

    @Override
    public Parser getParser() {
        return parser;
    }

    @Override
    public void part(final String reason) {
        parser.partChannel(name, reason);
    }

    @Override
    public void setTopic(final String topic) {
        parser.sendRawMessage("TOPIC " + name + " :" + topic);
    }

    @Override
    public void sendWho() {
        parser.sendRawMessage("WHO " + name);
    }
}
