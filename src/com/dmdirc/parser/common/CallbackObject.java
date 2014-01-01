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
package com.dmdirc.parser.common;

import com.dmdirc.parser.interfaces.FakableArgument;
import com.dmdirc.parser.interfaces.FakableSource;
import com.dmdirc.parser.interfaces.Parser;
import com.dmdirc.parser.interfaces.callbacks.CallbackInterface;
import com.dmdirc.parser.interfaces.callbacks.ErrorInfoListener;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Superclass for all callback types.
 */
public class CallbackObject {

    /** The type of callback that this object is operating with. */
    protected final Class<? extends CallbackInterface> type;

    /** Arraylist for storing callback information related to the callback. */
    protected final List<CallbackInterface> callbackInfo
            = new CopyOnWriteArrayList<>();

    /** Reference to the Parser that owns this callback. */
    protected Parser myParser;

    /** Reference to the CallbackManager in charge of this callback. */
    protected CallbackManager myManager;

    /** A map of interfaces to their concrete implementations. */
    private final Map<Class<?>, Class<?>> implementationMap;

    /**
     * Create a new instance of the Callback Object.
     *
     * @param parser Parser That owns this callback
     * @param manager CallbackManager that is in charge of this callback
     * @param type The type of callback to use
     * @param implementationMap A map of interfaces to their parser-specific
     * implementations
     * @since 0.6.6
     */
    public CallbackObject(final Parser parser, final CallbackManager manager,
            final Class<? extends CallbackInterface> type,
            final Map<Class<?>, Class<?>> implementationMap) {
        this.myParser = parser;
        this.myManager = manager;
        this.type = type;
        this.implementationMap = implementationMap;
    }

    /**
     * Add a callback pointer to the appropriate ArrayList.
     *
     * @param eMethod OBject to callback to.
     */
    protected final void addCallback(final CallbackInterface eMethod) {
        if (!callbackInfo.contains(eMethod)) {
            callbackInfo.add(eMethod);
        }
    }

    /**
     * Delete a callback pointer from the appropriate ArrayList.
     *
     * @param eMethod Object that was being called back to.
     */
    protected final void delCallback(final CallbackInterface eMethod) {
        callbackInfo.remove(eMethod);
    }

    /**
     * Call the OnErrorInfo callback.
     *
     * @param errorInfo ParserError object to pass as error.
     * @return true if error call succeeded, false otherwise
     */
    protected final boolean callErrorInfo(final ParserError errorInfo) {
        return myManager.getCallbackType(ErrorInfoListener.class).call(errorInfo);
    }

    /**
     * Add a new callback.
     *
     * @param eMethod Object to callback to.
     */
    public void add(final CallbackInterface eMethod) {
        addCallback(eMethod);
    }

    /**
     * Remove a callback.
     *
     * @param eMethod Object to remove callback to.
     */
    public void del(final CallbackInterface eMethod) {
        delCallback(eMethod);
    }

    /**
     * Retrieves the type of callback that this callback object is for.
     *
     * @return This object's type
     * @since 0.6.3m2
     */
    public Class<? extends CallbackInterface> getType() {
        return type;
    }

    /**
     * Actually calls this callback. The specified arguments must match those
     * specified in the callback's interface, or an error will be raised.
     *
     * @param args The arguments to pass to the callback implementation
     * @return True if a method was called, false otherwise
     */
    public boolean call(final Object... args) {
        return call(new Date(), args);
    }

    /**
     * Actually calls this callback. The specified arguments must match those
     * specified in the callback's interface, or an error will be raised.
     *
     * @param date The date/time at which the event occured
     * @param args The arguments to pass to the callback implementation
     * @return True if a method was called, false otherwise
     */
    public boolean call(final Date date, final Object... args) {
        boolean bResult = false;

        final Object[] newArgs = new Object[args.length + 2];
        System.arraycopy(args, 0, newArgs, 2, args.length);
        newArgs[0] = myParser;
        newArgs[1] = date;

        createFakeArgs(newArgs);

        for (CallbackInterface iface : callbackInfo) {
            try {
                type.getMethods()[0].invoke(iface, newArgs);
            } catch (SecurityException | IllegalAccessException |
                    IllegalArgumentException | InvocationTargetException e) {
                if (getType().equals(ErrorInfoListener.class)) {
                    System.out.printf("Exception in onError Callback. [%s]\n", e.getMessage());
                    e.printStackTrace();
                } else {
                    final ParserError ei = new ParserError(ParserError.ERROR_ERROR,
                            "Exception in callback (" + e.getMessage() + ")",
                            myParser.getLastLine());
                    ei.setException(e);
                    callErrorInfo(ei);
                }
            }
            bResult = true;
        }

        return bResult;
    }

