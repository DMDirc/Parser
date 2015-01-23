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
import com.dmdirc.parser.interfaces.callbacks.CallbackInterface;

/**
 * Parser Callback Manager.
 * Manages adding/removing/calling callbacks.
 */
public interface CallbackManager {
    /**
     * Initialises this callback manager.
     *
     * @param parser The parser associated with this CallbackManager
     */
    void initialise(Parser parser);

    /**
     * Remove all callbacks associated with a specific object.
     *
     * @param o instance of ICallbackInterface to remove.
     */
    void delAllCallback(CallbackInterface o);

    /**
     * Add all callbacks that this object implements.
     *
     * @param o instance of ICallbackInterface to add.
     */
    void addAllCallback(CallbackInterface o);

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
    <S extends CallbackInterface> void addCallback(Class<S> callback, S o)
            throws CallbackNotFoundException;

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
    <S extends CallbackInterface> void addCallback(Class<S> callback, S o, String target)
            throws CallbackNotFoundException;

    /**
     * Remove a callback.
     *
     * @param callback Type of callback object.
     * @param o instance of ICallbackInterface to remove.
     */
    void delCallback(Class<? extends CallbackInterface> callback, CallbackInterface o);

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
    <T extends CallbackInterface> T getCallback(Class<T> callback);
}
