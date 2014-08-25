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

import com.dmdirc.parser.common.BaseParser;
import com.dmdirc.parser.common.ChannelJoinRequest;
import com.dmdirc.parser.common.ChildImplementations;
import com.dmdirc.parser.common.CompositionState;
import com.dmdirc.parser.common.IgnoreList;
import com.dmdirc.parser.common.MyInfo;
import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.common.QueuePriority;
import com.dmdirc.parser.common.SRVRecord;
import com.dmdirc.parser.common.SystemEncoder;
import com.dmdirc.parser.interfaces.Encoder;
import com.dmdirc.parser.interfaces.EncodingParser;
import com.dmdirc.parser.interfaces.SecureParser;
import com.dmdirc.parser.interfaces.callbacks.ConnectErrorListener;
import com.dmdirc.parser.interfaces.callbacks.DataInListener;
import com.dmdirc.parser.interfaces.callbacks.DataOutListener;
import com.dmdirc.parser.interfaces.callbacks.DebugInfoListener;
import com.dmdirc.parser.interfaces.callbacks.ErrorInfoListener;
import com.dmdirc.parser.interfaces.callbacks.PingFailureListener;
import com.dmdirc.parser.interfaces.callbacks.PingSentListener;
import com.dmdirc.parser.interfaces.callbacks.PingSuccessListener;
import com.dmdirc.parser.interfaces.callbacks.ServerErrorListener;
import com.dmdirc.parser.interfaces.callbacks.ServerReadyListener;
import com.dmdirc.parser.interfaces.callbacks.SocketCloseListener;
import com.dmdirc.parser.irc.IRCReader.ReadLine;
import com.dmdirc.parser.irc.outputqueue.OutputQueue;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * IRC Parser.
 */
@ChildImplementations({
    IRCChannelClientInfo.class,
    IRCChannelInfo.class,
    IRCClientInfo.class
})
public class IRCParser extends BaseParser implements SecureParser, EncodingParser {

    /** Max length an outgoing line should be (NOT including \r\n). */
    public static final int MAX_LINELENGTH = 510;
    /** General Debug Information. */
    public static final int DEBUG_INFO = 1;
    /** Socket Debug Information. */
    public static final int DEBUG_SOCKET = 2;
    /** Processing Manager Debug Information. */
    public static final int DEBUG_PROCESSOR = 4;
    /** List Mode Queue Debug Information. */
    public static final int DEBUG_LMQ = 8;

    /** Attempt to update user host all the time, not just on Who/Add/NickChange. */
    static final boolean ALWAYS_UPDATECLIENT = true;
    /** Byte used to show that a non-boolean mode is a list (b). */
    static final byte MODE_LIST = 1;
    /** Byte used to show that a non-boolean mode is not a list, and requires a parameter to set (lk). */
    static final byte MODE_SET = 2;
    /** Byte used to show that a non-boolean mode is not a list, and requires a parameter to unset (k). */
    static final byte MODE_UNSET = 4;

    /**
     * Default channel prefixes if none are specified by the IRCd.
     *
     * <p>These are the RFC 2811 specified prefixes: '#', '&amp;', '!' and '+'.
     */
    private static final String DEFAULT_CHAN_PREFIX = "#&!+";

    /**
     * This is what the user wants settings to be.
     * Nickname here is *not* always accurate.<br><br>
     * ClientInfo variable tParser.getMyself() should be used for accurate info.
     */
    private MyInfo me = new MyInfo();
    /** Should PINGs be sent to the server to check if its alive? */
    private boolean checkServerPing = true;
    /** Timer for server ping. */
    private Timer pingTimer = null;
    /** Semaphore for access to pingTimer. */
    private final Semaphore pingTimerSem = new Semaphore(1);
    /** Is a ping needed? */
    private final AtomicBoolean pingNeeded = new AtomicBoolean(false);
    /** Time last ping was sent at. */
    private long pingTime;
    /** Current Server Lag. */
    private long serverLag;
    /** Last value sent as a ping argument. */
    private String lastPingValue = "";
    /**
     * Count down to next ping.
     * The timer fires every 10 seconds, this value is decreased every time the
     * timer fires.<br>
     * Once it reaches 0, we send a ping, and reset it to 6, this means we ping
     * the server every minute.
     *
     * @see #setPingTimerInterval
     */
    private int pingCountDown;

