/*
 * Copyright (c) 2006-2013 DMDirc Developers
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

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * Handles proxy authentication for the parser.
 *
 * @see IRCParser
 */
public final class IRCAuthenticator extends Authenticator {

    /**
     * A version number for this class. It should be changed whenever the class
     * structure is changed (or anything else that would prevent serialized
     * objects being unserialized with the new class).
     */
    private static final long serialVersionUID = 1;
    /** Singleton instance of IRCAuthenticator. */
    private static IRCAuthenticator me;
    /** List of authentication replies. */
    private final Map<String, PasswordAuthentication> replies = new HashMap<String, PasswordAuthentication>();
    /** List of servers for each host. */
    private final Map<String, List<URI>> owners = new HashMap<String, List<URI>>();
    /** Semaphore for connection limiting. */
    private final Semaphore mySemaphore = new Semaphore(1, true);

    /**
     * Create a new IRCAuthenticator.
     *
     * This creates an IRCAuthenticator and registers it as the default
     * Authenticator.
     */
    private IRCAuthenticator() {
        super();

        Authenticator.setDefault(this);
    }

    /**
     * Get the instance of IRCAuthenticator.
     *
     * @return The IRCAuthenticator instance.
     */
    public static synchronized IRCAuthenticator getIRCAuthenticator() {
        if (me == null) {
            me = new IRCAuthenticator();
        } else {
            // Make ourself the default authenticator again just incase.
            Authenticator.setDefault(me);
        }
        return me;
    }

    /**
     * Add a server to authenticate for.
     *
     * @param server ServerInfo object with server details.
     * @param proxy The URI of the proxy that is being used.
     */
    public void addAuthentication(final URI server, final URI proxy) {
        final String userInfo = proxy.getUserInfo();
        int offset;

        if (userInfo == null || (offset = userInfo.indexOf(':')) == -1) {
            return;
        }

        final String username = userInfo.substring(0, offset);
        final String password = userInfo.substring(offset + 1);

        final String host = proxy.getHost();
        final int port = proxy.getPort();
        final PasswordAuthentication pass = new PasswordAuthentication(username, password.toCharArray());
        final String fullhost = host.toLowerCase() + ":" + port;

        // Delete old username/password if one exists and then add the new one
        replies.remove(fullhost);
        replies.put(fullhost, pass);

        // Store which servers are associated with which proxy
        final List<URI> servers = owners.containsKey(fullhost) ? owners.get(fullhost) : new ArrayList<URI>();
        owners.remove(fullhost);
        servers.add(server);
        owners.put(fullhost, servers);
    }

    /**
     * Get a copy of the semaphore.
     *
     * @return the IRCAuthenticator semaphore.
     */
    public Semaphore getSemaphore() {
        return mySemaphore;
    }

    /**
     * Remove a server to authenticate for.
     *
     * @param server ServerInfo object with server details.
     * @param proxy The proxy that was in use for the given server.
     */
    public void removeAuthentication(final URI server, final URI proxy) {
        final String host = proxy.getHost();
        final int port = proxy.getPort();

        final String fullhost = host.toLowerCase() + ":" + port;

        // See if any other servers are associated with this proxy.
        final List<URI> servers = owners.containsKey(fullhost) ? owners.get(fullhost) : new ArrayList<URI>();
        servers.remove(server);

        if (servers.isEmpty()) {
            // No more servers need this authentication info, remove.
            owners.remove(fullhost);
            replies.remove(fullhost);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        /*
         * getRequestingHost: 85.234.138.2
         * getRequestingPort: 1080
         * getRequestingPrompt: SOCKS authentication
         * getRequestingProtocol: SOCKS5
         * getRequestingScheme: null
         * getRequestingSite: /85.234.138.2
         * getRequestingURL: null
         * getRequestorType: SERVER
         */

        final String fullhost = getRequestingHost().toLowerCase() + ":" + getRequestingPort();
        return replies.get(fullhost);
    }
}
