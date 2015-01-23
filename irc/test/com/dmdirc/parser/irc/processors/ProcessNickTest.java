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
import com.dmdirc.parser.common.CallbackNotFoundException;
import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.callbacks.ErrorInfoListener;
import com.dmdirc.parser.interfaces.callbacks.NickChangeListener;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;

@Ignore
public class ProcessNickTest {
    
    @Test
    public void testNickSameName() {
        final TestParser parser = new TestParser();
        final NickChangeListener tinc = mock(NickChangeListener.class);
        
        parser.injectConnectionStrings();
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":nick JOIN #DMDirc_testing2");
        parser.injectLine(":server 353 nick = #DMDirc_testing :@nick +luser @+nick2 nick3");
        parser.injectLine(":server 366 nick #DMDirc_testing :End of /NAMES list");
        parser.injectLine(":luser!lu@ser.com NICK LUSER");

        assertNotNull(parser.getClient("LUSER"));
        assertEquals(1, parser.getClient("LUSER").getChannelClients().size());

        final ChannelClientInfo cci = parser.getClient("LUSER").getChannelClients().get(0);
        assertEquals(parser.getChannel("#DMDirc_testing"), cci.getChannel());
        assertEquals("+", cci.getAllModesPrefix());

        verify(tinc).onNickChanged(same(parser), anyObject(),
                same(parser.getClient("LUSER")), eq("luser"));
    }
    
    @Test
    public void testNickDifferent() {
        final TestParser parser = new TestParser();

        parser.injectConnectionStrings();
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":server 353 nick = #DMDirc_testing :@nick +luser @+nick2 nick3");
        parser.injectLine(":server 366 nick #DMDirc_testing :End of /NAMES list");
        parser.injectLine(":luser!lu@ser.com NICK foobar");

        assertNotNull(parser.getClient("foobar"));
        assertFalse(parser.isKnownClient("luser"));
        assertEquals(1, parser.getClient("foobar").getChannelClients().size());

        final ChannelClientInfo cci = parser.getClient("foobar").getChannelClients().get(0);
        assertEquals(parser.getChannel("#DMDirc_testing"), cci.getChannel());
        assertEquals("+", cci.getAllModesPrefix());
    }    
    
    @Test
    public void testOverrideNick() throws CallbackNotFoundException {
        final TestParser parser = new TestParser();
        final ErrorInfoListener info = mock(ErrorInfoListener.class);

        parser.injectConnectionStrings();
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":server 353 nick = #DMDirc_testing :@nick +luser @+nick2 nick3");
        parser.injectLine(":server 366 nick #DMDirc_testing :End of /NAMES list");
        parser.injectLine(":luser!lu@ser.com NICK nick3");

        verify(info).onErrorInfo(same(parser), anyObject(), anyObject());
    }
    
    @Test
    public void testUnknownNick() {
        final TestParser parser = new TestParser();
        final NickChangeListener tinc = mock(NickChangeListener.class);

        parser.injectConnectionStrings();
        parser.injectLine(":random!lu@ser NICK rand");

        verify(tinc, never()).onNickChanged(anyObject(), anyObject(), anyObject(), anyString());
    }

}
