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

import com.dmdirc.parser.interfaces.Encoder;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link java.io.BufferedReader}-style reader that is aware of the IRC
 * protocol and can transcode text appropriately.
 *
 * @since 0.6.5
 */
public class IRCReader implements Closeable {

    /** Maximum length for an IRC line in bytes. */
    private static final int LINE_LENGTH = 1024;
    /** The input stream to read input from. */
    private final InputStream stream;
    /** The encoder to use to encode lines. */
    private final Encoder encoder;
    /** Decoder to use for parts not handled by the encoder. */
    private final CharsetDecoder decoder;

    /**
     * Creates a new IRCReader which will read from the specified stream.
     * Protocol-level elements (e.g. channel and user names) will be encoded
     * using the system default charset.
     *
     * @param inputStream The stream to read input from
     * @param encoder The encoder to use to encode lines
     */
    public IRCReader(final InputStream inputStream, final Encoder encoder) {
        this(inputStream, encoder, Charset.defaultCharset());
    }

    /**
     * Creates a new IRCReader which will read from the specified stream.
     *
     * @param inputStream The stream to read input from
     * @param encoder The encoder to use to encode lines
     * @param charset The charset to use for protocol-level elements
     */
    public IRCReader(final InputStream inputStream, final Encoder encoder,
            final Charset charset) {
        this.stream = inputStream;
        this.encoder = encoder;
        this.decoder = charset.newDecoder();
        this.decoder.onMalformedInput(CodingErrorAction.REPLACE);
        this.decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
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

        boolean hasTags = false;
        boolean endOfTags = false;
        boolean hasV3Tags = false;
        boolean foundFirstSpace = false;

        while (offset < LINE_LENGTH && (chr = stream.read()) > -1) {
            if (chr == '\r') {
                continue;
            } else if (chr == '\n') {
                // End of the line
                break;
            }

            if (hasTags && !endOfTags) {
                // Tags end either at the first @ for non-v3 tags or space for v3
                if (offset > 0 && ((chr == '@' && !hasV3Tags) || chr == ' ')) {
                    endOfTags = true;
                    hasV3Tags = (chr == ' ');
                }
                // If we are still possibly looking at tags, and we find a non-numeric
                // character, then we probably have v3Tags
                if (!endOfTags && (chr < '0' || chr > '9')) {
                    hasV3Tags = true;
                }
            } else if (offset == 0 && chr == '@') {
                hasTags = true;
            } else if (offset == 0) {
                endOfTags = true;
            }

            line[offset++] = (byte) chr;

            if (lastChr == ' ' && chr == ':' && paramOffset == -1) {
                // We've found the last param
                if (!hasV3Tags || foundFirstSpace) {
                    paramOffset = offset;
                } else if (hasV3Tags) {
                    foundFirstSpace = true;
                }
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
        try {
            final String firstPart = this.decoder.decode(ByteBuffer.wrap(line,
                    0, paramOffset == -1 ? length : paramOffset - 2)).toString();

            final String[] firstTokens = firstPart.split("[ ]+");

            final String[] tokens;
            if (paramOffset > -1) {
                final String source = getSource(firstTokens);
                final String destination = getDestination(firstTokens);

                final String lastPart = encoder.encode(source, destination,
                        line, paramOffset, length - paramOffset);
                tokens = new String[firstTokens.length + 1];
                System.arraycopy(firstTokens, 0, tokens, 0, firstTokens.length);
                tokens[firstTokens.length] = lastPart;
            } else {
                tokens = firstTokens;
            }

            return new ReadLine(new String(line, 0, length), tokens);
        } catch (CharacterCodingException ex) {
            // Shouldn't happen, as we're replacing errors.
            return null;
        }
    }

    /**
     * Determines the 'source' of a line made up of the specified tokens. A
     * source is described by the first token if and only if that token starts
     * with a colon.
     *
     * @param tokens The tokens to extract a source from
     * @return The relevant source or null if none specified
     */
    private String getSource(final String... tokens) {
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
    private String getDestination(final String... tokens) {
        if (tokens.length > 0 && tokens[0].length() >= 3 && tokens[0].charAt(0) == ':') {
            final int target = tokens[1].matches("^[0-9]+$") ? 3 : 2;

            if (tokens.length > target) {
                return tokens[target];
            }
        }

        return null;
    }

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
        /** The tags (if any) found in the line, individually encoded as appropriate. */
        private final Map<String,String> tags = new HashMap<>();

        /**
         * Creates a new instance of {@link ReadLine} with the specified line
         * and tokens.
         *
         * @param line A string representation of the line
         * @param lineTokens The tokens which make up the line
         */
        public ReadLine(final String line, final String... lineTokens) {
            this.line = line;

            String[] tokens = lineTokens;
            if (!tokens[0].isEmpty() && tokens[0].charAt(0) == '@') {
                // Look for old-style TSIRC timestamp first.
                final int tsEnd = tokens[0].indexOf('@', 1);
                boolean hasTSIRCDate = false;
                if (tsEnd > -1) {
                    try {
                        final long ts = Long.parseLong(tokens[0].substring(1, tsEnd));
                        tags.put("tsirc date", tokens[0].substring(1, tsEnd));
                        hasTSIRCDate = true;
                        tokens[0] = tokens[0].substring(tsEnd + 1);
                    } catch (final NumberFormatException nfe) { /* Not a timestamp. */ }
                }

                if (!hasTSIRCDate) {
                    final String[] lineTags = tokens[0].substring(1).split(";");
                    for (final String keyVal : lineTags) {
                        if (!keyVal.isEmpty()) {
                            final String[] keyValue = keyVal.split("=", 2);
                            tags.put(keyValue[0], keyValue.length > 1 ? keyValue[1] : "");
                        }
                    }

                    tokens = new String[lineTokens.length - 1];
                    System.arraycopy(lineTokens, 1, tokens, 0, lineTokens.length - 1);
                }
            }

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

        /**
         * Retrieves a map of tags extracted from the specified line.
         *
         * @return The line's tags
         */
        public Map<String,String> getTags() {
            return tags;
        }
    }
}
