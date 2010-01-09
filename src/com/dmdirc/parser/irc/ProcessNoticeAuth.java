/*
 * Copyright (c) 2006-2010 Chris Smith, Shane Mc Cormack, Gregory Holmes
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

import com.dmdirc.parser.interfaces.callbacks.AuthNoticeListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Process a NoticeAuth message.
 */
public class ProcessNoticeAuth extends IRCProcessor {
    /**
     * Pattern for automatic connecting to some servers such as undernet.
     * This will only work if the user only needs to alphanumeric characters,
     * this is so that messages from bouncers such as typing "/PASS <foo>" won't
     * get matched.
     */
    final Pattern pattern = Pattern.compile("(?i)to connect you must type \\/(?:RAW |QUOTE )?([0-9A-Z ]+)");

    /**
     * Process a NoticeAuth message.
     *
     * @param sParam Type of line to process ("Notice Auth")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String[] token) {
        final String message = token[token.length-1];
        callNoticeAuth(message);
        if (myParser.isAutoQuoteConnect()) {
            final Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                myParser.sendString(matcher.group(1).trim());
            }
        }
    }
    
    /**
     * Callback to all objects implementing the NoticeAuth Callback.
     *
     * @see INoticeAuth
     * @param data Incomming Line.
     * @return true if a method was called, false otherwise
     */
    protected boolean callNoticeAuth(final String data) {
        return getCallbackManager().getCallbackType(AuthNoticeListener.class).call(data);
    }
    
    /**
     * What does this IRCProcessor handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"Notice Auth"};
    } 
    
    /**
     * Create a new instance of the ProcessNoticeAuth Object.
     *
     * @param parser IRCParser That owns this object
     * @param manager ProcessingManager that is in charge of this object
     */
    protected ProcessNoticeAuth (final IRCParser parser, final ProcessingManager manager) { super(parser, manager); }

}
