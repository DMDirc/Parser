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

import com.dmdirc.harness.parser.TestParser;
import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.interfaces.callbacks.ErrorInfoListener;
import com.dmdirc.parser.irc.IRCEncoding;

import java.util.Date;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@Ignore
public class Process004005Test {

    private TestParser doCaseMappingTest(final String target, final IRCEncoding expected) {
        final TestParser parser = new TestParser();
        parser.injectConnectionStrings();
        parser.injectLine(":server 005 nick CASEMAPPING=" + target
                    + " :are supported by this server");

        assertEquals(expected, parser.getStringConverter().getEncoding());

        return parser;
    }

    @Test
    public void testCaseMappingASCII() {
        doCaseMappingTest("ascii", IRCEncoding.ASCII);
        doCaseMappingTest("ASCII", IRCEncoding.ASCII);
    }

    @Test
    public void testCaseMappingRFC() {
        doCaseMappingTest("rfc1459", IRCEncoding.RFC1459);
        doCaseMappingTest("RFC1459", IRCEncoding.RFC1459);
    }

    @Test
    public void testCaseMappingStrict() {
        doCaseMappingTest("strict-rfc1459", IRCEncoding.STRICT_RFC1459);
        doCaseMappingTest("strict-RFC1459", IRCEncoding.STRICT_RFC1459);
    }

    @Test
    public void testCaseMappingUnknown() {
        final TestParser tp = doCaseMappingTest("rfc1459", IRCEncoding.RFC1459);
        final ErrorInfoListener info = mock(ErrorInfoListener.class);

        tp.injectLine(":server 005 nick CASEMAPPING=unknown :are supported by this server");

        verify(info).onErrorInfo(same(tp), (Date) anyObject(), (ParserError) anyObject());
    }

}
