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

import com.dmdirc.parser.events.DataInEvent;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.IRCReader.ReadLine;

import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Called on every incoming line BEFORE parsing.
 *
 * This extends the standard DataInEvent to provide access to IRC-Specific bits.
 */
public class IRCDataInEvent extends DataInEvent {

    private final ReadLine line;
    private final String[] tokenisedData;
    private final String action;
    private final int numeric;
    private final boolean isNumeric;

    public IRCDataInEvent(final IRCParser parser, final LocalDateTime date, final ReadLine line) {
        super(parser, date, checkNotNull(line).getLine());
        this.line = line;
        tokenisedData = line.getTokens();

        // Action is slightly more complicated than for DataOut
        if (tokenisedData.length > 1) {
            if (tokenisedData[0].length() > 0 && tokenisedData[0].charAt(0) == ':') {
                if (tokenisedData[1].equalsIgnoreCase("NOTICE") && !parser.got001) {
                    action = "NOTICE AUTH";
                } else {
                    action = tokenisedData[1].toUpperCase();
                }
            } else if (tokenisedData[0].equalsIgnoreCase("NOTICE")) {
                action = "NOTICE AUTH";
            } else {
                action = tokenisedData[0].toUpperCase();
            }
        } else {
            action = "";
        }

        int num = -1;
        try { num = Integer.parseInt(action); } catch (final NumberFormatException e) { }
        numeric = num;
        isNumeric = (numeric != -1);
    }

    public String[] getTokenisedData() {
        return tokenisedData;
    }

    public ReadLine getLine() {
        return line;
    }

    public String getAction() {
        return action;
    }

    public boolean isNumeric() {
        return isNumeric;
    }

    public int getNumeric() {
        return numeric;
    }
}
