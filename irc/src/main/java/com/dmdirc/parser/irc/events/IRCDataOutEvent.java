/*
 * Copyright (c) 2006-2017 DMDirc Developers
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

package com.dmdirc.parser.irc.events;

import com.dmdirc.parser.events.DataOutEvent;
import com.dmdirc.parser.interfaces.Parser;
import com.dmdirc.parser.irc.IRCParser;

import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Called on every outgoing line BEFORE being sent.
 *
 * This extends the standard DataOutEvent to also pre-tokenise the data.
 */
public class IRCDataOutEvent extends DataOutEvent {

    private final String[] tokenisedData;
    private final String action;

    public IRCDataOutEvent(final Parser parser, final LocalDateTime date, final String data) {
        super(parser, date, data);
        tokenisedData = IRCParser.tokeniseLine(checkNotNull(data));
        action = tokenisedData[0].toUpperCase();
    }

    public String[] getTokenisedData() {
        return tokenisedData;
    }

    public String getAction() {
        return action;
    }
}
