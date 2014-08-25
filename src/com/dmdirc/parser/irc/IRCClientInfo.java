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

import com.dmdirc.parser.common.AwayState;
import com.dmdirc.parser.interfaces.LocalClientInfo;
import com.dmdirc.parser.interfaces.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Contains information about known users.
 *
 * @see IRCParser
 */
public class IRCClientInfo implements LocalClientInfo {

    /** Known nickname of client. */
    private String nickname = "";
    /** Known ident of client. */
    private String ident = "";
    /** Known host of client. */
    private String host = "";
    /** Known user modes of client. */
    private long modes;
    /** Known Away Reason of client. */
    private String awayReason = "";
    /** Known Account name of client. */
    private String accountName = "*";
    /** Known RealName of client. */
    private String realName = "";
    /** Known away state for client. */
    private AwayState away = AwayState.UNKNOWN;
    /** Is this a fake client created just for a callback? */
    private boolean fake;
    /** Reference to the parser object that owns this channel, Used for modes. */
    private final IRCParser parser;
    /** A Map to allow applications to attach misc data to this object. */
    private final Map<Object, Object> map;
    /** List of ChannelClientInfos that point to this. */
    private final Map<String, IRCChannelClientInfo> clients = new HashMap<>();
    /** Modes waiting to be sent to the server. */
    private final List<String> modeQueue = new LinkedList<>();

    /**
     * Create a new client object from a hostmask.
     *
     * @param tParser Refernce to parser that owns this channelclient (used for modes)
     * @param sHostmask Hostmask parsed by parseHost to get nickname
     * @see IRCClientInfo#parseHost
     */
    public IRCClientInfo(final IRCParser tParser, final String sHostmask) {
        map = new HashMap<>();
        setUserBits(sHostmask, true);
        parser = tParser;
    }

    /** {@inheritDoc} */
    @Override
    public Map<Object, Object> getMap() {
        return map;
    }

    /**
     * Check if this is a fake client.
     *
     * @return True if this is a fake client, else false
     */
    public boolean isFake() {
        return fake;
    }

    /**
     * Check if this client is actually a server.
     *
     * @return True if this client is actually a server.
     */
    public boolean isServer() {
        return nickname.indexOf(':') > -1;
    }

    /**
     * Set if this is a fake client.
     * This returns "this" and thus can be used in the construction line.
     *
     * @param newValue new value for isFake - True if this is a fake client, else false
     * @return this Object
     */
    public IRCClientInfo setFake(final boolean newValue) {
        fake = newValue;
        return this;
    }

    /**
     * Get a nickname of a user from a hostmask.
     * Hostmask must match (?:)nick(?!ident)(?@host)
     *
     * @param sWho Hostname to parse
     * @return nickname of user
     */
    public static String parseHost(final String sWho) {
        // Get the nickname from the string.
        return parseHostFull(sWho)[0];
    }

    /**
     * Get a nick ident and host of a user from a hostmask.
     * Hostmask must match (?:)nick(?!ident)(?@host)
     *
     * @param hostmask Hostname to parse
     * @return Array containing details. (result[0] -&gt; Nick | result[1]
     * -&gt; Ident | result[2] - Host)
     */
    public static String[] parseHostFull(final String hostmask) {
        String[] sTemp;
        final String[] result = new String[3];

        if (!hostmask.isEmpty() && hostmask.charAt(0) == ':') {
            sTemp = hostmask.substring(1).split("@", 2);
        } else {
            sTemp = hostmask.split("@", 2);
        }

        result[2] = sTemp.length == 1 ? "" : sTemp[1];
        sTemp = sTemp[0].split("!", 2);
        result[1] = sTemp.length == 1 ? "" : sTemp[1];
        result[0] = sTemp[0];

        return result;
    }

    /**
     * Set the nick/ident/host of this client.
     *
     * @param hostmask takes a host (?:)nick(?!ident)(?@host) and sets nick/host/ident variables
     * @param updateNick if this is false, only host/ident will be updated.
     */
    public void setUserBits(final String hostmask, final boolean updateNick) {
        setUserBits(hostmask, updateNick, false);
    }

