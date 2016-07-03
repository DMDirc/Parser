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

package com.dmdirc.parser.events;

import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.Parser;

import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Called when a person sends a notice to a channel with a mode prefix.
 */
public class ChannelModeNoticeEvent extends ParserEvent {

    private final ChannelInfo channel;
    private final char prefix;
    private final ChannelClientInfo client;
    private final String message;
    private final String host;

    public ChannelModeNoticeEvent(final Parser parser, final LocalDateTime date,
            final ChannelInfo channel, final char prefix, final ChannelClientInfo client,
            final String message, final String host) {
        super(parser, date);
        this.channel = checkNotNull(channel);
        this.prefix = checkNotNull(prefix);
        this.client = checkNotNull(client);
        this.message = checkNotNull(message);
        this.host = checkNotNull(host);
    }

    public ChannelInfo getChannel() {
        return channel;
    }

    public char getPrefix() {
        return prefix;
    }

    public ChannelClientInfo getClient() {
        return client;
    }

    public String getMessage() {
        return message;
    }

    public String getHost() {
        return host;
    }
}
