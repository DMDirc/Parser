/*
 * Copyright (c) 2006-2012 DMDirc Developers
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

import com.dmdirc.parser.interfaces.StringConverter;

/**
 * IRC String Converter.
 */
public class IRCStringConverter implements StringConverter {

    /** Characters to use when converting to lowercase. */
    private final char[] lowercase;
    /** Characters to use when converting to uppercase. */
    private final char[] uppercase;
    /** Encoding to use. */
    private final IRCEncoding encoding;

    /**
     * Create a new IRCStringConverter with rfc1459 encoding.
     */
    public IRCStringConverter() {
        this(IRCEncoding.RFC1459);
    }

    /**
     * Create a new IRCStringConverter.
     *
     * @param encoding The encoding to use.
     */
    public IRCStringConverter(final IRCEncoding encoding) {
        this.encoding = encoding;

        lowercase = new char[127];
        uppercase = new char[127];

        // Normal Chars
        for (char i = 0; i < lowercase.length; ++i) {
            lowercase[i] = i;
            uppercase[i] = i;
        }

        // Replace the uppercase chars with lowercase
        for (char i = 65; i <= (90 + encoding.getLimit()); ++i) {
            lowercase[i] = (char) (i + 32);
            uppercase[i + 32] = i;
        }
    }

    /**
     * Retrieves the encoding used by this converter.
     *
     * @return This converter's current encoding
     */
    public IRCEncoding getEncoding() {
        return encoding;
    }

    /** {@inheritDoc} */
    @Override
    public String toLowerCase(final String input) {
        final char[] result = input.toCharArray();

        for (int i = 0; i < input.length(); ++i) {
            if (result[i] >= 0 && result[i] < lowercase.length) {
                result[i] = lowercase[result[i]];
            } else {
                result[i] = result[i];
            }
        }

        return new String(result);
    }

    /** {@inheritDoc} */
    @Override
    public String toUpperCase(final String input) {
        final char[] result = input.toCharArray();

        for (int i = 0; i < input.length(); ++i) {
            if (result[i] >= 0 && result[i] < uppercase.length) {
                result[i] = uppercase[result[i]];
            } else {
                result[i] = result[i];
            }
        }

        return new String(result);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equalsIgnoreCase(final String first, final String second) {
        if (first == null && second == null) {
            return true;
        }

        if (first == null || second == null) {
            return false;
        }

        boolean result = first.length() == second.length();

        if (result) {
            final char[] firstChar = first.toCharArray();
            final char[] secondChar = second.toCharArray();

            for (int i = 0; i < first.length(); ++i) {
                if (firstChar[i] < lowercase.length && secondChar[i] < lowercase.length) {
                    result = lowercase[firstChar[i]] == lowercase[secondChar[i]];
                } else {
                    result = firstChar[i] == secondChar[i];
                }

                if (!result) {
                    break;
                }
            }
        }

        return result;
    }
}
