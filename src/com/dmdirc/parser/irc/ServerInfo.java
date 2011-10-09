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

import java.net.URI;

/**
 * Contains Server information.
 *
 * @see IRCParser
 */
public class ServerInfo {

    /**
     * A version number for this class. It should be changed whenever the class
     * structure is changed (or anything else that would prevent serialized
     * objects being unserialized with the new class).
     */
    private static final long serialVersionUID = 2;

    /** URI used to create this ServerInfo if applicable. */
    private URI uri;

    /**
     * Creates a new ServerInfo which will represent the server described by
     * the specified URI.
     *
     * @param uri The URI of the server
     * @since 0.6.3
     */
    public ServerInfo(final URI uri) {
        this.uri = uri;
    }

    /**
     * Get the URI for this ServerInfo.
     * This will return a new URI based on this ServerInfo.
     * Protocol/Password/Host and Port are derived from the getXXXXX() methods
     * the path, query and fragment from the
     *
     * @return URI for this ServerInfo
     */
    public URI getURI() {
        return uri;
    }

    /**
     * Get the hostname.
     *
     * @return Current hostname
     */
    public String getHost() {
        return uri.getHost() == null ? "" : uri.getHost();
    }

    /**
     * Get the port.
     *
     * @return Current port
     */
    public int getPort() {
        return uri.getPort() == -1 ? 6667 : uri.getPort();
    }

    /**
     * Get the password.
     *
     * @return Current Password
     */
    public String getPassword() {
        return uri.getUserInfo() == null ? "" : uri.getUserInfo();
    }

    /**
     * Get if the server uses ssl.
     *
     * @return true if server uses ssl, else false
     */
    public boolean isSSL() {
        return "ircs".equals(uri.getScheme());
    }

    /**
     * Retrieves a String describing any channels included in this ServerInfo's
     * URI.
     *
     * @return A channel string for this URI, or null if no URI specified
     * @since 0.6.3
     */
    public String getChannels() {
        if (uri == null) {
            return null;
        }

        String channelString = uri.getPath();
        if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
            channelString += "?" + uri.getRawQuery();
        }

        if (uri.getRawFragment() != null && !uri.getRawFragment().isEmpty()) {
            channelString += "#" + uri.getRawFragment();
        }

        if (!channelString.isEmpty() && channelString.charAt(0) == '/') {
            channelString = channelString.substring(1);
        }

        return channelString;
    }
}
