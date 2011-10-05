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

import java.net.URI;

/**
 * Implements configuration information for the parser
 *
 *
 * @author chris
 */
public interface ServerInfo {

    /**
     * Get the URI for this ServerInfo.
     * This will return a new URI based on this ServerInfo.
     * Protocol/Password/Host and Port are derived from the getXXXXX() methods
     * the path, query and fragment from the
     *
     * @return URI for this ServerInfo
     */
    URI getURI();

    /**
     * Get the hostname.
     *
     * @return Current hostname
     */
    String getHost();

    /**
     * Get the port.
     *
     * @return Current port
     */
    int getPort();

    /**
     * Get the password.
     *
     * @return Current Password
     */
    String getPassword();

    /**
     * Get if the server uses ssl.
     *
     * @return true if server uses ssl, else false
     */
    boolean isSSL();

    /**
     * Set if we are connecting via a socks proxy.
     *
     * @param newValue true if we are using socks, else false
     */
    void setUseSocks(final boolean newValue);

    /**
     * Get if we are connecting via a socks proxy.
     *
     * @return true if we are using socks, else false
     */
    boolean getUseSocks();

    /**
     * Set the Proxy hostname.
     *
     * @param newValue Value to set to.
     */
    void setProxyHost(final String newValue);

    /**
     * Get the Proxy hostname.
     *
     * @return Current Proxy hostname
     */
    String getProxyHost();

    /**
     * Set the Proxy port.
     *
     * @param newValue Value to set to.
     */
    void setProxyPort(final int newValue);

    /**
     * Get the Proxy port.
     *
     * @return Current Proxy port
     */
    int getProxyPort();

    /**
     * Set the Proxy username.
     *
     * @param newValue Value to set to.
     */
    void setProxyUser(final String newValue);

    /**
     * Get the Proxy username.
     *
     * @return Current Proxy username
     */
    String getProxyUser();

    /**
     * Set the Proxy password.
     *
     * @param newValue Value to set to.
     */
    void setProxyPass(final String newValue);

    /**
     * Get the Proxy password.
     *
     * @return Current Proxy password
     */
    String getProxyPass();

    /**
     * Retrieves a String describing any channels included in this ServerInfo's
     * URI.
     *
     * @return A channel string for this URI, or null if no URI specified
     * @since 0.6.3
     */
    String getChannels();
}
