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

import com.dmdirc.parser.interfaces.Encoder;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link java.io.BufferedReader}-style reader that is aware of the IRC
 * protocol and can transcode text appropriately.
 *
 * @since 0.6.5
 */
public class IRCReader implements Closeable {

    /** Maximum length for an IRC line in bytes. */
    private static final int LINE_LENGTH = 512;
    /** The input stream to read input from. */
    private final InputStream stream;
    /** The encoder to use to encode lines. */
    private final Encoder encoder;

    /**
     * Creates a new IRCReader which will read from the specified stream.
     *
     * @param inputStream The stream to read input from
     * @param encoder The encoder to use to encode lines
     */
    public IRCReader(final InputStream inputStream, final Encoder encoder) {
        this.stream = inputStream;
        this.encoder = encoder;
    }

    /**
     * Reads a line from the underlying input stream, tokenises it, and
     * requests that this reader's encoder encodes the message part of the
     * line, if any.
     *
     * @return A wrapped line tokenised per RFC1459, or null if the stream ends
     * @throws IOException If an IOException is encountered reading the
     * underlying stream
     */
    public ReadLine readLine() throws IOException {
        final byte[] line = new byte[LINE_LENGTH];
        int offset = 0;
        int paramOffset = -1;
        int chr = 0, lastChr = 0;

        while (offset < 512 && (chr = stream.read()) > -1) {
            if (chr == '\r') {
                continue;
            } else if (chr == '\n') {
                // End of the line
                break;
            }

            line[offset++] = (byte) chr;

            if (lastChr == ' ' && chr == ':' && paramOffset == -1) {
                // We've found the last param
                paramOffset = offset;
            }

            lastChr = chr;
        }

        if (chr == -1) {
            // Hit the end of the stream
            return null;
        }

        return processLine(line, offset, paramOffset);
    }

    /**
     * Processes the specified line into a wrapped {@link ReadLine} instance.
     *
     * @param line The line as read from the wire
     * @param length The length of the line in bytes
     * @param paramOffset The offset of the first byte of the trailing parameter
     * (i.e., the first byte following the ASCII sequence ' :'), or -1 if no
     * such parameter exists.
     * @return A corresponding {@link ReadLine} instance
     */
    private ReadLine processLine(final byte[] line, final int length, final int paramOffset) {
        final String firstPart = new String(line, 0, paramOffset == -1 ? length : paramOffset - 2);
        final String[] firstTokens = firstPart.split("[ ]+");

        final String[] tokens;
        if (paramOffset > -1) {
            final String source = getSource(firstTokens);
            final String destination = getDestination(firstTokens);

            final String lastPart = encoder.encode(source, destination, line, paramOffset, length - paramOffset);
            tokens = new String[firstTokens.length + 1];
            System.arraycopy(firstTokens, 0, tokens, 0, firstTokens.length);
            tokens[firstTokens.length] = lastPart;
        } else {
            tokens = firstTokens;
        }

        return new ReadLine(new String(line, 0, length), tokens);
    }

    /**
     * Determines the 'source' of a line made up of the specified tokens. A
     * source is described by the first token if and only if that token starts
     * with a colon.
     *
     * @param tokens The tokens to extract a source from
     * @return The relevant source or null if none specified
     */
    private String getSource(final String[] tokens) {
        if (tokens.length > 0 && tokens[0].length() > 1 && tokens[0].charAt(0) == ':') {
            return tokens[0].substring(1);
        }

        return null;
    }

    /**
     * Determines the 'destination' of a line made up of the specified tokens.
     * A destination exists only if a source exists
     * (see {@link #getSource(java.lang.String[])}), and is contained within
     * the third argument for non-numeric lines, and fourth for numerics.
     *
     * @param tokens The tokens to extract a destination from
     * @return The relevant destination or null if none specified
     */
    private String getDestination(final String[] tokens) {
        if (tokens.length > 0 && tokens[0].length() >= 3 && tokens[0].charAt(0) == ':') {
            final int target = tokens[1].matches("^[0-9]+$") ? 3 : 2;

            if (tokens.length > target) {
                return tokens[target];
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        stream.close();
    }

    /**
     * Represents a line that has been read from the IRC server and encoded
     * appropriately.
     */
    public static class ReadLine {

        /** A representation of the read-line using a default encoding. */
        private final String line;
        /** The tokens found in the line, individually encoded as appropriate. */
        private final String[] tokens;

        /**
         * Creates a new instance of {@link ReadLine} with the specified line
         * and tokens.
         *
         * @param line A string representation of the line
         * @param tokens The tokens which make up the line
         */
        public ReadLine(final String line, final String[] tokens) {
            this.line = line;
            this.tokens = tokens;
        }

        /**
         * Retrieves a string representation of the line that has been read.
         * This may be encoded using a charset which is not appropriate for
         * displaying all of the line's contents, and is intended for debug
         * purposes only.
         *
         * @return A string representation of the line
         */
        public String getLine() {
            return line;
        }

        /**
         * Retrieves an array of tokens extracted from the specified line.
         * Each token may have a different encoding.
         *
         * @return The line's tokens
         */
        public String[] getTokens() {
            return tokens;
        }
    }
}
