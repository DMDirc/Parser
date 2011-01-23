/*
 * Copyright (c) 2006-2011 Chris Smith, Shane Mc Cormack, Gregory Holmes
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

import com.dmdirc.parser.interfaces.Parser;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements common base functionality for parsers.
 */
public abstract class BaseParser implements Parser {

    /** The URI that this parser was constructed for. */
    private final URI uri;

    /** The ignore list for this parser. */
    private IgnoreList ignoreList;

    /** The ping timer interval for this parser. */
    private long pingTimerInterval;

    /** The ping timer fraction for this parser. */
    private int pingTimerFraction;

    /** The callback manager to use for this parser. */
    private final CallbackManager callbackManager;

    /** A map for callers to use to store things for no sane reason. */
    private final Map<Object, Object> map = new HashMap<Object, Object>();

    /**
     * Creates a new base parser for the specified URI.
     *
     * @param uri The URI this parser will connect to.
     * @param implementations A map of interface implementations for this parser
     */
    public BaseParser(final URI uri, final Map<Class<?>, Class<?>> implementations) {
        this.uri = uri;
        this.callbackManager = new CallbackManager(this, implementations);
    }

    /** {@inheritDoc} */
    @Override
    public URI getURI() {
        return uri;
    }

    /** {@inheritDoc} */
    @Override
    public IgnoreList getIgnoreList() {
        return ignoreList;
    }

    /** {@inheritDoc} */
    @Override
    public void setIgnoreList(final IgnoreList ignoreList) {
        this.ignoreList = ignoreList;
    }

    /** {@inheritDoc} */
    @Override
    public long getPingTimerInterval() {
        return pingTimerInterval;
    }

    /** {@inheritDoc} */
    @Override
    public void setPingTimerInterval(final long newValue) {
        pingTimerInterval = newValue;
    }

    /** {@inheritDoc} */
    @Override
    public int getPingTimerFraction() {
        return pingTimerFraction;
    }

    /** {@inheritDoc} */
    @Override
    public void setPingTimerFraction(final int newValue) {
        pingTimerFraction = newValue;
    }

    /** {@inheritDoc} */
    @Override
    public CallbackManager getCallbackManager() {
        return callbackManager;
    }

    /** {@inheritDoc} */
    @Override
    public Map<Object, Object> getMap() {
        return map;
    }

    /** {@inheritDoc} */
    @Override
    public void joinChannel(final String channel) {
        joinChannels(new ChannelJoinRequest(channel));
    }

    /** {@inheritDoc} */
    @Override
    public void joinChannel(final String channel, final String key) {
        joinChannels(new ChannelJoinRequest(channel, key));
    }

}
