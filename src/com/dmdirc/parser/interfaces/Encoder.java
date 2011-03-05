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

package com.dmdirc.parser.interfaces;

/**
 * Defines standard methods to allow for an external object to encode text
 * for a {@link Parser}.
 *
 * @since 0.6.5
 * @author chris
 */
public interface Encoder {

    /**
     * Encode the specified message which was sent from the optional source
     * to the optional target using an appropriate charset of the implementor's
     * choosing.
     *
     * @param source The source of the message or null if not applicable
     * @param target The target of the message or null if not applicable
     * @param message The message as read from the wire
     * @param offset The offset within the 'message' array to start reading
     * @param length The number of bytes to read from the message array
     * @return An encoded version of the message
     */
    String encode(String source, String target, byte[] message, int offset, int length);

}
