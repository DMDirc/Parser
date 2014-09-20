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

package com.dmdirc.parser.irc.processors;

import com.dmdirc.harness.parser.TestParser;
import com.dmdirc.parser.common.CallbackNotFoundException;
import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.interfaces.Parser;
import com.dmdirc.parser.interfaces.callbacks.ErrorInfoListener;
import com.dmdirc.parser.irc.IRCChannelClientInfo;

import java.util.Date;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ProcessNamesTest {
    
    @Test
    public void testExternalNames() throws CallbackNotFoundException {
        final TestParser parser = new TestParser();
        final ErrorInfoListener test = mock(ErrorInfoListener.class);
        parser.injectConnectionStrings();
        parser.getCallbackManager().addCallback(ErrorInfoListener.class, test);
        
        parser.injectLine(":server 366 nick #nonexistant :End of /NAMES list.");
        
        verify(test, never()).onErrorInfo((Parser) anyObject(),
                (Date) anyObject(), (ParserError) anyObject());
    }
    
    @Test
    public void testChannelUmodes() {
        final TestParser parser = new TestParser();

        parser.injectConnectionStrings();
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":server 353 nick = #DMDirc_testing :@nick +luser @+nick2 nick3");
        parser.injectLine(":server 366 nick #DMDirc_testing :End of /NAMES list");

        assertEquals(1, parser.getChannels().size());
        assertNotNull(parser.getChannel("#DMDirc_testing"));
        assertEquals(4, parser.getChannel("#DMDirc_testing").getChannelClients().size());
        assertNotNull(parser.getClient("luser"));
        assertEquals(1, parser.getClient("luser").getChannelClients().size());

        IRCChannelClientInfo cci = parser.getClient("luser").getChannelClients().get(0);
        assertEquals(parser.getChannel("#DMDirc_testing"), cci.getChannel());
        assertEquals("+", cci.getAllModesPrefix());
        
        cci = parser.getChannel("#DMDirc_testing").getChannelClient("nick2");
        assertNotNull(cci);
        assertEquals("@+", cci.getAllModesPrefix());

        cci = parser.getChannel("#DMDirc_testing").getChannelClient("nick3");
        assertNotNull(cci);
        assertEquals("", cci.getAllModesPrefix());
    }

}