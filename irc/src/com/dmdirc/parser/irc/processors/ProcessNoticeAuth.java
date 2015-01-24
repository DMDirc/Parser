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

package com.dmdirc.parser.irc.processors;

import com.dmdirc.parser.events.AuthNoticeEvent;
import com.dmdirc.parser.irc.IRCParser;

import java.util.Date;

import javax.inject.Inject;

/**
 * Process a NoticeAuth message.
 */
public class ProcessNoticeAuth extends IRCProcessor {

    /**
     * Create a new instance of the ProcessNoticeAuth Object.
     *
     * @param parser IRCParser That owns this object
     */
    @Inject
    public ProcessNoticeAuth(final IRCParser parser) {
        super(parser, "Notice Auth");
    }

    /**
     * Process a NoticeAuth message.
     *
     * @param sParam Type of line to process ("Notice Auth")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String... token) {
        callNoticeAuth(token[token.length - 1]);
    }

    /**
     * Callback to all objects implementing the NoticeAuth Callback.
     *
     * @param data Incomming Line.
     */
    protected void callNoticeAuth(final String data) {
        getCallbackManager().publish(new AuthNoticeEvent(parser, new Date(), data));
    }

}
