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

import com.dmdirc.parser.interfaces.Parser;
import com.dmdirc.parser.interfaces.SpecificCallback;
import com.dmdirc.parser.interfaces.callbacks.AuthNoticeListener;
import com.dmdirc.parser.interfaces.callbacks.AwayStateListener;
import com.dmdirc.parser.interfaces.callbacks.CallbackInterface;
import com.dmdirc.parser.interfaces.callbacks.ChannelActionListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelCtcpListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelCtcpReplyListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelJoinListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelKickListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelListModeListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelMessageListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelModeChangeListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelModeMessageListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelModeNoticeListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelNamesListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelNickChangeListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelNonUserModeChangeListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelNoticeListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelOtherAwayStateListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelPartListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelQuitListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelSelfJoinListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelSingleModeChangeListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelTopicListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelUserModeChangeListener;
import com.dmdirc.parser.interfaces.callbacks.CompositionStateChangeListener;
import com.dmdirc.parser.interfaces.callbacks.ConnectErrorListener;
import com.dmdirc.parser.interfaces.callbacks.DataInListener;
import com.dmdirc.parser.interfaces.callbacks.DataOutListener;
import com.dmdirc.parser.interfaces.callbacks.DebugInfoListener;
import com.dmdirc.parser.interfaces.callbacks.ErrorInfoListener;
import com.dmdirc.parser.interfaces.callbacks.GroupListEndListener;
import com.dmdirc.parser.interfaces.callbacks.GroupListEntryListener;
import com.dmdirc.parser.interfaces.callbacks.GroupListStartListener;
import com.dmdirc.parser.interfaces.callbacks.InviteListener;
import com.dmdirc.parser.interfaces.callbacks.MotdEndListener;
import com.dmdirc.parser.interfaces.callbacks.MotdLineListener;
import com.dmdirc.parser.interfaces.callbacks.MotdStartListener;
import com.dmdirc.parser.interfaces.callbacks.NetworkDetectedListener;
import com.dmdirc.parser.interfaces.callbacks.NickChangeListener;
import com.dmdirc.parser.interfaces.callbacks.NickInUseListener;
import com.dmdirc.parser.interfaces.callbacks.NumericListener;
import com.dmdirc.parser.interfaces.callbacks.OtherAwayStateListener;
import com.dmdirc.parser.interfaces.callbacks.PasswordRequiredListener;
import com.dmdirc.parser.interfaces.callbacks.PingFailureListener;
import com.dmdirc.parser.interfaces.callbacks.PingSentListener;
import com.dmdirc.parser.interfaces.callbacks.PingSuccessListener;
import com.dmdirc.parser.interfaces.callbacks.PrivateActionListener;
import com.dmdirc.parser.interfaces.callbacks.PrivateCtcpListener;
import com.dmdirc.parser.interfaces.callbacks.PrivateCtcpReplyListener;
import com.dmdirc.parser.interfaces.callbacks.PrivateMessageListener;
import com.dmdirc.parser.interfaces.callbacks.PrivateNoticeListener;
import com.dmdirc.parser.interfaces.callbacks.QuitListener;
import com.dmdirc.parser.interfaces.callbacks.ServerErrorListener;
import com.dmdirc.parser.interfaces.callbacks.ServerNoticeListener;
import com.dmdirc.parser.interfaces.callbacks.ServerReadyListener;
import com.dmdirc.parser.interfaces.callbacks.SocketCloseListener;
import com.dmdirc.parser.interfaces.callbacks.UnknownActionListener;
import com.dmdirc.parser.interfaces.callbacks.UnknownCtcpListener;
import com.dmdirc.parser.interfaces.callbacks.UnknownCtcpReplyListener;
import com.dmdirc.parser.interfaces.callbacks.UnknownMessageListener;
import com.dmdirc.parser.interfaces.callbacks.UnknownNoticeListener;
import com.dmdirc.parser.interfaces.callbacks.UnknownServerNoticeListener;
import com.dmdirc.parser.interfaces.callbacks.UserModeChangeListener;
import com.dmdirc.parser.interfaces.callbacks.UserModeDiscoveryListener;
import com.dmdirc.parser.interfaces.callbacks.WallDesyncListener;
import com.dmdirc.parser.interfaces.callbacks.WallopListener;
import com.dmdirc.parser.interfaces.callbacks.WalluserListener;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser Callback Manager.
 * Manages adding/removing/calling callbacks.
 */
public class CallbackManagerImpl implements CallbackManager {

