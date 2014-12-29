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

package com.dmdirc.parser.events;

import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.Parser;

import java.util.Date;

/**
 * Called when the channel modes are changed or discovered.
 */
public class ChannelModeChangeEvent extends ParserEvent {

    private final ChannelInfo channel;
    private final ChannelClientInfo client;
    private final String host;
    private final String modes;

    public ChannelModeChangeEvent(final Parser parser, final Date date, final ChannelInfo channel,
            final ChannelClientInfo client, final String host, final String modes) {
        super(parser, date);
        this.channel = channel;
        this.client = client;
        this.host = host;
        this.modes = modes;
    }

    public ChannelInfo getChannel() {
        return channel;
    }

    public ChannelClientInfo getClient() {
        return client;
    }

    public String getHost() {
        return host;
    }

    public String getModes() {
        return modes;
    }
}
