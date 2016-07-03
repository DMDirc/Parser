/*
 * Copyright (c) 2006-2016 DMDirc Developers
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

package com.dmdirc.parser.irc.integration;

import com.dmdirc.parser.events.ServerReadyEvent;
import com.dmdirc.parser.interfaces.Parser;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.integration.util.DockerContainerRule;

import java.net.URI;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;

import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;

public class ExampleTest {

    @Rule
    public final DockerContainerRule containerRule =
            new DockerContainerRule("cloudposse/unrealircd");

    @Test
    public void testBasicConnect() throws InterruptedException {
        final Parser ircParser = new IRCParser(URI.create(
                "irc://" + containerRule.getContainerIpAddress()));
        final ConnectListener listener = new ConnectListener();
        ircParser.getCallbackManager().subscribe(listener);
        ircParser.connect();
        listener.connected.tryAcquire(2500, TimeUnit.MILLISECONDS);
    }

    @Listener(references = References.Strong)
    private static final class ConnectListener {

        final Semaphore connected = new Semaphore(0);

        @Handler
        public void onConnect(final ServerReadyEvent event) {
            connected.release();
        }

    }


} 
