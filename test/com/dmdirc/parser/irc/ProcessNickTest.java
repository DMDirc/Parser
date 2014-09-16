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

package com.dmdirc.parser.irc;

import com.dmdirc.harness.parser.TestParser;
import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.interfaces.ClientInfo;
import com.dmdirc.parser.interfaces.Parser;
import com.dmdirc.parser.interfaces.callbacks.ErrorInfoListener;
import com.dmdirc.parser.interfaces.callbacks.NickChangeListener;
import com.dmdirc.parser.common.CallbackNotFoundException;
import java.util.Date;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ProcessNickTest {
    
    @Test
    public void testNickSameName() {
        final TestParser parser = new TestParser();
        final NickChangeListener tinc = mock(NickChangeListener.class);

        parser.getCallbackManager().addCallback(NickChangeListener.class, tinc);
        
        parser.injectConnectionStrings();
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":nick JOIN #DMDirc_testing2");
        parser.injectLine(":server 353 nick = #DMDirc_testing :@nick +luser @+nick2 nick3");
        parser.injectLine(":server 366 nick #DMDirc_testing :End of /NAMES list");
        parser.injectLine(":luser!lu@ser.com NICK LUSER");

        assertNotNull(parser.getClient("LUSER"));
        assertEquals(1, parser.getClient("LUSER").getChannelClients().size());

        final IRCChannelClientInfo cci = parser.getClient("LUSER").getChannelClients().get(0);
        assertEquals(parser.getChannel("#DMDirc_testing"), cci.getChannel());
        assertEquals("+", cci.getChanModeStr(true));

        verify(tinc).onNickChanged(same(parser), (Date) anyObject(),
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

        final IRCChannelClientInfo cci = parser.getClient("foobar").getChannelClients().get(0);
        assertEquals(parser.getChannel("#DMDirc_testing"), cci.getChannel());
        assertEquals("+", cci.getChanModeStr(true));
    }    
    
    @Test
    public void testOverrideNick() throws CallbackNotFoundException {
        final TestParser parser = new TestParser();
        final ErrorInfoListener info = mock(ErrorInfoListener.class);
        
        parser.getCallbackManager().addCallback(ErrorInfoListener.class, info);
        parser.injectConnectionStrings();
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":server 353 nick = #DMDirc_testing :@nick +luser @+nick2 nick3");
        parser.injectLine(":server 366 nick #DMDirc_testing :End of /NAMES list");
        parser.injectLine(":luser!lu@ser.com NICK nick3");

        verify(info).onErrorInfo(same(parser), (Date) anyObject(), (ParserError) anyObject());
    }
    
    @Test
    public void testUnknownNick() {
        final TestParser parser = new TestParser();
        final NickChangeListener tinc = mock(NickChangeListener.class);
        
        parser.getCallbackManager().addCallback(NickChangeListener.class, tinc);
        
        parser.injectConnectionStrings();
        parser.injectLine(":random!lu@ser NICK rand");

        verify(tinc, never()).onNickChanged((Parser) anyObject(),
                (Date) anyObject(), (ClientInfo) anyObject(), anyString());
    }

}