    /** Network name. This is "" if no network name is provided */
    String networkName;
    /** This is what we think the nickname should be. */
    String thinkNickname;
    /** When using inbuilt pre-001 NickInUse handler, have we tried our AltNick. */
    boolean triedAlt;
    /** Have we received the 001. */
    boolean got001;
    /** Have we fired post005? */
    boolean post005;
    /** Has the thread started execution yet, (Prevents run() being called multiple times). */
    boolean hasBegan;
    /** Connect timeout. */
    private int connectTimeout = 5000;
    /** Hashtable storing known prefix modes (ohv). */
    final Map<Character, Long> prefixModes = new HashMap<>();
    /**
     * Hashtable maping known prefix modes (ohv) to prefixes (@%+) - Both ways.
     * Prefix map contains 2 pairs for each mode. (eg @ => o and o => @)
     */
    final Map<Character, Character> prefixMap = new HashMap<>();
    /** Integer representing the next avaliable integer value of a prefix mode. */
    long nextKeyPrefix = 1;
    /** Hashtable storing known user modes (owxis etc). */
    final Map<Character, Long> userModes = new HashMap<>();
    /** Integer representing the next avaliable integer value of a User mode. */
    long nNextKeyUser = 1;
    /**
     * Hashtable storing known boolean chan modes (cntmi etc).
     * Valid Boolean Modes are stored as Hashtable.pub('m',1); where 'm' is the mode and 1 is a numeric value.<br><br>
     * Numeric values are powers of 2. This allows up to 32 modes at present (expandable to 64)<br><br>
     * ChannelInfo/ChannelClientInfo etc provide methods to view the modes in a human way.<br><br>
     * <br>
     * Channel modes discovered but not listed in 005 are stored as boolean modes automatically (and a ERROR_WARNING Error is called)
     */
    final Map<Character, Long> chanModesBool = new HashMap<>();
    /** Integer representing the next avaliable integer value of a Boolean mode. */
    long nextKeyCMBool = 1;
    /**
     * Hashtable storing known non-boolean chan modes (klbeI etc).
     * Non Boolean Modes (for Channels) are stored together in this hashtable, the value param
     * is used to show the type of variable. (List (1), Param just for set (2), Param for Set and Unset (2+4=6))<br><br>
     * <br>
     * see MODE_LIST<br>
     * see MODE_SET<br>
     * see MODE_UNSET<br>
     */
    final Map<Character, Byte> chanModesOther = new HashMap<>();
    /** The last line of input received from the server */
    private ReadLine lastLine = null;
    /** Should the lastline (where given) be appended to the "data" part of any onErrorInfo call? */
    private boolean addLastLine = false;
    /** Channel Prefixes (ie # + etc). */
    private String chanPrefix = DEFAULT_CHAN_PREFIX;
    /** Hashtable storing all known clients based on nickname (in lowercase). */
    private final Map<String, IRCClientInfo> clientList = new HashMap<>();
    /** Hashtable storing all known channels based on chanel name (inc prefix - in lowercase). */
    private final Map<String, IRCChannelInfo> channelList = new HashMap<>();
    /** Reference to the ClientInfo object that references ourself. */
    private IRCClientInfo myself = new IRCClientInfo(this, "myself").setFake(true);
    /** Hashtable storing all information gathered from 005. */
    final Map<String, String> h005Info = new HashMap<>();
    /** difference in ms between our time and the servers time (used for timestampedIRC). */
    long tsdiff;
    /** Reference to the Processing Manager. */
    private final ProcessingManager myProcessingManager = new ProcessingManager(this);
    /** Should we automatically disconnect on fatal errors?. */
    private boolean disconnectOnFatal = true;
    /** Current Socket State. */
    protected SocketState currentSocketState = SocketState.NULL;
    /** This is the socket used for reading from/writing to the IRC server. */
    private Socket socket;
    /**
     * The underlying socket used for reading/writing to the IRC server.
     * For normal sockets this will be the same as {@link #socket} but for SSL
     * connections this will be the underlying {@link Socket} while
     * {@link #socket} will be an {@link SSLSocket}.
     */
    private Socket rawSocket;
    /** Used for writing to the server. */
    private OutputQueue out;
    /** The encoder to use to encode incoming lines. */
    private Encoder encoder = new SystemEncoder();
    /** Used for reading from the server. */
    private IRCReader in;
    /** This is the default TrustManager for SSL Sockets, it trusts all ssl certs. */
    private final TrustManager[] trustAllCerts = {
        new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
            }
        },};
    /** Should channels automatically request list modes? */
    private boolean autoListMode = true;
    /** Should part/quit/kick callbacks be fired before removing the user internally? */
    private boolean removeAfterCallback = true;
    /** This is the TrustManager used for SSL Sockets. */
    private TrustManager[] myTrustManager = trustAllCerts;
    /** The KeyManagers used for client certificates for SSL sockets. */
    private KeyManager[] myKeyManagers;
    /** This is list containing 001 - 005 inclusive. */
    private final List<String> serverInformationLines = new LinkedList<>();
    /** Map of capabilities and their state. */
    private final Map<String, CapabilityState> capabilities = new HashMap<>();

    /**
     * Default constructor, ServerInfo and MyInfo need to be added separately (using IRC.me and IRC.server).
     */
    public IRCParser() {
        this((MyInfo) null);
    }

    /**
     * Constructor with ServerInfo, MyInfo needs to be added separately (using IRC.me).
     *
     * @param uri The URI to connect to
     */
    public IRCParser(final URI uri) {
        this(null, uri);
    }

    /**
     * Constructor with MyInfo, ServerInfo needs to be added separately (using IRC.server).
     *
     * @param myDetails Client information.
     */
    public IRCParser(final MyInfo myDetails) {
        this(myDetails, null);
    }

    /**
     * Creates a new IRCParser with the specified client details which will
     * connect to the specified URI.
     *
     * @since 0.6.3
     * @param myDetails The client details to use
     * @param uri The URI to connect to
     */
    public IRCParser(final MyInfo myDetails, final URI uri) {
        super(uri);

        out = new OutputQueue();
        if (myDetails != null) {
            this.me = myDetails;
        }

        setIgnoreList(new IgnoreList());
        setPingTimerInterval(10000);
        setPingTimerFraction(6);
        resetState();
    }

    /**
     * Get the current OutputQueue
     *
     * @return the current OutputQueue
     */
    public OutputQueue getOutputQueue() {
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public boolean compareURI(final URI uri) {
        // Get the old URI.
        final URI oldURI = getURI();

        // Check that protocol, host and port are the same.
        // Anything else won't change the server we connect to just what we
        // would do after connecting, so is not relevent.
        return uri.getScheme().equalsIgnoreCase(oldURI.getScheme())
                && uri.getHost().equalsIgnoreCase(oldURI.getHost())
                && ((uri.getUserInfo() == null || uri.getUserInfo().isEmpty()) || uri.getUserInfo().equalsIgnoreCase((oldURI.getUserInfo() == null ? "" : oldURI.getUserInfo())))
                && uri.getPort() == oldURI.getPort();
    }

    /**
     * From the given URI, get a URI to actually connect to.
     * This function will check for DNS SRV records for the given URI and use
     * those if found.
     * If no SRV records exist, then fallback to using the URI as-is but with
     * a default port specified if none is given.
     *
     * @param uri Requested URI.
     * @return A connectable version of the given URI.
     */
    private URI getConnectURI(final URI uri) {
        if (uri == null) { return null; }

        final boolean isSSL = uri.getScheme().endsWith("s");
        final int defaultPort = isSSL ? 6697 : 6667;

        // Default to what the URI has already..
        int port = uri.getPort();
        String host = uri.getHost();

        // Look for SRV records if no port is specified.
        if (port == -1) {
            List<SRVRecord> recordList = new ArrayList<>();
            if (isSSL) {
                // There are a few possibilities for ssl...
                final String[] protocols = {"_ircs._tcp.", "_irc._tls."};
                for (final String protocol : protocols) {
                    recordList = SRVRecord.getRecords(protocol + host);
                    if (!recordList.isEmpty()) {
                        break;
                    }
                }
            } else {
                recordList = SRVRecord.getRecords("_irc._tcp." + host);
            }
            if (!recordList.isEmpty()) {
                host = recordList.get(0).getHost();
                port = recordList.get(0).getPort();
            }
        }

        // Fix the port if required.
        if (port == -1) { port = defaultPort; }

        // Return the URI to connect to based on the above.
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), host, port, uri.getPath(), uri.getQuery(), uri.getFragment());
        } catch (URISyntaxException ex) {
            // Shouldn't happen - but return the URI as-is if it does.
            return uri;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends ChannelJoinRequest> extractChannels(final URI uri) {
        if (uri == null) {
            return Collections.<ChannelJoinRequest>emptyList();
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

        return extractChannels(channelString);
    }

    /**
     * Extracts a set of channels and optional keys from the specified String.
     * Channels are separated by commas, and keys are separated from their
     * channels by a space.
     *
     * @since 0.6.4
     * @param channels The string of channels to parse
     * @return A corresponding collection of join request objects
     */
    protected Collection<? extends ChannelJoinRequest> extractChannels(final String channels) {
        final List<ChannelJoinRequest> res = new ArrayList<>();

        for (String channel : channels.split(",")) {
            final String[] parts = channel.split(" ", 2);

            if (parts.length == 2) {
                res.add(new ChannelJoinRequest(parts[0], parts[1]));
            } else {
                res.add(new ChannelJoinRequest(parts[0]));
            }
        }

        return res;
    }

    /**
     * Get the current Value of autoListMode.
     *
     * @return Value of autoListMode (true if channels automatically ask for list modes on join, else false)
     */
    public boolean getAutoListMode() {
        return autoListMode;
    }

    /**
     * Set the current Value of autoListMode.
     *
     * @param newValue New value to set autoListMode
     */
    public void setAutoListMode(final boolean newValue) {
        autoListMode = newValue;
    }

    /**
     * Get the current Value of removeAfterCallback.
     *
     * @return Value of removeAfterCallback (true if kick/part/quit callbacks are fired before internal removal)
     */
    public boolean getRemoveAfterCallback() {
        return removeAfterCallback;
    }

    /**
     * Get the current Value of removeAfterCallback.
     *
     * @param newValue New value to set removeAfterCallback
     */
    public void setRemoveAfterCallback(final boolean newValue) {
        removeAfterCallback = newValue;
    }

    /**
     * Get the current Value of addLastLine.
     *
     * @return Value of addLastLine (true if lastLine info will be automatically
     *         added to the errorInfo data line). This should be true if lastLine
     *         isn't handled any other way.
     */
    public boolean getAddLastLine() {
        return addLastLine;
    }

    /**
     * Get the current Value of addLastLine.
     *
     * @param newValue New value to set addLastLine
     */
    public void setAddLastLine(final boolean newValue) {
        addLastLine = newValue;
    }

    /**
     * Get the current Value of connectTimeout.
     *
     * @return The value of getConnectTimeout.
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Set the Value of connectTimeout.
     *
     * @param newValue new value for value of getConnectTimeout.
     */
    public void setConnectTimeout(final int newValue) {
        connectTimeout = newValue;
    }

    /**
     * Get the current socket State.
     *
     * @since 0.6.3m1
     * @return Current {@link SocketState}
     */
    public SocketState getSocketState() {
        return currentSocketState;
    }

    /**
     * Get a reference to the Processing Manager.
     *
     * @return Reference to the CallbackManager
     */
    public ProcessingManager getProcessingManager() {
        return myProcessingManager;
    }

    /**
     * Get a reference to the default TrustManager for SSL Sockets.
     *
     * @return a reference to trustAllCerts
     */
    public TrustManager[] getDefaultTrustManager() {
        return Arrays.copyOf(trustAllCerts, trustAllCerts.length);
    }

    /**
     * Get a reference to the current TrustManager for SSL Sockets.
     *
     * @return a reference to myTrustManager;
     */
    public TrustManager[] getTrustManager() {
        return Arrays.copyOf(myTrustManager, myTrustManager.length);
    }

    /** {@inheritDoc} */
    @Override
    public void setTrustManagers(final TrustManager[] managers) {
        myTrustManager = managers == null ? null : Arrays.copyOf(managers, managers.length);
    }

    /** {@inheritDoc} */
    @Override
    public void setKeyManagers(final KeyManager[] managers) {
        myKeyManagers = managers == null ? null : Arrays.copyOf(managers, managers.length);
    }

    //---------------------------------------------------------------------------
    // Start Callbacks
    //---------------------------------------------------------------------------

    /**
     * Callback to all objects implementing the ServerError Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.ServerErrorListener
     * @param message The error message
     */
    protected void callServerError(final String message) {
        getCallback(ServerErrorListener.class).onServerError(null, null, message);
    }

    /**
     * Callback to all objects implementing the DataIn Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.DataInListener
     * @param data Incoming Line.
     */
    protected void callDataIn(final String data) {
        getCallback(DataInListener.class).onDataIn(null, null, data);
    }

    /**
     * Callback to all objects implementing the DataOut Callback.
     *
     * @param data Outgoing Data
     * @param fromParser True if parser sent the data, false if sent using .sendLine
     * @see com.dmdirc.parser.interfaces.callbacks.DataOutListener
     */
    protected void callDataOut(final String data, final boolean fromParser) {
        getCallback(DataOutListener.class).onDataOut(null, null, data, fromParser);
    }

    /**
     * Callback to all objects implementing the DebugInfo Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.DebugInfoListener
     * @param level Debugging Level (DEBUG_INFO, DEBUG_SOCKET etc)
     * @param data Debugging Information as a format string
     * @param args Formatting String Options
     */
    protected void callDebugInfo(final int level, final String data, final Object... args) {
        callDebugInfo(level, String.format(data, args));
    }

    /**
     * Callback to all objects implementing the DebugInfo Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.DebugInfoListener
     * @param level Debugging Level (DEBUG_INFO, DEBUG_SOCKET etc)
     * @param data Debugging Information
     */
    protected void callDebugInfo(final int level, final String data) {
        getCallback(DebugInfoListener.class).onDebugInfo(null, null, level, data);
    }

    /**
     * Callback to all objects implementing the IErrorInfo Interface.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.ErrorInfoListener
     * @param errorInfo ParserError object representing the error.
     */
    protected void callErrorInfo(final ParserError errorInfo) {
        getCallback(ErrorInfoListener.class).onErrorInfo(null, null, errorInfo);
    }

    /**
     * Callback to all objects implementing the IConnectError Interface.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.ConnectErrorListener
     * @param errorInfo ParserError object representing the error.
     */
    protected void callConnectError(final ParserError errorInfo) {
        getCallback(ConnectErrorListener.class).onConnectError(null, null, errorInfo);
    }

    /**
     * Callback to all objects implementing the SocketClosed Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.SocketCloseListener
     */
    protected void callSocketClosed() {
        getCallback(SocketCloseListener.class).onSocketClosed(null, null);
    }

    /**
     * Callback to all objects implementing the PingFailed Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.PingFailureListener
     * @return True if any callback was called, false otherwise.
     */
    protected boolean callPingFailed() {
        return getCallbackManager().getCallbackType(PingFailureListener.class).call();
    }

    /**
     * Callback to all objects implementing the PingSent Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.PingSentListener
     */
    protected void callPingSent() {
        getCallback(PingSentListener.class).onPingSent(null, null);
    }

    /**
     * Callback to all objects implementing the PingSuccess Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.PingSuccessListener
     */
    protected void callPingSuccess() {
        getCallback(PingSuccessListener.class).onPingSuccess(null, null);
    }

    /**
     * Callback to all objects implementing the Post005 Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.ServerReadyListener
     */
    protected synchronized void callPost005() {
        if (post005) {
            return;
        }

        post005 = true;

        if (!h005Info.containsKey("PREFIX")) {
            parsePrefixModes();
        }
        if (!h005Info.containsKey("USERMODES")) {
            parseUserModes();
        }
        if (!h005Info.containsKey("CHANMODES")) {
            parseChanModes();
        }

        getCallback(ServerReadyListener.class).onServerReady(null, null);
    }

    //---------------------------------------------------------------------------
    // End Callbacks
    //---------------------------------------------------------------------------
    /** Reset internal state (use before doConnect). */
    private void resetState() {
        // Reset General State info
        triedAlt = false;
        got001 = false;
        post005 = false;
        // Clear the hash tables
        channelList.clear();
        clientList.clear();
        h005Info.clear();
        prefixModes.clear();
        prefixMap.clear();
        chanModesOther.clear();
        chanModesBool.clear();
        userModes.clear();
        chanPrefix = DEFAULT_CHAN_PREFIX;
        // Clear output queue.
        if (out != null) {
            out.clearQueue();
        }
        // Reset the mode indexes
        nextKeyPrefix = 1;
        nextKeyCMBool = 1;
        nNextKeyUser = 1;
        setServerName("");
        networkName = "";
        lastLine = null;
        myself = new IRCClientInfo(this, "myself").setFake(true);

        synchronized (serverInformationLines) {
            serverInformationLines.clear();
        }
        stopPingTimer();

        currentSocketState = SocketState.CLOSED;
        setEncoding(IRCEncoding.RFC1459);
    }

    /**
     * Called after other error callbacks.
     * CallbackOnErrorInfo automatically calls this *AFTER* any registered callbacks
     * for it are called.
     *
     * @param errorInfo ParserError object representing the error.
     * @param called True/False depending on the the success of other callbacks.
     */
    public void onPostErrorInfo(final ParserError errorInfo, final boolean called) {
        if (errorInfo.isFatal() && disconnectOnFatal) {
            disconnect("Fatal Parser Error");
        }
    }

    /**
     * Get the current Value of disconnectOnFatal.
     *
     * @return Value of disconnectOnFatal (true if the parser automatically disconnects on fatal errors, else false)
     */
    public boolean getDisconnectOnFatal() {
        return disconnectOnFatal;
    }

    /**
     * Set the current Value of disconnectOnFatal.
     *
     * @param newValue New value to set disconnectOnFatal
     */
    public void setDisconnectOnFatal(final boolean newValue) {
        disconnectOnFatal = newValue;
    }

    /**
     * Create a new Socket object for the given target, using the given proxy
     * if appropriate.
     *
     * @param target Target URI to connect to.
     * @param proxy Proxy URI to use
     * @return Socket, with IP Binding or Proxy as appropriate,
     * @throws IOException If there was an issue creating the socket.
     */
    private Socket newSocket(final URI target, final URI proxy) throws IOException {
        if (target.getPort() > 65535 || target.getPort() <= 0) {
            throw new IOException("Server port (" + target.getPort() + ") is invalid.");
        }

        Socket mySocket;
        if (proxy == null) {
            callDebugInfo(DEBUG_SOCKET, "Not using Proxy");
            mySocket = new Socket();

            final InetSocketAddress sockAddr = new InetSocketAddress(target.getHost(), target.getPort());

            if (sockAddr.getAddress() instanceof Inet6Address) {
                if (getBindIPv6() != null && !getBindIPv6().isEmpty()) {
                    callDebugInfo(DEBUG_SOCKET, "Binding to IPv6: " + getBindIP());
                    try {
                        mySocket.bind(new InetSocketAddress(InetAddress.getByName(getBindIPv6()), 0));
                    } catch (IOException e) {
                        callDebugInfo(DEBUG_SOCKET, "Binding failed: " + e.getMessage());
                    }
                }
            } else {
                if (getBindIP() != null && !getBindIP().isEmpty()) {
                    callDebugInfo(DEBUG_SOCKET, "Binding to IPv4: " + getBindIP());
                    try {
                        mySocket.bind(new InetSocketAddress(InetAddress.getByName(getBindIP()), 0));
                    } catch (IOException e) {
                        callDebugInfo(DEBUG_SOCKET, "Binding failed: " + e.getMessage());
                    }
                }
            }

            mySocket.connect(sockAddr, connectTimeout);
        } else {
            callDebugInfo(DEBUG_SOCKET, "Using Proxy");

            if ((getBindIP() != null && !getBindIP().isEmpty()) || (getBindIPv6() != null && !getBindIPv6().isEmpty())) {
                callDebugInfo(DEBUG_SOCKET, "IP Binding is not possible when using a proxy.");
            }

            final String proxyHost = proxy.getHost();
            final int proxyPort = proxy.getPort();

            if (proxyPort > 65535 || proxyPort <= 0) {
                throw new IOException("Proxy port (" + proxyPort + ") is invalid.");
            }

            final Proxy.Type proxyType = Proxy.Type.valueOf(proxy.getScheme().toUpperCase());
            mySocket = new Socket(new Proxy(proxyType, new InetSocketAddress(proxyHost, proxyPort)));

            final IRCAuthenticator ia = IRCAuthenticator.getIRCAuthenticator();

            try {
                try {
                    ia.getSemaphore().acquire();
                } catch (InterruptedException ex) {
                }

                ia.addAuthentication(target, proxy);
                mySocket.connect(new InetSocketAddress(target.getHost(), target.getPort()), connectTimeout);
            } finally {
                ia.removeAuthentication(target, proxy);
                ia.getSemaphore().release();
            }
        }

        return mySocket;
    }

    /**
     * Find a socket to connect using, this will where possible attempt IPv6
     * first, before falling back to IPv4.
     *
     * This will:
     *   - Try using v6 first before v4.
     *   - If there are any failures at all with v6 (including, not having a
     *     v6 address...) then fall through to v4.
     *   - If there is no v4 address, throw the v6 exception
     *   - Otherwise, try v4
     *   - If there are failures with v4, throw the exception.
     *
     * If we have both IPv6 and IPv4, and a target host has both IPv6 and IPv4
     * addresses, but does not listen on the requested port on either, this
     * will cause a double-length connection timeout.
     *
     * @param target Target URI to connect to.
     * @param proxy Proxy URI to use
     * @return Socket to use in future connections.
     * @throws IOException if there is an error.
     */
    private Socket findSocket(final URI target, final URI proxy) throws IOException {
        if (proxy != null) {
            // If we have a proxy, let it worry about all this instead.
            //
            // 1) We have no idea what sort of connectivity the proxy has
            // 2) If we do this here, then any DNS-based geo-balancing is
            //    going to be based on our location, not the proxy.
            return newSocket(target, proxy);
        }

        URI target6 = null;
        URI target4 = null;

        // Get the v6 and v4 addresses if appropriate..
        try {
            for (InetAddress i : InetAddress.getAllByName(target.getHost())) {
                if (target6 == null && i instanceof Inet6Address) {
                    target6 = new URI(target.getScheme(), target.getUserInfo(), i.getHostAddress(), target.getPort(), target.getPath(), target.getQuery(), target.getFragment());
                } else if (target4 == null && i instanceof Inet4Address) {
                    target4 = new URI(target.getScheme(), target.getUserInfo(), i.getHostAddress(), target.getPort(), target.getPath(), target.getQuery(), target.getFragment());
                }
            }
        } catch (final URISyntaxException use) { /* Won't happen. */ }

        // Now try and connect.
        // Try using v6 first before v4.
        // If there are any failures at all with v6 (including, not having a
        // v6 address...) then fall through to v4.
        // If there is no v4 address, throw the v6 exception
        // Otherwise, try v4
        // If there are failures with v4, throw the exception.
        //
        // If we have both IPv6 and IPv4, and a target host has both IPv6 and
        // IPv4 addresses, but does not listen on the requested port on either,
        // this will cause a double-length connection timeout.
        //
        // In future this may want to be rewritten to perform a happy-eyeballs
        // race rather than trying one then the other.
        Exception v6Exception = null;
        if (target6 != null) {
            try {
                return newSocket(target6, null);
            } catch (final IOException ioe) {
                if (target4 == null) {
                    throw ioe;
                }
            } catch (final Exception e) {
                v6Exception = e;
                callDebugInfo(DEBUG_SOCKET, "Exception trying to use IPv6: " + e);
            }
        }
        if (target4 != null) {
            try {
                return newSocket(target4, null);
            } catch (final IOException e2) {
                callDebugInfo(DEBUG_SOCKET, "Exception trying to use IPv4: " + e2);
                throw e2;
            }
        } else if (v6Exception != null) {
            throw new IOException("Error connecting to: " + target + " (IPv6 failure, with no IPv4 fallback)", v6Exception);
        } else {
            // We should never get here as if both target4 and target6 were null
            // getAllByName would have thrown an exception instead.
            throw new IOException("Error connecting to: " + target + " (General connectivity failure)");
        }
    }

    /**
     * Connect to IRC.
     *
     * @throws IOException if the socket can not be connected
     * @throws NoSuchAlgorithmException if SSL is not available
     * @throws KeyManagementException if the trustManager is invalid
     */
    private void doConnect() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        if (getURI() == null || getURI().getHost() == null) {
            throw new UnknownHostException("Unspecified host.");
        }

        resetState();
        callDebugInfo(DEBUG_SOCKET, "Connecting to " + getURI().getHost() + ":" + getURI().getPort());

        currentSocketState = SocketState.OPENING;
        socket = findSocket(getConnectURI(getURI()), getProxy());

        rawSocket = socket;

        if (getURI().getScheme().endsWith("s")) {
            callDebugInfo(DEBUG_SOCKET, "Server is SSL.");

            if (myTrustManager == null) {
                myTrustManager = trustAllCerts;
            }

            final SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(myKeyManagers, myTrustManager, new java.security.SecureRandom());

            final SSLSocketFactory socketFactory = sc.getSocketFactory();
            socket = socketFactory.createSocket(socket, getURI().getHost(), getURI().getPort(), false);

            // Manually start a handshake so we get proper SSL errors here,
            // and so that we can control the connection timeout
            final int timeout = socket.getSoTimeout();
            socket.setSoTimeout(10000);
            ((SSLSocket) socket).startHandshake();
            socket.setSoTimeout(timeout);

            currentSocketState = SocketState.OPENING;
        }

        callDebugInfo(DEBUG_SOCKET, "\t-> Opening socket output stream PrintWriter");
        out.setOutputStream(socket.getOutputStream());
        out.setQueueEnabled(true);
        currentSocketState = SocketState.OPEN;
        callDebugInfo(DEBUG_SOCKET, "\t-> Opening socket input stream BufferedReader");
        in = new IRCReader(socket.getInputStream(), encoder);
        callDebugInfo(DEBUG_SOCKET, "\t-> Socket Opened");
    }

    /**
     * Send server connection strings (NICK/USER/PASS).
     */
    protected void sendConnectionStrings() {
        sendString("CAP LS");
        if (getURI().getUserInfo() != null && !getURI().getUserInfo().isEmpty()) {
            sendString("PASS " + getURI().getUserInfo());
        }
        sendString("NICK " + me.getNickname());
        thinkNickname = me.getNickname();
        String localhost;
        try {
            localhost = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException uhe) {
            localhost = "*";
        }
        sendString("USER " + me.getUsername() + " " + localhost + " " + getURI().getHost() + " :" + me.getRealname());
    }

    /**
     * Handle an onConnect error.
     *
     * @param e Exception to handle
     * @param isUserError Is this a user error?
     */
    private void handleConnectException(final Exception e, final boolean isUserError) {
        callDebugInfo(DEBUG_SOCKET, "Error Connecting (" + e.getMessage() + "), Aborted");
        final ParserError ei = new ParserError(ParserError.ERROR_ERROR + (isUserError ? ParserError.ERROR_USER : 0), "Exception with server socket", getLastLine());
        ei.setException(e);
        callConnectError(ei);

        if (currentSocketState != SocketState.CLOSED) {
            currentSocketState = SocketState.CLOSED;
            callSocketClosed();
        }
        resetState();
    }

    /**
     * Begin execution.
     * Connect to server, and start parsing incoming lines
     */
    @Override
    public void run() {
        callDebugInfo(DEBUG_INFO, "Begin Thread Execution");
        if (hasBegan) {
            return;
        } else {
            hasBegan = true;
        }
        try {
            doConnect();
        } catch (IOException e) {
            handleConnectException(e, true);
            return;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            handleConnectException(e, false);
            return;
        }

        callDebugInfo(DEBUG_SOCKET, "Socket Connected");

        sendConnectionStrings();

        while (true) {
            try {
                lastLine = in.readLine(); // Blocking :/
                if (lastLine == null) {
                    if (currentSocketState != SocketState.CLOSED) {
                        currentSocketState = SocketState.CLOSED;
                        callSocketClosed();
                    }
                    resetState();
                    break;
                } else if (currentSocketState != SocketState.CLOSING) {
                    processLine(lastLine);
                }
            } catch (IOException e) {
                callDebugInfo(DEBUG_SOCKET, "Exception in main loop (" + e.getMessage() + "), Aborted");

                if (currentSocketState != SocketState.CLOSED) {
                    currentSocketState = SocketState.CLOSED;
                    callSocketClosed();
                }
                resetState();
                break;
            }
        }
        callDebugInfo(DEBUG_INFO, "End Thread Execution");
    }

    /** {@inheritDoc} */
    @Override
    public int getLocalPort() {
        if (currentSocketState == SocketState.OPENING || currentSocketState == SocketState.OPEN) {
            return socket.getLocalPort();
        } else {
            return 0;
        }
    }

    /** Close socket on destroy. */
    @Override
    protected void finalize() throws Throwable {
        try {
            // See note at disconnect() method for why we close rawSocket.
            if (rawSocket != null) {
                rawSocket.close();
            }
        } catch (IOException e) {
            callDebugInfo(DEBUG_SOCKET, "Could not close socket");
        }
        super.finalize();
    }

    /**
     * Get the trailing parameter for a line.
     * The parameter is everything after the first occurrence of " :" to the last token in the line after a space.
     *
     * @param line Line to get parameter for
     * @return Parameter of the line
     */
    public static String getParam(final String line) {
        String[] params = line.split(" :", 2);
        return params[params.length - 1];
    }

    /**
     * Tokenise a line.
     * splits by " " up to the first " :" everything after this is a single token
     *
     * @param line Line to tokenise
     * @return Array of tokens
     */
    public static String[] tokeniseLine(final String line) {
        if (line == null) {
            return new String[]{""};
        }

        final int lastarg = line.indexOf(" :");
        String[] tokens;

        if (lastarg > -1) {
            final String[] temp = line.substring(0, lastarg).split(" ");
            tokens = new String[temp.length + 1];
            System.arraycopy(temp, 0, tokens, 0, temp.length);
            tokens[temp.length] = line.substring(lastarg + 2);
        } else {
            tokens = line.split(" ");
        }

        if (tokens.length < 1) {
            tokens = new String[]{""};
        }

        return tokens;
    }

    /** {@inheritDoc} */
    @Override
    public IRCClientInfo getClient(final String details) {
        final String sWho = getStringConverter().toLowerCase(IRCClientInfo.parseHost(details));

        if (clientList.containsKey(sWho)) {
            return clientList.get(sWho);
        } else {
            return new IRCClientInfo(this, details).setFake(true);
        }
    }

    public boolean isKnownClient(final String host) {
        final String sWho = getStringConverter().toLowerCase(IRCClientInfo.parseHost(host));
        return clientList.containsKey(sWho);
    }

    /** {@inheritDoc} */
    @Override
    public IRCChannelInfo getChannel(final String channel) {
        synchronized (channelList) {
            return channelList.get(getStringConverter().toLowerCase(channel));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void sendInvite(final String channel, final String user) {
        sendRawMessage("INVITE " + user + " " + channel);
    }

    /** {@inheritDoc} */
    @Override
    public void sendRawMessage(final String message) {
        doSendString(message, QueuePriority.NORMAL, false);
    }

    /** {@inheritDoc} */
    @Override
    public void sendRawMessage(final String message, final QueuePriority priority) {
        doSendString(message, priority, false);
    }

    /**
     * Send a line to the server and add proper line ending.
     *
     * @param line Line to send (\r\n termination is added automatically)
     * @return True if line was sent, else false.
     */
    protected boolean sendString(final String line) {
        return doSendString(line, QueuePriority.NORMAL, true);
    }

    /**
     * Send a line to the server and add proper line ending.
     * If a non-empty argument is given, it is appended as a trailing argument
     * (i.e., separated by " :"); otherwise, the line is sent as-is.
     *
     * @param line Line to send
     * @param argument Trailing argument for the command, if any
     * @return True if line was sent, else false.
     */
    protected boolean sendString(final String line, final String argument) {
        return sendString(argument.isEmpty() ? line : line + " :" + argument);
    }

    /**
     * Send a line to the server and add proper line ending.
     *
     * @param line Line to send (\r\n termination is added automatically)
     * @param priority Priority of this line.
     * @return True if line was sent, else false.
     */
    protected boolean sendString(final String line, final QueuePriority priority) {
        return doSendString(line, priority, true);
    }

    /**
     * Send a line to the server and add proper line ending.
     *
     * @param line Line to send (\r\n termination is added automatically)
     * @param priority Priority of this line.
     * @param fromParser is this line from the parser? (used for callDataOut)
     * @return True if line was sent, else false.
     */
    protected boolean doSendString(final String line, final QueuePriority priority, final boolean fromParser) {
        if (out == null || getSocketState() != SocketState.OPEN) {
            return false;
        }
        callDataOut(line, fromParser);
        out.sendLine(line, priority);
        final String[] newLine = tokeniseLine(line);
        if (newLine[0].equalsIgnoreCase("away") && newLine.length > 1) {
            myself.setAwayReason(newLine[newLine.length - 1]);
        } else if (newLine[0].equalsIgnoreCase("mode") && newLine.length == 3) {
            final IRCChannelInfo channel = getChannel(newLine[1]);
            if (channel != null) {
                // This makes sure we don't add the same item to the LMQ twice,
                // even if its requested twice, as the ircd will only reply once
                final Deque<Character> foundModes = new LinkedList<>();
                final Queue<Character> listModeQueue = channel.getListModeQueue();
                for (int i = 0; i < newLine[2].length(); ++i) {
                    final Character mode = newLine[2].charAt(i);
                    callDebugInfo(DEBUG_LMQ, "Intercepted mode request for " + channel + " for mode " + mode);
                    if (chanModesOther.containsKey(mode) && chanModesOther.get(mode) == MODE_LIST) {
                        if (foundModes.contains(mode)) {
                            callDebugInfo(DEBUG_LMQ, "Already added to LMQ");
                        } else {
                            listModeQueue.offer(mode);
                            foundModes.offer(mode);
                            callDebugInfo(DEBUG_LMQ, "Added to LMQ");
                        }
                    }
                }
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getNetworkName() {
        return networkName;
    }

    /** {@inheritDoc} */
    @Override
    public String getLastLine() {
        return lastLine == null ? "" : lastLine.getLine();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getServerInformationLines() {
        synchronized (serverInformationLines) {
            return new LinkedList<>(serverInformationLines);
        }
    }

    /**
     * Process a line and call relevant methods for handling.
     *
     * @param line Line read from the IRC server
     */
    @SuppressWarnings("fallthrough")
    protected void processLine(final ReadLine line) {
        callDataIn(line.getLine());
        String[] token = line.getTokens();
        Date lineTS = new Date();
        if (!token[0].isEmpty() && token[0].charAt(0) == '@') {
            try {
                final int tsEnd = token[0].indexOf('@', 1);
                final long ts = Long.parseLong(token[0].substring(1, tsEnd));
                token[0] = token[0].substring(tsEnd + 1);
                lineTS = new Date(ts - tsdiff);
            } catch (final NumberFormatException nfe) { /* Do nothing. */ }
        }

        int nParam;
        setPingNeeded(false);

        if (token.length < 2) {
            return;
        }

        try {
            final String sParam = token[1];
            if (token[0].equalsIgnoreCase("PING") || token[1].equalsIgnoreCase("PING")) {
                sendString("PONG :" + sParam, QueuePriority.HIGH);
            } else if (token[0].equalsIgnoreCase("PONG") || token[1].equalsIgnoreCase("PONG")) {
                if (!lastPingValue.isEmpty() && lastPingValue.equals(token[token.length - 1])) {
                    lastPingValue = "";
                    serverLag = System.currentTimeMillis() - pingTime;
                    callPingSuccess();
                }
            } else if (token[0].equalsIgnoreCase("ERROR")) {
                final StringBuilder errorMessage = new StringBuilder();
                for (int i = 1; i < token.length; ++i) {
                    errorMessage.append(token[i]);
                }
                callServerError(errorMessage.toString());
            } else if (token[1].equalsIgnoreCase("TSIRC") && token.length > 3) {
                if (token[2].equals("1")) {
                    try {
                        final long ts = Long.parseLong(token[3]);
                        tsdiff = ts - System.currentTimeMillis();
                    } catch (final NumberFormatException nfe) { /* Do nothing. */ }
                }
            } else {
                if (got001) {
                    // Freenode sends a random notice in a stupid place, others might do aswell
                    // These shouldn't cause post005 to be fired, so handle them here.
                    if (token[0].equalsIgnoreCase("NOTICE") || (token.length > 2 && token[2].equalsIgnoreCase("NOTICE"))) {
                        try {
                            myProcessingManager.process(lineTS, "Notice Auth", token);
                        } catch (ProcessorNotFoundException e) {
                            // ???
                        }

                        return;
                    }

                    if (!post005) {
                        try {
                            nParam = Integer.parseInt(token[1]);
                        } catch (NumberFormatException e) {
                            nParam = -1;
                        }

                        if (nParam < 0 || nParam > 5) {
                            callPost005();
                        } else {
                            // Store 001 - 005 for informational purposes.
                            synchronized (serverInformationLines) {
                                serverInformationLines.add(line.getLine());
                            }
                        }
                    }
                    // After 001 we potentially care about everything!
                    try {
                        myProcessingManager.process(lineTS, sParam, token);
                    } catch (ProcessorNotFoundException e) {
                        // ???
                    }
                } else {
                    // Before 001 we don't care about much.
                    try {
                        nParam = Integer.parseInt(token[1]);
                    } catch (NumberFormatException e) {
                        nParam = -1;
                    }
                    switch (nParam) {
                        case 1: // 001 - Welcome to IRC
                            synchronized (serverInformationLines) {
                                serverInformationLines.add(line.getLine());
                            }
                            // Fallthrough
                        case 464: // Password Required
                        case 433: // Nick In Use
                            try {
                                myProcessingManager.process(sParam, token);
                            } catch (ProcessorNotFoundException e) {
                            }
                            break;
                        default: // Unknown - Send to Notice Auth
                            // Some networks send a CTCP during the auth process, handle it
                            if (token.length > 3 && !token[3].isEmpty() && token[3].charAt(0) == (char) 1 && token[3].charAt(token[3].length() - 1) == (char) 1) {
                                try {
                                    myProcessingManager.process(lineTS, sParam, token);
                                } catch (ProcessorNotFoundException e) {
                                }
                                break;
                            }
                            // Some networks may send a NICK message if you nick change before 001
                            // Eat it up so that it isn't treated as a notice auth.
                            if (token[1].equalsIgnoreCase("NICK")) {
                                break;
                            }

                            // CAP also happens here, so try that.
                            if (token[1].equalsIgnoreCase("CAP")) {
                                myProcessingManager.process(lineTS, sParam, token);
                                break;
                            }

                            // Otherwise, send to Notice Auth
                            try {
                                myProcessingManager.process(lineTS, "Notice Auth", token);
                            } catch (ProcessorNotFoundException e) {
                            }
                            break;
                    }
                }
            }
        } catch (Exception e) {
            final ParserError ei = new ParserError(ParserError.ERROR_FATAL, "Fatal Exception in Parser.", getLastLine());
            ei.setException(e);
            callErrorInfo(ei);
        }
    }

    /** The IRCStringConverter for this parser */
    private IRCStringConverter stringConverter = null;

    /** {@inheritDoc} */
    @Override
    public IRCStringConverter getStringConverter() {
        if (stringConverter == null) {
            stringConverter = new IRCStringConverter(IRCEncoding.RFC1459);
        }
        return stringConverter;
    }

    /**
     * Sets the encoding that this parser's string converter should use.
     *
     * @param encoding The encoding to use
     */
    protected void setEncoding(final IRCEncoding encoding) {
        stringConverter = new IRCStringConverter(encoding);
    }

    /**
     * Check the state of the requested capability.
     *
     * @param capability The capability to check the state of.
     * @return State of the requested capability.
     */
    public CapabilityState getCapabilityState(final String capability) {
        synchronized (capabilities) {
            if (capabilities.containsKey(capability.toLowerCase())) {
                return capabilities.get(capability.toLowerCase());
            } else {
                return CapabilityState.INVALID;
            }
        }
    }

    /**
     * Set the state of the requested capability.
     *
     * @param capability Requested capability
     * @param state State to set for capability
     */
    public void setCapabilityState(final String capability, final CapabilityState state) {
        synchronized (capabilities) {
            if (capabilities.containsKey(capability.toLowerCase())) {
                capabilities.put(capability.toLowerCase(), state);
            }
        }
    }

    /**
     * Add the given capability as a supported capability by the server.
     *
     * @param capability Requested capability
     */
    public void addCapability(final String capability) {
        synchronized (capabilities) {
            capabilities.put(capability.toLowerCase(), CapabilityState.DISABLED);
        }
    }

    /**
     * Get the server capabilities and their current state.
     *
     * @return Server capabilities and their current state.
     */
    public Map<String, CapabilityState> getCapabilities() {
        synchronized (capabilities) {
            return new HashMap<>(capabilities);
        }
    }

    /**
     * Process CHANMODES from 005.
     */
    public void parseChanModes() {
        final StringBuilder sDefaultModes = new StringBuilder("b,k,l,");
        String[] bits;
        String modeStr;
        if (h005Info.containsKey("USERCHANMODES")) {
            if (getServerType() == ServerType.DANCER) {
                sDefaultModes.insert(0, "dqeI");
            } else if (getServerType() == ServerType.AUSTIRC) {
                sDefaultModes.insert(0, "e");
            }
            modeStr = h005Info.get("USERCHANMODES");
            char mode;
            for (int i = 0; i < modeStr.length(); ++i) {
                mode = modeStr.charAt(i);
                if (!prefixModes.containsKey(mode) && sDefaultModes.indexOf(Character.toString(mode)) < 0) {
                    sDefaultModes.append(mode);
                }
            }
        } else {
            sDefaultModes.append("imnpstrc");
        }
        if (h005Info.containsKey("CHANMODES")) {
            modeStr = h005Info.get("CHANMODES");
        } else {
            modeStr = sDefaultModes.toString();
            h005Info.put("CHANMODES", modeStr);
        }
        bits = modeStr.split(",", 5);
        if (bits.length < 4) {
            modeStr = sDefaultModes.toString();
            callErrorInfo(new ParserError(ParserError.ERROR_ERROR, "CHANMODES String not valid. Using default string of \"" + modeStr + "\"", getLastLine()));
            h005Info.put("CHANMODES", modeStr);
            bits = modeStr.split(",", 5);
        }

        // resetState
        chanModesOther.clear();
        chanModesBool.clear();
        nextKeyCMBool = 1;

        // List modes.
        for (int i = 0; i < bits[0].length(); ++i) {
            final Character cMode = bits[0].charAt(i);
            callDebugInfo(DEBUG_INFO, "Found List Mode: %c", cMode);
            if (!chanModesOther.containsKey(cMode)) {
                chanModesOther.put(cMode, MODE_LIST);
            }
        }

        // Param for Set and Unset.
        final Byte nBoth = MODE_SET + MODE_UNSET;
        for (int i = 0; i < bits[1].length(); ++i) {
            final Character cMode = bits[1].charAt(i);
            callDebugInfo(DEBUG_INFO, "Found Set/Unset Mode: %c", cMode);
            if (!chanModesOther.containsKey(cMode)) {
                chanModesOther.put(cMode, nBoth);
            }
        }

        // Param just for Set
        for (int i = 0; i < bits[2].length(); ++i) {
            final Character cMode = bits[2].charAt(i);
            callDebugInfo(DEBUG_INFO, "Found Set Only Mode: %c", cMode);
            if (!chanModesOther.containsKey(cMode)) {
                chanModesOther.put(cMode, MODE_SET);
            }
        }

        // Boolean Mode
        for (int i = 0; i < bits[3].length(); ++i) {
            final Character cMode = bits[3].charAt(i);
            callDebugInfo(DEBUG_INFO, "Found Boolean Mode: %c [%d]", cMode, nextKeyCMBool);
            if (!chanModesBool.containsKey(cMode)) {
                chanModesBool.put(cMode, nextKeyCMBool);
                nextKeyCMBool *= 2;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getChannelUserModes() {
        if (h005Info.containsKey("PREFIXSTRING")) {
            return h005Info.get("PREFIXSTRING");
        } else {
            return "";
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getBooleanChannelModes() {
        final char[] modes = new char[chanModesBool.size()];
        int i = 0;
        for (char mode : chanModesBool.keySet()) {
            modes[i] = mode;
            i++;
        }
        // Alphabetically sort the array
        Arrays.sort(modes);
        return new String(modes);
    }

    /** {@inheritDoc} */
    @Override
    public String getListChannelModes() {
        return getOtherModeString(MODE_LIST);
    }

    /** {@inheritDoc} */
    @Override
    public String getParameterChannelModes() {
        return getOtherModeString(MODE_SET);
    }

    /** {@inheritDoc} */
    @Override
    public String getDoubleParameterChannelModes() {
        return getOtherModeString((byte) (MODE_SET + MODE_UNSET));
    }

    /** {@inheritDoc} */
    @Override
    public String getChannelPrefixes() {
        return chanPrefix;
    }

    /**
     * Get modes from hChanModesOther that have a specific value.
     * Modes are returned in alphabetical order
     *
     * @param value Value mode must have to be included
     * @return All the currently known Set-Unset modes
     */
    protected String getOtherModeString(final byte value) {
        final char[] modes = new char[chanModesOther.size()];
        Byte nTemp;
        int i = 0;
        for (char cTemp : chanModesOther.keySet()) {
            nTemp = chanModesOther.get(cTemp);
            if (nTemp == value) {
                modes[i] = cTemp;
                i++;
            }
        }
        // Alphabetically sort the array
        Arrays.sort(modes);
        return new String(modes).trim();
    }

    /** {@inheritDoc} */
    @Override
    public String getUserModes() {
        if (h005Info.containsKey("USERMODES")) {
            return h005Info.get("USERMODES");
        } else {
            return "";
        }
    }

    /**
     * Process USERMODES from 004.
     */
    protected void parseUserModes() {
        final String sDefaultModes = "nwdoi";
        String modeStr;
        if (h005Info.containsKey("USERMODES")) {
            modeStr = h005Info.get("USERMODES");
        } else {
            modeStr = sDefaultModes;
            h005Info.put("USERMODES", sDefaultModes);
        }

        // resetState
        userModes.clear();
        nNextKeyUser = 1;

        // Boolean Mode
        for (int i = 0; i < modeStr.length(); ++i) {
            final Character cMode = modeStr.charAt(i);
            callDebugInfo(DEBUG_INFO, "Found User Mode: %c [%d]", cMode, nNextKeyUser);
            if (!userModes.containsKey(cMode)) {
                userModes.put(cMode, nNextKeyUser);
                nNextKeyUser *= 2;
            }
        }
    }

    /**
     * Resets the channel prefix property to the default, RFC specified value.
     */
    protected void resetChanPrefix() {
        chanPrefix = DEFAULT_CHAN_PREFIX;
    }

    /**
     * Sets the set of possible channel prefixes to those in the given value.
     *
     * @param value The new set of channel prefixes.
     */
    protected void setChanPrefix(final String value) {
        chanPrefix = value;
    }

    /**
     * Process PREFIX from 005.
     */
    public void parsePrefixModes() {
        final String sDefaultModes = "(ohv)@%+";
        String[] bits;
        String modeStr;
        if (h005Info.containsKey("PREFIX")) {
            modeStr = h005Info.get("PREFIX");
        } else {
            modeStr = sDefaultModes;
        }
        if (modeStr.substring(0, 1).equals("(")) {
            modeStr = modeStr.substring(1);
        } else {
            modeStr = sDefaultModes.substring(1);
            h005Info.put("PREFIX", sDefaultModes);
        }

        bits = modeStr.split("\\)", 2);
        if (bits.length != 2 || bits[0].length() != bits[1].length()) {
            modeStr = sDefaultModes;
            callErrorInfo(new ParserError(ParserError.ERROR_ERROR, "PREFIX String not valid. Using default string of \"" + modeStr + "\"", getLastLine()));
            h005Info.put("PREFIX", modeStr);
            modeStr = modeStr.substring(1);
            bits = modeStr.split("\\)", 2);
        }

        // resetState
        prefixModes.clear();
        prefixMap.clear();
        nextKeyPrefix = 1;

        for (int i = bits[0].length() - 1; i > -1; --i) {
            final Character cMode = bits[0].charAt(i);
            final Character cPrefix = bits[1].charAt(i);
            callDebugInfo(DEBUG_INFO, "Found Prefix Mode: %c => %c [%d]", cMode, cPrefix, nextKeyPrefix);
            if (!prefixModes.containsKey(cMode)) {
                prefixModes.put(cMode, nextKeyPrefix);
                prefixMap.put(cMode, cPrefix);
                prefixMap.put(cPrefix, cMode);
                nextKeyPrefix *= 2;
            }
        }

        h005Info.put("PREFIXSTRING", bits[0]);
    }

    /** {@inheritDoc} */
    @Override
    public void joinChannels(final ChannelJoinRequest... channels) {
        // We store a map from key->channels to allow intelligent joining of
        // channels using as few JOIN commands as needed.
        final Map<String, StringBuffer> joinMap = new HashMap<>();

        for (ChannelJoinRequest channel : channels) {
            // Make sure we have a list to put stuff in.
            StringBuffer list = joinMap.get(channel.getPassword());
            if (list == null) {
                list = new StringBuffer();
                joinMap.put(channel.getPassword(), list);
            }

            // Add the channel to the list. If the name is invalid and
            // autoprefix is off we will just skip this channel.
            if (!channel.getName().isEmpty()) {
                if (list.length() > 0) {
                    list.append(',');
                }
                if (!isValidChannelName(channel.getName())) {
                    if (chanPrefix.isEmpty()) {
                        // TODO: This is wrong - empty chan prefix means the
                        // IRCd supports no channels.
                        list.append('#');
                    } else {
                        list.append(chanPrefix.charAt(0));
                    }
                }
                list.append(channel.getName());
            }
        }

        for (Map.Entry<String, StringBuffer> entrySet : joinMap.entrySet()) {
            final String thisKey = entrySet.getKey();
            final String channelString = entrySet.getValue().toString();
            if (!channelString.isEmpty()) {
                if (thisKey == null || thisKey.isEmpty()) {
                    sendString("JOIN " + channelString);
                } else {
                    sendString("JOIN " + channelString + " " + thisKey);
                }
            }
        }
    }

    /**
     * Leave a Channel.
     *
     * @param channel Name of channel to part
     * @param reason Reason for leaving (Nothing sent if sReason is "")
     */
    public void partChannel(final String channel, final String reason) {
        if (getChannel(channel) == null) {
            return;
        }

        sendString("PART " + channel, reason);
    }

    /**
     * Set Nickname.
     *
     * @param nickname New nickname wanted.
     */
    public void setNickname(final String nickname) {
        if (getSocketState() == SocketState.OPEN) {
            if (!myself.isFake() && myself.getRealNickname().equals(nickname)) {
                return;
            }
            sendString("NICK " + nickname);
        } else {
            me.setNickname(nickname);
        }

        thinkNickname = nickname;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxLength(final String type, final String target) {
        // If my host is "nick!user@host" and we are sending "#Channel"
        // a "PRIVMSG" this will find the length of ":nick!user@host PRIVMSG #channel :"
        // and subtract it from the MAX_LINELENGTH. This should be sufficient in most cases.
        // Lint = the 2 ":" at the start and end and the 3 separating " "s
        int length = 0;
        if (type != null) {
            length += type.length();
        }
        if (target != null) {
            length += target.length();
        }
        return getMaxLength(length);
    }

    /**
     * Get the max length a message can be.
     *
     * @param length Length of stuff. (Ie "PRIVMSG"+"#Channel")
     * @return Max Length message should be.
     */
    public int getMaxLength(final int length) {
        final int lineLint = 5;
        if (myself.isFake()) {
            callErrorInfo(new ParserError(ParserError.ERROR_ERROR + ParserError.ERROR_USER, "getMaxLength() called, but I don't know who I am?", getLastLine()));
            return MAX_LINELENGTH - length - lineLint;
        } else {
            return MAX_LINELENGTH - myself.toString().length() - length - lineLint;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxListModes(final char mode) {
        // MAXLIST=bdeI:50
        // MAXLIST=b:60,e:60,I:60
        // MAXBANS=30
        int result = -2;
        callDebugInfo(DEBUG_INFO, "Looking for maxlistmodes for: " + mode);
        // Try in MAXLIST
        if (h005Info.get("MAXLIST") != null) {
            if (h005Info.get("MAXBANS") == null) {
                result = 0;
            }
            final String maxlist = h005Info.get("MAXLIST");
            callDebugInfo(DEBUG_INFO, "Found maxlist (" + maxlist + ")");
            final String[] bits = maxlist.split(",");
            for (String bit : bits) {
                final String[] parts = bit.split(":", 2);
                callDebugInfo(DEBUG_INFO, "Bit: " + bit + " | parts.length = " + parts.length + " (" + parts[0] + " -> " + parts[0].indexOf(mode) + ")");
                if (parts.length == 2 && parts[0].indexOf(mode) > -1) {
                    callDebugInfo(DEBUG_INFO, "parts[0] = '" + parts[0] + "' | parts[1] = '" + parts[1] + "'");
                    try {
                        result = Integer.parseInt(parts[1]);
                        break;
                    } catch (NumberFormatException nfe) {
                        result = -1;
                    }
                }
            }
        }

        // If not in max list, try MAXBANS
        if (result == -2 && h005Info.get("MAXBANS") != null) {
            callDebugInfo(DEBUG_INFO, "Trying max bans");
            try {
                result = Integer.parseInt(h005Info.get("MAXBANS"));
            } catch (NumberFormatException nfe) {
                result = -1;
            }
        } else if (result == -2 && getServerType() == ServerType.WEIRCD) {
            result = 50;
        } else if (result == -2 && getServerType() == ServerType.OTHERNET) {
            result = 30;
        } else if (result == -2) {
            result = -1;
            callDebugInfo(DEBUG_INFO, "Failed");
            callErrorInfo(new ParserError(ParserError.ERROR_ERROR + ParserError.ERROR_USER, "Unable to discover max list modes.", getLastLine()));
        }
        callDebugInfo(DEBUG_INFO, "Result: " + result);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void sendMessage(final String target, final String message) {
        if (target == null || message == null) {
            return;
        }
        if (target.isEmpty()) {
            return;
        }

        sendString("PRIVMSG " + target, message);
    }

    /** {@inheritDoc} */
    @Override
    public void sendNotice(final String target, final String message) {
        if (target == null || message == null) {
            return;
        }
        if (target.isEmpty()) {
            return;
        }

        sendString("NOTICE " + target, message);
    }

    /** {@inheritDoc} */
    @Override
    public void sendAction(final String target, final String message) {
        sendCTCP(target, "ACTION", message);
    }

    /** {@inheritDoc} */
    @Override
    public void sendCTCP(final String target, final String type, final String message) {
        if (target == null || message == null) {
            return;
        }
        if (target.isEmpty() || type.isEmpty()) {
            return;
        }
        final char char1 = (char) 1;
        sendString("PRIVMSG " + target, char1 + type.toUpperCase() + " " + message + char1);
    }

    /** {@inheritDoc} */
    @Override
    public void sendCTCPReply(final String target, final String type, final String message) {
        if (target == null || message == null) {
            return;
        }
        if (target.isEmpty() || type.isEmpty()) {
            return;
        }
        final char char1 = (char) 1;
        sendString("NOTICE " + target, char1 + type.toUpperCase() + " " + message + char1);
    }

    /** {@inheritDoc} */
    @Override
    public void requestGroupList(final String searchTerms) {
        sendString("LIST", searchTerms);
    }

    /** {@inheritDoc} */
    @Override
    public void quit(final String reason) {
        sendString("QUIT", reason);
    }

    /** {@inheritDoc} */
    @Override
    public void disconnect(final String message) {
        super.disconnect(message);
        if (currentSocketState == SocketState.OPENING || currentSocketState == SocketState.OPEN) {
            currentSocketState = SocketState.CLOSING;
            if (got001) {
                quit(message);
            }
        }

        try {
            // SSLSockets try to close nicely and read data from the socket,
            // which seems to hang indefinitely in some circumstances. We don't
            // like indefinite hangs, so just close the underlying socket
            // direct.
            if (rawSocket != null) {
                rawSocket.close();
            }
        } catch (IOException e) {
            /* Do Nothing */
        } finally {
            if (currentSocketState != SocketState.CLOSED) {
                currentSocketState = SocketState.CLOSED;
                callSocketClosed();
            }
            resetState();
        }
    }

    /** {@inheritDoc}
     *
     * - Before channel prefixes are known (005/noMOTD/MOTDEnd), this checks
     *   that the first character is either #, &amp;, ! or +
     * - Assumes that any channel that is already known is valid, even if
     *   005 disagrees.
     */
    @Override
    public boolean isValidChannelName(final String name) {
        // Check sChannelName is not empty or null
        if (name == null || name.isEmpty()) {
            return false;
        }
        // Check its not ourself (PM recieved before 005)
        if (getStringConverter().equalsIgnoreCase(getMyNickname(), name)) {
            return false;
        }
        // Check if we are already on this channel
        if (getChannel(name) != null) {
            return true;
        }
        // Otherwise return true if:
        // Channel equals "0"
        // first character of the channel name is a valid channel prefix.
        return chanPrefix.indexOf(name.charAt(0)) >= 0 || "0".equals(name);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUserSettable(final char mode) {
        String validmodes;
        if (h005Info.containsKey("USERCHANMODES")) {
            validmodes = h005Info.get("USERCHANMODES");
        } else {
            validmodes = "bklimnpstrc";
        }
        return validmodes.matches(".*" + mode + ".*");
    }

    /**
     * Get the 005 info.
     *
     * @return 005Info hashtable.
     */
    public Map<String, String> get005() {
        return Collections.unmodifiableMap(h005Info);
    }

    /**
     * Get the ServerType for this IRCD.
     *
     * @return The ServerType for this IRCD.
     */
    public ServerType getServerType() {
        return ServerType.findServerType(h005Info.get("004IRCD"), networkName, h005Info.get("003IRCD"), h005Info.get("002IRCD"));
    }

    /** {@inheritDoc} */
    @Override
    public String getServerSoftware() {
        final String version = h005Info.get("004IRCD");
        return version == null ? "" : version;
    }

    /** {@inheritDoc} */
    @Override
    public String getServerSoftwareType() {
        return getServerType().getType();
    }

    /**
     * Get the value of checkServerPing.
     *
     * @return value of checkServerPing.
     * @see #setCheckServerPing
     */
    public boolean getCheckServerPing() {
        return checkServerPing;
    }

    /**
     * Set the value of checkServerPing.
     *
     * @param newValue New value to use.
     * @see #setCheckServerPing
     */
    public void setCheckServerPing(final boolean newValue) {
        checkServerPing = newValue;
        if (checkServerPing) {
            startPingTimer();
        } else {
            stopPingTimer();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setEncoder(final Encoder encoder) {
        this.encoder = encoder;
    }

    /** {@inheritDoc} */
    @Override
    public void setPingTimerInterval(final long newValue) {
        super.setPingTimerInterval(newValue);

        startPingTimer();
    }

    /**
     * Start the pingTimer.
     */
    protected void startPingTimer() {
        pingTimerSem.acquireUninterruptibly();

        try {
            setPingNeeded(false);

            if (pingTimer != null) {
                pingTimer.cancel();
            }

            pingTimer = new Timer("IRCParser pingTimer");
            pingTimer.schedule(new PingTimer(this, pingTimer), 0, getPingTimerInterval());
            pingCountDown = 1;
        } finally {
            pingTimerSem.release();
        }
    }

    /**
     * Stop the pingTimer.
     */
    protected void stopPingTimer() {
        pingTimerSem.acquireUninterruptibly();
        if (pingTimer != null) {
            pingTimer.cancel();
            pingTimer = null;
        }
        pingTimerSem.release();
    }

    /**
     * This is called when the ping Timer has been executed.
     * As the timer is restarted on every incomming message, this will only be
     * called when there has been no incomming line for 10 seconds.
     *
     * @param timer The timer that called this.
     */
    protected void pingTimerTask(final Timer timer) {
        // If user no longer wants server ping to be checked, or the socket is
        // closed then cancel the time and do nothing else.
        if (!getCheckServerPing() || getSocketState() != SocketState.OPEN) {
            pingTimerSem.acquireUninterruptibly();
            if (pingTimer != null && pingTimer.equals(timer)) {
                pingTimer.cancel();
            }
            pingTimerSem.release();

            return;
        }
        if (getPingNeeded()) {
            if (!callPingFailed()) {
                pingTimerSem.acquireUninterruptibly();

                if (pingTimer != null && pingTimer.equals(timer)) {
                    pingTimer.cancel();
                }
                pingTimerSem.release();

                disconnect("Server not responding.");
            }
        } else {
            --pingCountDown;
            if (pingCountDown < 1) {
                pingTime = System.currentTimeMillis();
                setPingNeeded(true);
                pingCountDown = getPingTimerFraction();
                lastPingValue = String.valueOf(System.currentTimeMillis());
                if (sendString("PING " + lastPingValue, QueuePriority.HIGH)) {
                    callPingSent();
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getServerLatency() {
        return serverLag;
    }

    /**
     * Updates the name of the server that this parser is connected to.
     *
     * @param serverName The discovered server name
     */
    protected void updateServerName(final String serverName) {
        setServerName(serverName);
    }

    /**
     * Get the current server lag.
     *
     * @param actualTime if True the value returned will be the actual time the ping was sent
     *                   else it will be the amount of time sinse the last ping was sent.
     * @return Time last ping was sent
     */
    public long getPingTime(final boolean actualTime) {
        if (actualTime) {
            return pingTime;
        } else {
            return System.currentTimeMillis() - pingTime;
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getPingTime() {
        return getPingTime(false);
    }

    /**
     * Set if a ping is needed or not.
     *
     * @param newStatus new value to set pingNeeded to.
     */
    private void setPingNeeded(final boolean newStatus) {
        pingNeeded.set(newStatus);
    }

    /**
     * Get if a ping is needed or not.
     *
     * @return value of pingNeeded.
     */
    private boolean getPingNeeded() {
        return pingNeeded.get();
    }

    /** {@inheritDoc} */
    @Override
    public IRCClientInfo getLocalClient() {
        return myself;
    }

    /**
     * Get the current nickname.
     * After 001 this returns the exact same as getLocalClient().getRealNickname();
     * Before 001 it returns the nickname that the parser Thinks it has.
     *
     * @return Current nickname.
     */
    public String getMyNickname() {
        if (myself.isFake()) {
            return thinkNickname;
        } else {
            return myself.getRealNickname();
        }
    }

    /**
     * Retrieves the local user information that this parser was configured
     * with.
     *
     * @return This parser's local user configuration
     */
    public MyInfo getMyInfo() {
        return me;
    }

    /**
     * Get the current username (Specified in MyInfo on construction).
     * Get the username given in MyInfo
     *
     * @return My username.
     */
    public String getMyUsername() {
        return me.getUsername();
    }

    /**
     * Add a client to the ClientList.
     *
     * @param client Client to add
     */
    public void addClient(final IRCClientInfo client) {
        clientList.put(getStringConverter().toLowerCase(client.getRealNickname()), client);
    }

    /**
     * Remove a client from the ClientList.
     * This WILL NOT allow cMyself to be removed from the list.
     *
     * @param client Client to remove
     */
    public void removeClient(final IRCClientInfo client) {
        if (client != myself) {
            forceRemoveClient(client);
        }
    }

    /**
     * Remove a client from the ClientList.
     * This WILL allow cMyself to be removed from the list
     *
     * @param client Client to remove
     */
    protected void forceRemoveClient(final IRCClientInfo client) {
        clientList.remove(getStringConverter().toLowerCase(client.getRealNickname()));
    }

    /**
     * Get the number of known clients.
     *
     * @return Count of known clients
     */
    public int knownClients() {
        return clientList.size();
    }

    /**
     * Get the known clients as a collection.
     *
     * @return Known clients as a collection
     */
    public Collection<IRCClientInfo> getClients() {
        return clientList.values();
    }

    /**
     * Clear the client list.
     */
    public void clearClients() {
        clientList.clear();
        addClient(getLocalClient());
    }

    /**
     * Add a channel to the ChannelList.
     *
     * @param channel Channel to add
     */
    public void addChannel(final IRCChannelInfo channel) {
        synchronized (channelList) {
            channelList.put(getStringConverter().toLowerCase(channel.getName()), channel);
        }
    }

    /**
     * Remove a channel from the ChannelList.
     *
     * @param channel Channel to remove
     */
    public void removeChannel(final IRCChannelInfo channel) {
        synchronized (channelList) {
            channelList.remove(getStringConverter().toLowerCase(channel.getName()));
        }
    }

    /**
     * Get the number of known channel.
     *
     * @return Count of known channel
     */
    public int knownChannels() {
        synchronized (channelList) {
            return channelList.size();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Collection<IRCChannelInfo> getChannels() {
        synchronized (channelList) {
            return channelList.values();
        }
    }

    /**
     * Clear the channel list.
     */
    public void clearChannels() {
        synchronized (channelList) {
            channelList.clear();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String[] parseHostmask(final String hostmask) {
        return IRCClientInfo.parseHostFull(hostmask);
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxTopicLength() {
        if (h005Info.containsKey("TOPICLEN")) {
            try {
                return Integer.parseInt(h005Info.get("TOPICLEN"));
            } catch (NumberFormatException ex) {
                // Do nothing
            }
        }

        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxLength() {
        return MAX_LINELENGTH;
    }

    /** {@inheritDoc} */
    @Override
    public void setCompositionState(final String host, final CompositionState state) {
        // Do nothing
    }
}
