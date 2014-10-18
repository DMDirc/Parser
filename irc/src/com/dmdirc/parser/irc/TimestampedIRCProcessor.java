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

import com.dmdirc.parser.irc.processors.IRCProcessor;

import java.util.Date;

/**
 * TimestampedIRCProcessor.
 *
 * Superclass for all IRCProcessor types that accept timestamps for process.
 */
public abstract class TimestampedIRCProcessor extends IRCProcessor {
    /**
     * Create a new instance of the IRCTimestampedProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param manager ProcessingManager that is in charge of this IRCProcessor
     */
    protected TimestampedIRCProcessor(final IRCParser parser, final ProcessingManager manager) {
        super(parser, manager);
    }

    @Override
    public final void process(final String sParam, final String... token) {
        process(new Date(), sParam, token);
    }

    /**
     * Process a Line.
     *
     * @param date Date of this line
     * @param sParam Type of line to process ("005", "PRIVMSG" etc)
     * @param token IRCTokenised line to process
     */
    public abstract void process(final Date date, final String sParam, final String... token);


}
