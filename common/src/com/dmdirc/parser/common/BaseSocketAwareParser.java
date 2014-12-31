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

package com.dmdirc.parser.common;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import javax.net.SocketFactory;

/**
 * A base parser which can construct a SocketFactory given socket-related
 * options.
 */
public abstract class BaseSocketAwareParser extends BaseParser {

    /** The socket that was most recently created by this parser. */
    private Socket socket;

    /** The local port that this parser's *most recently created* socket bound to. */
    private int localPort = -1;

    /** The connection timeout, in milliseconds. */
    private int connectTimeout = 5000;

    /**
     * Creates a new base parser for the specified URI.
     *
     * @param uri The URI this parser will connect to.
     */
    public BaseSocketAwareParser(final URI uri) {
        super(uri);
    }

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
     * Gets the current connection timeout.
     *
     * @return The connection timeout, in milliseconds.
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the connection timeout.
     *
     * @param connectTimeout The connection timeout, in milliseconds.
     */
    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Creates a socket factory that can be used by this parser.
     *
     * @return An appropriately configured socket factory
     */
    protected SocketFactory getSocketFactory() {
        return new BindingSocketFactory();
    }

    /**
     * Stores a reference to a recently created socket.
     *
     * @param socket The newly created socket.
     */
    private void setSocket(final Socket socket) {
        this.socket = socket;
        this.localPort = socket.getLocalPort();
    }

    /**
     * Allows subclasses to handle socket-related debug messages.
     *
     * @param message The debug message.
     */
    protected void handleSocketDebug(final String message) {
        // Do nothing by default
    }

    /**
     * Creates and binds a new socket to the IP address specified by the parser. If the target
     * address is an IPv6 address, the parser's {@link #getBindIPv6()} value will be used;
     * otherwise, the standard {@link #getBindIP()} will be used.
     *
     * @param host The host to connect to.
     * @param port The port to connect on.
     * @return A new socket bound appropriately and connected.
     */
    @SuppressWarnings({"resource", "SocketOpenedButNotSafelyClosed"})
    private Socket boundSocket(final InetAddress host, final int port) throws IOException {
        final Socket socket = new Socket();
        final String bindIp = host instanceof Inet6Address ? getBindIPv6() : getBindIP();

        if (bindIp != null && !bindIp.isEmpty()) {
            try {
                socket.bind(new InetSocketAddress(InetAddress.getByName(bindIp), 0));
            } catch (IOException ex) {
                // Bind failed; continue trying to connect anyway.
                handleSocketDebug("Binding failed: " + ex.getMessage());
            }
        }
        setSocket(socket);
        socket.connect(new InetSocketAddress(host, port), connectTimeout);
        return socket;
    }

    /**
     * Creates a new socket via a proxy.
     *
     * @param host The host to connect to.
     * @param port The port to connect on.
     * @return A new proxy-using socket.
     */
    @SuppressWarnings({"resource", "SocketOpenedButNotSafelyClosed"})
    private Socket proxiedSocket(final InetAddress host, final int port) throws IOException {
        final URI proxy = getProxy();
        final Proxy.Type proxyType = Proxy.Type.valueOf(proxy.getScheme().toUpperCase());
        final String proxyHost = proxy.getHost();
        final int proxyPort = checkPort(proxy.getPort(), "Proxy");

        final Socket socket = new Socket(
                new Proxy(proxyType, new InetSocketAddress(proxyHost, proxyPort)));

        try {
            final ProxyAuthenticator ia = ProxyAuthenticator.getProxyAuthenticator();
            final URI serverUri = new URI(null, null, host.getHostName(), port,
                    null, null, null);
            try {
                ia.getSemaphore().acquireUninterruptibly();
                ia.addAuthentication(serverUri, proxy);
                socket.connect(new InetSocketAddress(host, port), connectTimeout);
            } finally {
                ia.removeAuthentication(serverUri, proxy);
                ia.getSemaphore().release();
            }
        } catch (URISyntaxException ex) {
            // Won't happen.
        }

        return socket;
    }

    /**
     * Utility method to ensure a port is in the correct range. This stops networking classes
     * throwing obscure exceptions.
     *
     * @param port The port to test.
     * @param description Description of the port for error messages.
     * @return The given port.
     * @throws IOException If the port is out of range.
     */
    private int checkPort(final int port, final String description) throws IOException {
        if (port > 65535 || port <= 0) {
            throw new IOException(description + " port (" + port + ") is invalid.");
        }
        return port;
    }

    /**
     * Finds an address to connect to of the specified type.
     *
     * @param host The host to resolve.
     * @param type The type of address to look for
     * @return An address of the specified type, or {@code null} if none exists.
     * @throws IOException if there is an error.
     */
    private InetAddress getAddressForHost(final String host,
            final Class<? extends InetAddress> type) throws IOException {
        if (getProxy() != null) {
            // If we have a proxy, let it worry about all this instead.
            //
            // 1) We have no idea what sort of connectivity the proxy has
            // 2) If we do this here, then any DNS-based geo-balancing is
            //    going to be based on our location, not the proxy.
            return InetAddress.getByName(host);
        }

        for (InetAddress i : InetAddress.getAllByName(host)) {
            if (type.isAssignableFrom(i.getClass())) {
                return i;
            }
        }

        return null;
    }

    private class BindingSocketFactory extends SocketFactory {

        @Override
        public Socket createSocket(final String host, final int port) throws IOException {
            // Attempt to connect to IPv6 addresses first.
            IOException sixException = null;
            try {
                final InetAddress sixAddress = getAddressForHost(host, Inet6Address.class);
                if (sixAddress != null) {
                    return createSocket(sixAddress, port);
                }
            } catch (IOException ex) {
                handleSocketDebug("Unable to use IPv6: " + ex.getMessage());
                sixException = ex;
            }

            // If there isn't an IPv6 address, or it has failed, fall back to IPv4.
            final InetAddress fourAddress = getAddressForHost(host, Inet4Address.class);
            if (fourAddress == null) {
                throw new IOException("No IPv4 address and IPv6 failed", sixException);
            }
            return createSocket(fourAddress, port);
        }

        @Override
        @SuppressWarnings("resource")
        public Socket createSocket(final InetAddress host, final int port) throws IOException {
            checkPort(port, "server");
            return getProxy() == null ? boundSocket(host, port) : proxiedSocket(host, port);
        }

        @Override
        public Socket createSocket(final String host, final int port,
                final InetAddress localHost, final int localPort) throws IOException {
            return createSocket(host, port);
        }

        @Override
        public Socket createSocket(final InetAddress address, final int port,
                final InetAddress localAddress, final int localPort) throws IOException {
            return createSocket(address, port);
        }

    }
}
