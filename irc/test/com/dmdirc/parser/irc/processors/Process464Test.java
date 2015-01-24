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

package com.dmdirc.parser.irc.processors;

import com.dmdirc.parser.common.CallbackManager;
import com.dmdirc.parser.events.PasswordRequiredEvent;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.ProcessingManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Process464Test {

    @Mock private IRCParser parser;
    @Mock private ProcessingManager processingManager;
    @Mock private CallbackManager callbackManager;
    @Captor private ArgumentCaptor<PasswordRequiredEvent> eventCaptor;
    private Process464 processor;

    @Before
    public void setup() {
        when(parser.getCallbackManager()).thenReturn(callbackManager);
        processor = new Process464(parser);
    }

    @Test
    public void testFiresPasswordListenerOn464() {
        processor.process("464", ":server.com", "464", ":Bad password!");
        verify(callbackManager).publish(eventCaptor.capture());
        assertSame(parser, eventCaptor.getValue().getParser());
    }

}