/*
 * Copyright (c) 2006-2011 Chris Smith, Shane Mc Cormack, Gregory Holmes
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

package com.dmdirc.parser.common;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Map;

import javax.net.SocketFactory;

/**
 * A base parser which can construct a SocketFactory given socket-related
 * options.
 */
public abstract class BaseSocketAwareParser extends BaseParser {
    
    /** The IP address or hostname that this parser's sockets should bind to. */
    private String bindIp = null;
    
    /** The socket that was most recently created by this parser. */
    private Socket socket;
    
    /** The local port that this parser's *most recently created* socket bound to. */
    private int localPort = -1;

    /**
     * Creates a new base parser for the specified URI.
     *
     * @param uri The URI this parser will connect to.
     * @param implementations A map of interface implementations for this parser
     */
    public BaseSocketAwareParser(final URI uri, final Map<Class<?>, Class<?>> implementations) {
        super(uri, implementations);
    }

    /** {@inheritDoc} */
    @Override
    public String getBindIP() {
        return bindIp;
    }
    
    /** {@inheritDoc} */
    @Override
    public void setBindIP(final String ip) {
        bindIp = ip;
    }
    
    /** {@inheritDoc} */
    @Override
    public int getLocalPort() {
        if (localPort == -1 && socket != null) {
            // Try to update the local port from the socket, as it may have
            // bound since the last time we tried
            localPort = socket.getLocalPort();
        }

        return localPort;
    }
    
    /**
     * Convenience method to record the local port of the specified socket.
     *
     * @param socket The socket whose port is being recorded
     * @return The socket reference passed in, for convenience
     */
    private Socket handleSocket(final Socket socket) {
        // Store the socket as it might not have been bound yet
        this.socket = socket;
        this.localPort = socket.getLocalPort();

        return socket;
    }

    /**
     * Creates a socket factory that can be used by this parser.
     *
     * @return An appropriately configured socket factory
     */
    protected SocketFactory getSocketFactory() {
        return new SocketFactory() {

            /** {@inheritDoc} */
            @Override
            public Socket createSocket(final String host, final int port) throws IOException {
                if (bindIp == null) {
                    return handleSocket(new Socket(host, port));
                } else {
                    return handleSocket(new Socket(host, port, InetAddress.getByName(bindIp), 0));
                }
            }
            
            /** {@inheritDoc} */
            @Override
            public Socket createSocket(final InetAddress host, final int port) throws IOException {
                if (bindIp == null) {
                    return handleSocket(new Socket(host, port));
                } else {
                    return handleSocket(new Socket(host, port, InetAddress.getByName(bindIp), 0));
                }
            }

            /** {@inheritDoc} */
            @Override
            public Socket createSocket(final String host, final int port,
                    final InetAddress localHost, final int localPort) throws IOException {
                return handleSocket(new Socket(host, port,
                        bindIp == null ? localHost : InetAddress.getByName(bindIp), localPort));
            }

            /** {@inheritDoc} */
            @Override
            public Socket createSocket(final InetAddress address,
                    final int port, final InetAddress localAddress,
                    final int localPort) throws IOException {
                return handleSocket(new Socket(address, port,
                        bindIp == null ? localAddress : InetAddress.getByName(bindIp), localPort));
            }
            
        };
    }
}
