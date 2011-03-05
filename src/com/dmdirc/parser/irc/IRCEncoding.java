/*
 * Copyright (c) 2006-2011 DMDirc Developers
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
 * An enumeration of the supported types of encoding for an IRC connection.
 *
 * @since 0.6.6
 */
public enum IRCEncoding {

    /** The ASCII encoding. */
    ASCII(0),
    /** "Strict" RFC1459. */
    STRICT_RFC1459(3),
    /** Standard RFC1459. */
    RFC1459(4);
    /** The limit used for this encoding. */
    private final int limit;

    /**
     * Creates a new instance of IRCEncoding with the specified limit.
     *
     * @param limit The limit used for this encoding.
     */
    private IRCEncoding(final int limit) {
        this.limit = limit;
    }

    /**
     * Retrieves the "limit" of this encoding. That is, the number of characters
     * after the uppercase ASCII 'Z' which should be treated
     * "case-insensitively". A limit of 3 will cause the '[', '\' and ']'
     * characters to be treated as equal to '{', '|' and '}', respectively.
     *
     * @return The limit of this encoding
     */
    public int getLimit() {
        return limit;
    }
}
