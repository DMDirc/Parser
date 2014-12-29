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

package com.dmdirc.parser.irc.processors;

import com.dmdirc.parser.interfaces.callbacks.MotdEndListener;
import com.dmdirc.parser.interfaces.callbacks.MotdLineListener;
import com.dmdirc.parser.interfaces.callbacks.MotdStartListener;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.ProcessingManager;

import java.util.Date;

/**
 * Process a MOTD Related Line.
 */
public class ProcessMOTD extends IRCProcessor {

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param manager ProcessingManager that is in charge of this IRCProcessor
     */
    public ProcessMOTD(final IRCParser parser, final ProcessingManager manager) {
        super(parser, manager);
    }

    /**
     * Process a MOTD Related Line.
     *
     * @param sParam Type of line to process ("375", "372", "376", "422")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String... token) {
        switch (sParam) {
            case "375":
                callMOTDStart(token[token.length - 1]);
                break;
            case "372":
                callMOTDLine(token[token.length - 1]);
                break;
            default:
                callMOTDEnd("422".equals(sParam), token[token.length - 1]);
                break;
        }
    }

    /**
     * Callback to all objects implementing the MOTDEnd Callback.
     *
     * @param noMOTD Was this an MOTDEnd or NoMOTD
     * @param data The contents of the line (incase of language changes or so)
     * @see MotdEndListener
     */
    protected void callMOTDEnd(final boolean noMOTD, final String data) {
        getCallback(MotdEndListener.class).onMOTDEnd(parser, new Date(), noMOTD, data);
    }

    /**
     * Callback to all objects implementing the MOTDLine Callback.
     *
     * @see MotdLineListener
     * @param data Incomming Line.
     */
    protected void callMOTDLine(final String data) {
        getCallback(MotdLineListener.class).onMOTDLine(parser, new Date(), data);
    }

    /**
     * Callback to all objects implementing the MOTDStart Callback.
     *
     * @see MotdStartListener
     * @param data Incomming Line.
     */
    protected void callMOTDStart(final String data) {
        getCallback(MotdStartListener.class).onMOTDStart(parser, new Date(), data);
    }

    /**
     * What does this IRCProcessor handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"372", "375", "376", "422"};
    }
}
