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

package com.dmdirc.parser.irc;

/**
 * Constants used by the IRC parser.
 */
public final class IrcConstants {

    public static final int DEFAULT_PORT = 6667;
    public static final int DEFAULT_SSL_PORT = 6697;

    public static final String ISUPPORT_CHANNEL_MODES = "CHANMODES";
    public static final String ISUPPORT_CHANNEL_USER_PREFIXES = "PREFIX";
    public static final String ISUPPORT_MAXIMUM_BANS = "MAXBANS";
    public static final String ISUPPORT_MAXIMUM_LIST_MODES = "MAXLIST";
    public static final String ISUPPORT_USER_CHANNEL_MODES = "USERCHANMODES";
    public static final String ISUPPORT_USER_MODES = "USERMODES";
    public static final String ISUPPORT_TOPIC_LENGTH = "TOPICLEN";

    public static final int NUMERIC_ERROR_NICKNAME_IN_USE = 433;
    public static final int NUMERIC_ERROR_PASSWORD_MISMATCH = 464;

    private IrcConstants() {
        // Shouldn't be instantiated.
    }

}
