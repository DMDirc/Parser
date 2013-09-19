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

package com.dmdirc.parser.common;

import com.dmdirc.parser.interfaces.Parser;
import com.dmdirc.parser.interfaces.SpecificCallback;
import com.dmdirc.parser.interfaces.callbacks.*; //NOPMD

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser Callback Manager.
 * Manages adding/removing/calling callbacks.
 */
public class CallbackManager {

    private static final Class[] CLASSES = {
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
            = new HashMap<Class<? extends CallbackInterface>, CallbackObject>();

    /** A map of implementations to use for parser interfaces. */
    private final Map<Class<?>, Class<?>> implementationMap;

    /**
     * Constructor to create a CallbackManager.
     *
     * @param parser Parser that owns this callback manager.
     * @param implementationMap A map of implementations to use
     */
    public CallbackManager(final Parser parser, final Map<Class<?>, Class<?>> implementationMap) {
        this.implementationMap = implementationMap;

        initialise(parser);
    }

    /**
     * Initialises this callback manager.
     *
     * @param parser The parser associated with this CallbackManager
     */
    protected void initialise(final Parser parser) {
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
        return new CallbackObject(parser, this, type.asSubclass(CallbackInterface.class), implementationMap);
    }

    /**
     * Retrieves a relevant {@link CallbackObjectSpecific} for the specified type.
     *
     * @param parser The parser that this manager belongs to
     * @param type The type of callback to create an object for
     * @return The relevant CallbackObject
     */
    protected CallbackObjectSpecific getSpecificCallbackObject(final Parser parser, final Class<?> type) {
        return new CallbackObjectSpecific(parser, this, type.asSubclass(CallbackInterface.class), implementationMap);
    }

    /**
     * Add new callback type.
     *
     * @param callback CallbackObject subclass for the callback.
     * @return if adding succeeded or not.
     */
    public boolean addCallbackType(final CallbackObject callback) {
        if (!callbackHash.containsKey(callback.getType())) {
            callbackHash.put(callback.getType(), callback);
            return true;
        }

        return false;
    }

    /**
     * Remove a callback type.
     *
     * @param callback CallbackObject subclass to remove.
     * @return if removal succeeded or not.
     */
    public boolean delCallbackType(final CallbackObject callback) {
        if (callbackHash.containsKey(callback.getType())) {
            callbackHash.remove(callback.getType());
            return true;
        }

        return false;
    }

    /**
     * Get reference to callback object.
     *
     * @param callback Name of type of callback object.
     * @return CallbackObject returns the callback object for this type
     */
    public CallbackObject getCallbackType(final Class<? extends CallbackInterface> callback) {
        if (!callbackHash.containsKey(callback)) {
            throw new CallbackNotFoundException("Callback not found: " + callback.getName()
                    + "\n\nMy class: " + getClass().getName()
                    + "\nContents: " + callbackHash.keySet()
                    + "\nThread: " + Thread.currentThread().getName());
        }

        return callbackHash.get(callback);
    }

    /**
     * Remove all callbacks associated with a specific object.
     *
     * @param o instance of ICallbackInterface to remove.
     */
    public void delAllCallback(final CallbackInterface o) {
        for (CallbackObject cb : callbackHash.values()) {
            if (cb != null && cb.getType().isInstance(o)) {
                cb.del(o);
            }
        }
    }

    /**
     * Add all callbacks that this object implements.
     *
     * @param o instance of ICallbackInterface to add.
     */
    public void addAllCallback(final CallbackInterface o) {
        for (CallbackObject cb : callbackHash.values()) {
            if (cb != null && cb.getType().isInstance(o)) {
                cb.add(o);
            }
        }
    }

    /**
     * Add a callback.
     * This method will throw a CallbackNotFoundException if the callback does not exist.
     *
     * @param <S> The type of callback
     * @param callback Type of callback object
     * @param o instance of ICallbackInterface to add.
     * @throws CallbackNotFoundException If callback is not found.
     * @throws NullPointerException If 'o' is null
     */
    public <S extends CallbackInterface> void addCallback(
            final Class<S> callback, final S o) throws CallbackNotFoundException {
        if (o == null) {
            throw new NullPointerException("CallbackInterface is null");
        }

        final CallbackObject cb = getCallbackType(callback);

        if (cb != null) {
            cb.add(o);
        }
    }

    /**
     * Add a callback with a specific target.
     * This method will throw a CallbackNotFoundException if the callback does not exist.
     *
     * @param <S> The type of callback
     * @param callback Type of callback object.
     * @param o instance of ICallbackInterface to add.
     * @param target Parameter to specify that a callback should only fire for specific things
     * @throws CallbackNotFoundException If callback is not found.
     * @throws NullPointerException If 'o' is null
     */
    public <S extends CallbackInterface> void addCallback(
            final Class<S> callback,
            final S o, final String target) throws CallbackNotFoundException {
        if (o == null) {
            throw new NullPointerException("CallbackInterface is null");
        }

        ((CallbackObjectSpecific) getCallbackType(callback)).add(o, target);
    }

    /**
     * Add a callback without an exception.
     * This should be used if a callback is not essential for execution (ie the DebugOut callback)
     *
     * @param <S> The type of callback object
     * @param callback Type of callback object.
     * @param o instance of ICallbackInterface to add.
     * @return true/false if the callback was added or not.
     */
    public <S extends CallbackInterface> boolean addNonCriticalCallback(
            final Class<S> callback, final S o) {
        try {
            addCallback(callback, o);
            return true;
        } catch (CallbackNotFoundException e) {
            return false;
        }
    }

    /**
     * Add a callback with a specific target.
     * This should be used if a callback is not essential for execution
     *
     * @param <S> The type of callback
     * @param callback Type of callback object.
     * @param o instance of ICallbackInterface to add.
     * @param target Parameter to specify that a callback should only fire for specific things
     * @return true/false if the callback was added or not.
     */
    public <S extends CallbackInterface> boolean addNonCriticalCallback(
            final Class<S> callback, final S o, final String target) {
        try {
            addCallback(callback, o, target);
            return true;
        } catch (CallbackNotFoundException e) {
            return false;
        }
    }

    /**
     * Remove a callback.
     *
     * @param callback Type of callback object.
     * @param o instance of ICallbackInterface to remove.
     */
    public void delCallback(final Class<? extends CallbackInterface> callback,
            final CallbackInterface o) {
        getCallbackType(callback).del(o);
    }

    /**
     * Gets a proxy object which can be used to despatch callbacks of the
     * specified type. Callers may pass <code>null</code> for the first two
     * arguments of any callback, and these will automatically be replaced
     * by the relevant Parser instance and the current date.
     *
     * @param <T> The type of the callback to retrieve
     * @param callback The callback to retrieve
     * @return A proxy object which can be used to call the specified callback
     */
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

        /** {@inheritDoc} */
        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) {
            final Object[] modifiedArgs = new Object[args.length - 2];
            System.arraycopy(args, 2, modifiedArgs, 0, args.length - 2);

            if (args[1] == null) {
                getCallbackType(callback).call(modifiedArgs);
            } else {
                getCallbackType(callback).call((Date) args[1], modifiedArgs);
            }

            return null;
        }

    }
}