    /**
     * Replaces all null entries in the specified array with fake values, if the
     * corresponding parameter of this callback's type is marked with the
     * {@link FakableArgument} annotation. The fake classes are constructed by
     * using parameters designated {@link FakableSource}.
     *
     * @param args The arguments to be faked
     */
    protected void createFakeArgs(final Object[] args) {
        int i = 0;

        for (Annotation[] anns : type.getMethods()[0].getParameterAnnotations()) {
            for (Annotation ann : anns) {
                if (ann.annotationType().equals(FakableArgument.class) && args[i] == null) {
                    args[i] = getFakeArg(args, type.getMethods()[0].getParameterTypes()[i]);
                }
            }

            i++;
        }
    }

    /**
     * Tries to create fake argument of the specified target class, by using
     * {@link FakableSource} denoted parameters from the specified arg array.
     *
     * If an argument is missing, the method attempts to create a fake instance
     * by recursing into this method again. Note that this could cause an
     * infinite recursion in cases of cyclic dependencies. If recursion fails,
     * the constructor is skipped.
     *
     * If the created object has a <code>setFake(boolean)</code> method, it is
     * automatically invoked with an argument of <code>true</code>.
     *
     * @param args The arguments array to use for sources
     * @param target The class that should be constructed
     * @return An instance of the target class, or null on failure
     */
    protected Object getFakeArg(final Object[] args, final Class<?> target) {
        final Map<Class<?>, Object> sources = new HashMap<>();
        int i = 0;

        for (Annotation[] anns : type.getMethods()[0].getParameterAnnotations()) {
            for (Annotation ann : anns) {
                if (ann.annotationType().equals(FakableSource.class)) {
                    final Class<?> argType = type.getMethods()[0].getParameterTypes()[i];

                    sources.put(argType, args[i]);
                }
            }

            i++;
        }

        final Class<?> actualTarget = getImplementation(target);

        for (Constructor<?> ctor : actualTarget.getConstructors()) {
            Object[] params = new Object[ctor.getParameterTypes().length];

            i = 0;
            Object param;
            boolean failed = false;

            for (Class<?> needed : ctor.getParameterTypes()) {
                boolean found = false;

                for (Map.Entry<Class<?>, Object> entry : sources.entrySet()) {
                    if (entry.getKey().isAssignableFrom(needed)) {
                        params[i] = entry.getValue();
                        found = true;
                    }
                }

                if (!found) {
                    param = getFakeArg(args, needed);

                    if (param == null) {
                        failed = true;
                    } else {
                        params[i] = param;
                    }
                }

                i++;
            }

            if (!failed) {
                try {
                    final Object instance = ctor.newInstance(params);

                    for (Method method : actualTarget.getMethods()) {
                        if (method.getName().equals("setFake")
                                && method.getParameterTypes().length == 1
                                && method.getParameterTypes()[0].equals(Boolean.TYPE)) {
                            method.invoke(instance, true);
                        }
                    }

                    return instance;
                } catch (InstantiationException | IllegalAccessException |
                        IllegalArgumentException | InvocationTargetException ex) {
                    // Do nothing
                }
            }
        }

        return null;
    }

    /**
     * Returns the concrete class which should be instantiated if this callback
     * object desires an instance of the specified type. Generally, this will
     * translate the common interfaces to specific parser-dependent
     * implementations.
     *
     * @param type The type to be instantiated
     * @return A concrete implementation of that type
     */
    protected Class<?> getImplementation(final Class<?> type) {
        return implementationMap.containsKey(type) ? implementationMap.get(type) : type;
    }

}
