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

package com.dmdirc.parser.common;

import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.ClientInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a basic implementation of the {@link ChannelClientInfo} interface.
 */
public abstract class BaseChannelClientInfo implements ChannelClientInfo {

    /** A map for random data associated with the client to be stored in. */
    private final Map<Object, Object> map = new HashMap<>();

    /** The channel that the client is associated with. */
    private final ChannelInfo channel;

    /** The client that is associated with the channel. */
    private final ClientInfo client;

    /**
     * Creates a new BaseChannelClientInfo object for the specified client's
     * association with the specified channel.
     *
     * @param channel The channel the association is with
     * @param client The user that holds the association
     */
    public BaseChannelClientInfo(final ChannelInfo channel, final ClientInfo client) {
        this.channel = channel;
        this.client = client;
    }

    @Override
    public ChannelInfo getChannel() {
        return channel;
    }

    @Override
    public ClientInfo getClient() {
        return client;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<Object, Object> getMap() {
        return map;
    }

    @Override
    public String toString() {
        return getImportantMode() + client;
    }

}
