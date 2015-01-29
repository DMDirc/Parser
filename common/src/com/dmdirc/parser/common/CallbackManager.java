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

import com.dmdirc.parser.events.ParserErrorEvent;
import com.dmdirc.parser.events.ParserEvent;
import com.dmdirc.parser.interfaces.Parser;

import java.util.Date;

import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.bus.config.Feature;

/**
 * Parser Callback Manager.
 * Manages adding/removing/calling callbacks.
 */
public class CallbackManager extends MBassador<ParserEvent> {

    private final Object errorHandlerLock = new Object();
    private final Parser parser;

    public CallbackManager(final Parser parser) {
        super(new BusConfiguration().addFeature(Feature.SyncPubSub.Default())
                .addFeature(Feature.AsynchronousHandlerInvocation.Default(1, 1)).addFeature(
                        Feature.AsynchronousMessageDispatch.Default()
                                .setNumberOfMessageDispatchers(1)));
        this.parser = parser;
        setupErrorHandler();
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public CallbackManager(final BusConfiguration configuration, final Parser parser) {
        super(configuration);
        setupErrorHandler();
        this.parser = parser;
    }

    @Override
    public void publish(final ParserEvent message) {
        // TODO: HACKY REMOVE
        if (message.getParser() == null) {
            message.setParser(parser);
        }
        if (message.getDate() == null) {
            message.setDate(new Date());
        }
        // TODO: HACKY REMOVE
        super.publish(message);
    }

    @SuppressWarnings({
            "ThrowableResultOfMethodCallIgnored",
            "CallToPrintStackTrace",
            "UseOfSystemOutOrSystemErr"
    })
    private void setupErrorHandler() {
        addErrorHandler(e -> {
            if (Thread.holdsLock(errorHandlerLock)) {
                // ABORT ABORT ABORT - we're publishing an error on the same thread we just tried
                // to publish an error on. Something in the error reporting pipeline must be
                // breaking, so don't try adding any more errors.
                System.err.println("ERROR: Error when reporting error");
                e.getCause().printStackTrace();
                return;
            }

            synchronized (errorHandlerLock) {
                publish(new ParserErrorEvent(parser, new Date(), new Exception(
                        "Message: " + e.getCause().getCause().getMessage() + " in Handler: "
                                + e.getHandler(), e.getCause().getCause())));
            }
        });
    }
}
