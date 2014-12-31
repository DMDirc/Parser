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

import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains information about a client on a channel.
 *
 * @see IRCParser
 */
public class IRCChannelClientInfo implements ChannelClientInfo {

    /** Reference to ClientInfo object this represents. */
    private final IRCClientInfo cClient;
    /** The channel modes associated with this user. */
    private String modes = "";
    /** Manager to use when dealing with prefix modes. */
    private final PrefixModeManager modeManager;
    /** The parser to use to kick people. */
    private final IRCParser parser;
    /** Reference to the channel object that owns this channel client. */
    private final ChannelInfo myChannel;
    /** A Map to allow applications to attach misc data to this object. */
    private Map<Object, Object> myMap;

    /**
     * Create a ChannelClient instance of a CLient.
     *
     * @param tParser Refernce to parser that owns this channelclient (used for modes)
     * @param prefixModeManager Manager to use to access prefix mode information.
     * @param client Client that this channelclient represents
     * @param channel Channel that owns this channelclient
     */
    public IRCChannelClientInfo(final IRCParser tParser, final PrefixModeManager prefixModeManager,
            final IRCClientInfo client, final ChannelInfo channel) {
        myMap = new HashMap<>();
        modeManager = prefixModeManager;
        parser = tParser;
        cClient = client;
        myChannel = channel;
        cClient.addChannelClientInfo(this);
    }

    /**
     * Set the Map object attatched to this object.
     *
     * @param newMap New Map to attatch.
     * @see #getMap
     */
    public void setMap(final Map<Object, Object> newMap) {
        myMap = newMap;
    }

    @Override
    public Map<Object, Object> getMap() {
        return myMap;
    }

    @Override
    public IRCClientInfo getClient() {
        return cClient;
    }

    @Override
    public ChannelInfo getChannel() {
        return myChannel;
    }

    /**
     * Get the nickname of the client object represented by this channelclient.
     *
     * @return Nickname of the Client object represented by this channelclient
     */
    public String getNickname() {
        return cClient.getNickname();
    }

    /**
     * Set the modes this client has (Prefix modes).
     *
     * @param modes The new modes this client has, sorted most-to-least important.
     */
    public void setChanMode(final String modes) {
        this.modes = modes;
    }

    @Override
    public String getAllModes() {
        return modes;
    }

    @Override
    public String getAllModesPrefix() {
        return modeManager.getPrefixesFor(modes);
    }

    @Override
    public String getImportantMode() {
        return modes.isEmpty() ? "" : modes.substring(0, 1);
    }

    @Override
    public String getImportantModePrefix() {
        return modes.isEmpty() ? "" : getAllModesPrefix().substring(0, 1);
    }

    /**
     * Get the String Value of ChannelClientInfo (e.g. @Nickname).
     *
     * @return String Value of user (inc prefix) (e.g. @Nickname)
     */
    @Override
    public String toString() {
        return getImportantModePrefix() + getNickname();
    }

    @Override
    public void kick(final String message) {
        parser.sendString("KICK " + myChannel + ' ' + getNickname(), message);
    }

    /**
     * Get the "Complete" String Value of ChannelClientInfo (ie @+Nickname).
     *
     * @return String Value of user (inc prefix) (ie @+Nickname)
     */
    public String toFullString() {
        return getAllModesPrefix() + getNickname();
    }

    @Override
    public int compareTo(final ChannelClientInfo arg0) {
        return modeManager.compareImportantModes(getAllModes(), arg0.getAllModes());
    }

    @Override
    public Comparator<String> getImportantModeComparator() {
        return modeManager::compareImportantModes;
    }

    /**
     * Determines if this client is opped or not.
     *
     * @return True if the client is opped, false otherwise.
     */
    public boolean isOpped() {
        return modeManager.isOpped(getAllModes());
    }

    /**
     * Adds the specified mode to this client model.
     *
     * @param mode The mode to be added.
     */
    public void addMode(final char mode) {
        modes = modeManager.insertMode(modes, mode);
    }

    /**
     * Removes the specified mode from this client model.
     *
     * @param mode The mode to be removed.
     */
    public void removeMode(final char mode) {
        modes = modeManager.removeMode(modes, mode);
    }
}
