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

import com.dmdirc.parser.interfaces.Parser;

import java.util.Date;

/**
 * Interface Used when the Network=blah 005 token is recieved.
 */
public class NetworkDetectedEvent extends ParserEvent {

    private final String networkName;
    private final String ircdVersion;
    private final String ircdType;

    public NetworkDetectedEvent(final Parser parser, final Date date, final String networkName,
            final String ircdVersion, final String ircdType) {
        super(parser, date);
        this.networkName = networkName;
        this.ircdVersion = ircdVersion;
        this.ircdType = ircdType;
    }

    public String getNetworkName() {
        return networkName;
    }

    public String getIrcdVersion() {
        return ircdVersion;
    }

    public String getIrcdType() {
        return ircdType;
    }
}
