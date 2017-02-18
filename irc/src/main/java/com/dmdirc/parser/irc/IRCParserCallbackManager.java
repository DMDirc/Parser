/*
 * Copyright (c) 2006-2017 DMDirc Developers
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

import com.dmdirc.parser.common.CallbackManager;
import com.dmdirc.parser.events.ParserEvent;
import net.engio.mbassy.bus.IMessagePublication;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.bus.config.Feature;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;

import java.util.concurrent.TimeUnit;

/**
 * Parser Callback Manager.
 * Manages adding/removing/calling callbacks.
 *
 * Because IRCParser was designed for synchronous callbacks not async events, we enforce synchronous publish only
 * in this CallbackManager for now.
 *
 * This may change in future.
 */
public class IRCParserCallbackManager extends CallbackManager {
    public IRCParserCallbackManager(final IPublicationErrorHandler errorHandler) {
        super(new BusConfiguration().addFeature(Feature.SyncPubSub.Default())
                .addFeature(Feature.AsynchronousHandlerInvocation.Default(1, 1))
                .addFeature(Feature.AsynchronousMessageDispatch.Default()
                        .setNumberOfMessageDispatchers(0))
                .addPublicationErrorHandler(errorHandler));
    }

    @Override
    public IMessagePublication publishAsync(final ParserEvent message) {
        throw new UnsupportedOperationException("IRCParser does not support publishAsync");
    }

    @Override
    public IMessagePublication publishAsync(final ParserEvent message, final long timeout, final TimeUnit unit) {
        throw new UnsupportedOperationException("IRCParser does not support publishAsync");
    }
}
