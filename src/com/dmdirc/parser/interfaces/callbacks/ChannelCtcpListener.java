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

package com.dmdirc.parser.interfaces.callbacks;

import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.FakableArgument;
import com.dmdirc.parser.interfaces.FakableSource;
import com.dmdirc.parser.interfaces.Parser;
import com.dmdirc.parser.interfaces.SpecificCallback;

import java.util.Date;

/**
 * Called when a person sends a CTCP to a channel.
 * sHost is the hostname of the person sending the CTCP. (Can be a server or a person)<br>
 * cChannelClient is null if user is a server.
 */
@SpecificCallback
public interface ChannelCtcpListener extends CallbackInterface {

    /**
     * Called when a person sends a CTCP to a channel.
     * sHost is the hostname of the person sending the CTCP. (Can be a server or a person)<br>
     * cChannelClient is null if user is a server.
     *
     * @param parser Reference to the parser object that made the callback.
     * @param date The date/time at which the event occured
     * @param channel Channel where CTCP was sent
     * @param client ChannelClient who sent the message (may be null if server)
     * @param type Type of CTCP (VERSION, TIME etc)
     * @param message Additional contents
     * @param host Hostname of sender (or servername)
     * @see com.dmdirc.parser.irc.ProcessMessage#callChannelCTCP
     */
    void onChannelCTCP(@FakableSource Parser parser,
            Date date,
            @FakableSource ChannelInfo channel,
            @FakableArgument ChannelClientInfo client,
            String type, String message,
            @FakableSource String host);
}