    private static final Class<?>[] CLASSES = {
        AwayStateListener.class,
        OtherAwayStateListener.class,
        ChannelOtherAwayStateListener.class,
        ChannelActionListener.class,
        ChannelCtcpListener.class,
        ChannelCtcpReplyListener.class,
        ChannelListModeListener.class,
        ChannelNamesListener.class,
        ChannelJoinListener.class,
        ChannelKickListener.class,
        ChannelMessageListener.class,
        ChannelModeChangeListener.class,
        ChannelNickChangeListener.class,
        ChannelNonUserModeChangeListener.class,
        ChannelModeMessageListener.class,
        ChannelModeNoticeListener.class,
        ChannelNoticeListener.class,
        ChannelPartListener.class,
        ChannelQuitListener.class,
        ChannelSelfJoinListener.class,
        ChannelSingleModeChangeListener.class,
        ChannelTopicListener.class,
        ChannelUserModeChangeListener.class,
        CompositionStateChangeListener.class,
        ConnectErrorListener.class,
        DataInListener.class,
        DataOutListener.class,
        DebugInfoListener.class,
        ErrorInfoListener.class,
        GroupListStartListener.class,
        GroupListEntryListener.class,
        GroupListEndListener.class,
        NetworkDetectedListener.class,
        InviteListener.class,
        MotdEndListener.class,
        MotdLineListener.class,
        MotdStartListener.class,
        NickChangeListener.class,
        NickInUseListener.class,
        AuthNoticeListener.class,
        NumericListener.class,
        PasswordRequiredListener.class,
        PingFailureListener.class,
        PingSuccessListener.class,
        PingSentListener.class,
        PrivateActionListener.class,
        PrivateCtcpListener.class,
        PrivateCtcpReplyListener.class,
        PrivateMessageListener.class,
        PrivateNoticeListener.class,
        QuitListener.class,
        ServerErrorListener.class,
        ServerReadyListener.class,
        SocketCloseListener.class,
        UnknownActionListener.class,
        UnknownCtcpListener.class,
        UnknownCtcpReplyListener.class,
        UnknownMessageListener.class,
        UnknownNoticeListener.class,
        UserModeChangeListener.class,
        UserModeDiscoveryListener.class,
        WallDesyncListener.class,
        WallopListener.class,
        WalluserListener.class,
        ServerNoticeListener.class,
        UnknownServerNoticeListener.class,
    };

    /** Hashtable used to store the different types of callback known. */
    private final Map<Class<? extends CallbackInterface>, CallbackObject> callbackHash
            = new HashMap<>();

    /** A map of implementations to use for parser interfaces. */
    private final Map<Class<?>, Class<?>> implementationMap;

    /**
     * Constructor to create a CallbackManager.
     *
     * @param implementationMap A map of implementations to use
     */
    public CallbackManagerImpl(final Map<Class<?>, Class<?>> implementationMap) {
        this.implementationMap = Collections.unmodifiableMap(implementationMap);
    }

    @Override
    public void initialise(final Parser parser) {
        for (Class<?> type : CLASSES) {
            if (type.isAnnotationPresent(SpecificCallback.class)) {
                addCallbackType(getSpecificCallbackObject(parser, type));
            } else {
                addCallbackType(getCallbackObject(parser, type));
            }
        }
    }

    /**
     * Retrieves a relevant {@link CallbackObject} for the specified type.
     *
     * @param parser The parser that this manager belongs to
     * @param type The type of callback to create an object for
     * @return The relevant CallbackObject
     */
    protected CallbackObject getCallbackObject(final Parser parser, final Class<?> type) {
        return new CallbackObject(parser, this, type.asSubclass(CallbackInterface.class),
                implementationMap);
    }

    /**
     * Retrieves a relevant {@link CallbackObjectSpecific} for the specified type.
     *
     * @param parser The parser that this manager belongs to
     * @param type The type of callback to create an object for
     * @return The relevant CallbackObject
     */
    protected CallbackObjectSpecific getSpecificCallbackObject(
            final Parser parser, final Class<?> type) {
        return new CallbackObjectSpecific(parser, this, type.asSubclass(CallbackInterface.class),
                implementationMap);
    }

    /**
     * Add new callback type.
     *
     * @param callback CallbackObject subclass for the callback.
     */
    private void addCallbackType(final CallbackObject callback) {
        if (!callbackHash.containsKey(callback.getType())) {
            callbackHash.put(callback.getType(), callback);
        }
    }

    /**
     * Get reference to callback object.
     *
     * @param callback Name of type of callback object.
     * @return CallbackObject returns the callback object for this type
     */
    private CallbackObject getCallbackType(final Class<? extends CallbackInterface> callback) {
        if (!callbackHash.containsKey(callback)) {
            throw new CallbackNotFoundException("Callback not found: " + callback.getName());
        }

        return callbackHash.get(callback);
    }

    @Override
    public void delAllCallback(final CallbackInterface o) {
        callbackHash.values().stream()
                .filter(cb -> cb != null && cb.getType().isInstance(o))
                .forEach(cb -> cb.del(o));
    }

    @Override
    public void addAllCallback(final CallbackInterface o) {
        callbackHash.values().stream()
                .filter(cb -> cb != null && cb.getType().isInstance(o))
                .forEach(cb -> cb.add(o));
    }

    @Override
    public <S extends CallbackInterface> void addCallback(final Class<S> callback, final S o) throws CallbackNotFoundException {
        if (o == null) {
            throw new NullPointerException("CallbackInterface is null");
        }

        final CallbackObject cb = getCallbackType(callback);

        if (cb != null) {
            cb.add(o);
        }
    }

    @Override
    public <S extends CallbackInterface> void addCallback(final Class<S> callback, final S o,
            final String target) throws CallbackNotFoundException {
        if (o == null) {
            throw new NullPointerException("CallbackInterface is null");
        }

        ((CallbackObjectSpecific) getCallbackType(callback)).add(o, target);
    }

    @Override
    public void delCallback(final Class<? extends CallbackInterface> callback,
            final CallbackInterface o) {
        getCallbackType(callback).del(o);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends CallbackInterface> T getCallback(final Class<T> callback) {
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{ callback }, new CallbackHandler(callback));
    }

    /**
     * A Proxy invocation handler for a specified parser callback.
     */
    private class CallbackHandler implements InvocationHandler {

        /** The callback that should be called. */
        private final Class<? extends CallbackInterface> callback;

        /**
         * Creates a new callback handler for the specified callback.
         *
         * @param callback The callback to handle
         */
        public CallbackHandler(final Class<? extends CallbackInterface> callback) {
            this.callback = callback;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) {
            getCallbackType(callback).call(args);
            return null;
        }

    }
}
