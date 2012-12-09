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

package com.dmdirc.parser.common;

import com.dmdirc.parser.interfaces.ClientInfo;
import com.dmdirc.parser.interfaces.Parser;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a basic implementation of the {@link ClientInfo} interface.
 */
public abstract class BaseClientInfo implements ClientInfo {

    /** The parser that owns this client. */
    private final Parser parser;

    /** A map for random data associated with the client to be stored in. */
    private final Map<Object, Object> map = new HashMap<Object, Object>();

    /** The user's details. */
    private String nick, user, host, realname = null;

    /**
     * Creates a new base client info for the specified parser with the
     * specified details.
     *
     * @param parser The parser that owns this client info object
     * @param nick The nickname of the user this object represents
     * @param user The username of the user this object represents
     * @param host The hostname of the user this object represents
     */
    public BaseClientInfo(final Parser parser, final String nick,
            final String user, final String host) {
        this.parser = parser;
        this.nick = nick;
        this.user = user;
        this.host = host;
    }

    /** {@inheritDoc} */
    @Override
    public String getNickname() {
        return nick;
    }

    /** {@inheritDoc} */
    @Override
    public String getUsername() {
        return user;
    }

    /** {@inheritDoc} */
    @Override
    public String getHostname() {
        return host;
    }

    /** {@inheritDoc} */
    @Override
    public String getRealname() {
        return realname;
    }

    /** {@inheritDoc} */
    @Override
    public String getAccountName() {
        return "*";
    }

    /**
     * Sets the hostname of this user.
     *
     * @param host The new hostname
     */
    protected void setHostname(final String host) {
        this.host = host;
    }

    /**
     * Sets the nickname of this user.
     *
     * @param nick The new nickname
     */
    protected void setLocalNickname(final String nick) {
        this.nick = nick;
    }

    /**
     * Sets the realname of this user.
     *
     * @param realname The new realname
     */
    protected void setRealname(final String realname) {
        this.realname = realname;
    }

    /**
     * Sets the username of this user.
     *
     * @param user The new username
     */
    protected void setUsername(final String user) {
        this.user = user;
    }

    /** {@inheritDoc} */
    @Override
    public Map<Object, Object> getMap() {
        return map;
    }

    /** {@inheritDoc} */
    @Override
    public Parser getParser() {
        return parser;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getNickname();
    }

    /** {@inheritDoc} */
    @Override
    public String getAwayReason() {
        return "";
    }

}