    /**
     * Set the nick/ident/host of this client.
     *
     * @param hostmask takes a host (?:)nick(?!ident)(?@host) and sets nick/host/ident variables
     * @param updateNick if this is false, only host/ident will be updated.
     * @param allowBlank if this is true, ident/host will be set even if
     *                   parseHostFull returns empty values for them
     */
    public void setUserBits(final String hostmask, final boolean updateNick, final boolean allowBlank) {
        final String[] hostParts = parseHostFull(hostmask);

        if (!hostParts[2].isEmpty() || allowBlank) {
            host = hostParts[2];
        }

        if (!hostParts[1].isEmpty() || allowBlank) {
            ident = hostParts[1];
        }

        if (updateNick) {
            nickname = hostParts[0];
        }
    }

    /**
     * Get a string representation of the user.
     *
     * @return String representation of the user.
     */
    @Override
    public String toString() {
        return nickname + "!" + ident + "@" + host;
    }

    /**
     * {@inheritDoc}
     *
     * For local clients, this method will return the nickname value supplied
     * by the {@link IRCParser} instead of the local nickname.
     */
    @Override
    public String getNickname() {
        // If this is the localClient then do what we are supposed to do, and ask
        // the parser using parser.getNickname()
        if (this.equals(parser.getLocalClient())) {
            return parser.getMyNickname();
        } else {
            return nickname;
        }
    }

    /**
     * Retrieves the locally stored nickname of this client.
     *
     * @return The client's nickname
     */
    public String getRealNickname() {
        return nickname;
    }

    /** {@inheritDoc} */
    @Override
    public String getUsername() {
        return ident;
    }

    /** {@inheritDoc} */
    @Override
    public String getHostname() {
        return host;
    }

    /**
     * Set the away state of a user.
     * Automatically sets away reason to "" if not set to AwayState.AWAY
     *
     * @param newState AwayState representing new away state.
     */
    protected void setAwayState(final AwayState newState) {
        away = newState;

        if (away != AwayState.AWAY) {
            awayReason = "";
        }
    }

    /** {@inheritDoc} */
    @Override
    public AwayState getAwayState() {
        return away;
    }

    /** {@inheritDoc} */
    @Override
    public String getAwayReason() {
        return awayReason;
    }

    /**
     * Set the Away Reason for this user.
     * Automatically set to "" if awaystate is set to false
     *
     * @param newValue new away reason for user.
     */
    protected void setAwayReason(final String newValue) {
        awayReason = newValue;
    }

    /** {@inheritDoc} */
    @Override
    public String getRealname() {
        return realName;
    }

    /**
     * Set the RealName for this user.
     *
     * @param newValue new RealName for user.
     */
    protected void setRealName(final String newValue) {
        realName = newValue;
    }

    /** {@inheritDoc} */
    @Override
    public String getAccountName() {
        return accountName;
    }

    /**
     * Set the account name for this user.
     *
     * @param newValue new account name for user.
     */
    protected void setAccountName(final String newValue) {
        accountName = newValue;
    }

    /**
     * Set the user modes (as an integer).
     *
     * @param newMode new long representing channel modes. (Boolean only)
     */
    protected void setUserMode(final long newMode) {
        modes = newMode;
    }

    /**
     * Get the user modes (as an integer).
     *
     * @return long representing channel modes. (Boolean only)
     */
    public long getUserMode() {
        return modes;
    }

    /** {@inheritDoc} */
    @Override
    public String getModes() {
        final StringBuilder sModes = new StringBuilder("+");
        final long nChanModes = this.getUserMode();

        for (char cTemp : parser.userModes.keySet()) {
            final long nTemp = parser.userModes.get(cTemp);

            if ((nChanModes & nTemp) == nTemp) {
                sModes.append(cTemp);
            }
        }

        return sModes.toString();
    }

    /**
     * Is this client an oper?
     * This is a guess currently based on user-modes and thus only works on the
     * parsers own client.
     *
     * @return True/False if this client appears to be an oper
     */
    public boolean isOper() {
        final String modestr = getModes();
        return modestr.indexOf('o') > -1 || modestr.indexOf('O') > -1;
    }

