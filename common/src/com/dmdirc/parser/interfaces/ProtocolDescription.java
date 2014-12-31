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

package com.dmdirc.parser.interfaces;

import java.net.URI;

/**
 * Describes the default properties a protocol handled by a {@link Parser}
 * instance.
 *
 * @since 0.6.4
 */
public interface ProtocolDescription {

    /**
     * Retrieves the default port to use for this protocol if none is specified.
     *
     * @return This protocol's default port
     */
    int getDefaultPort();

    /**
     * Parses the specified hostmask into an array containing a nickname,
     * username and hostname, in that order.
     *
     * @param hostmask The hostmask to be parsed
     * @return An array containing the nickname, username and hostname
     */
    String[] parseHostmask(String hostmask);

    /**
     * Checks if the specified URI is going via a secure connection or not.
     *
     * @param uri URI to check
     *
     * @return true if the URI is secure false otherwise
     *
     * @since 0.6.6
     */
    boolean isSecure(URI uri);

}