    /**
     * Add a ChannelClientInfo as a known reference to this client.
     *
     * @param cci ChannelClientInfo to add as a known reference
     */
    public void addChannelClientInfo(final IRCChannelClientInfo cci) {
        final String key = parser.getStringConverter().toLowerCase(cci.getChannel().getName());
        if (!clients.containsKey(key)) {
            clients.put(key, cci);
        }
    }

    /**
     * Remove a ChannelClientInfo as a known reference to this client.
     *
     * @param cci ChannelClientInfo to remove as a known reference
     */
    public void delChannelClientInfo(final IRCChannelClientInfo cci) {
        final String key = parser.getStringConverter().toLowerCase(cci.getChannel().getName());
        if (clients.containsKey(key)) {
            clients.remove(key);
        }
    }

    /**
     * Check to see if a client is still known on any of the channels we are on.
     *
     * @return Boolean to see if client is still visable.
     */
    public boolean checkVisibility() {
        return !clients.isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public int getChannelCount() {
        return clients.size();
    }

    /**
     * Get a list of channelClients that point to this object.
     *
     * @return int with the count of known channels
     */
    public List<IRCChannelClientInfo> getChannelClients() {
        return new ArrayList<>(clients.values());
    }

    /** {@inheritDoc} */
    @Override
    public void alterMode(final boolean add, final Character mode) {
        if (isFake() || !parser.userModes.containsKey(mode)) {
            return;
        }

        int modecount = 1;
        if (parser.h005Info.containsKey("MODES")) {
            try {
                modecount = Integer.parseInt(parser.h005Info.get("MODES"));
            } catch (NumberFormatException e) {
                modecount = 1;
            }
        }

        final String modestr = (add ? "+" : "-") + mode;
        final String teststr = (add ? "-" : "+") + mode;

        if (modeQueue.contains(teststr)) {
            modeQueue.remove(teststr);
            return;
        } else if (modeQueue.contains(modestr)) {
            return;
        }

        parser.callDebugInfo(IRCParser.DEBUG_INFO, "Queueing user mode: %s", modestr);
        modeQueue.add(modestr);

        if (modeQueue.size() == modecount) {
            flushModes();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void flushModes() {
        if (modeQueue.isEmpty()) {
            return;
        }

        final StringBuilder positivemode = new StringBuilder();
        final StringBuilder negativemode = new StringBuilder();
        final StringBuilder sendModeStr = new StringBuilder();
        String modestr;
        boolean positive;
        for (String aModeQueue : modeQueue) {
            modestr = aModeQueue;
            positive = modestr.charAt(0) == '+';
            if (positive) {
                positivemode.append(modestr.charAt(1));
            } else {
                negativemode.append(modestr.charAt(1));
            }
        }

        if (negativemode.length() > 0) {
            sendModeStr.append("-").append(negativemode);
        }

        if (positivemode.length() > 0) {
            sendModeStr.append("+").append(positivemode);
        }

        parser.callDebugInfo(IRCParser.DEBUG_INFO, "Sending mode: %s", sendModeStr.toString());
        parser.sendRawMessage("MODE " + nickname + " " + sendModeStr.toString());
        clearModeQueue();
    }

    /**
     * This function will clear the mode queue (WITHOUT Sending).
     */
    public void clearModeQueue() {
        modeQueue.clear();
    }

    /** {@inheritDoc} */
    @Override
    public Parser getParser() {
        return parser;
    }

    /** {@inheritDoc} */
    @Override
    public void setNickname(final String name) {
        if (parser.getLocalClient().equals(this)) {
            parser.setNickname(name);
        } else {
            throw new UnsupportedOperationException("Cannot call setNickname on non-local client");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setAway(final String reason) {
        parser.sendRawMessage("AWAY :" + reason);
    }

    /** {@inheritDoc} */
    @Override
    public void setBack() {
        parser.sendRawMessage("AWAY");
    }
}
