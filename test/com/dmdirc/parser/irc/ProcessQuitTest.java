/*
 * Copyright (c) 2006-2011 Chris Smith, Shane Mc Cormack, Gregory Holmes
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
import com.dmdirc.parser.interfaces.callbacks.ChannelQuitListener;
import com.dmdirc.parser.interfaces.callbacks.QuitListener;
import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ClientInfo;
import com.dmdirc.parser.common.CallbackNotFoundException;
import java.util.Date;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ProcessQuitTest {
    
    @Test
    public void testChannelQuit() throws CallbackNotFoundException {
        final TestParser parser = new TestParser();

        parser.injectConnectionStrings();

        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":server 353 nick = #DMDirc_testing :@nick +luser");
        parser.injectLine(":server 366 nick #DMDirc_testing :End of /NAMES list.");
        parser.injectLine(":nick JOIN #DMDirc_testing2");
        parser.injectLine(":server 353 nick = #DMDirc_testing2 :@nick +luser2");
        parser.injectLine(":server 366 nick #DMDirc_testing2 :End of /NAMES list.");        
        
        final ChannelQuitListener test = mock(ChannelQuitListener.class);
        parser.getCallbackManager().addCallback(ChannelQuitListener.class, test);
        
        assertEquals(2, parser.getChannel("#DMDirc_testing").getChannelClients().size());

        final ChannelClientInfo cci = parser.getChannel("#DMDirc_testing")
                .getChannelClient("luser");
        
        parser.injectLine(":luser!foo@barsville QUIT :Bye bye, cruel world");
        
        assertEquals(1, parser.getChannel("#DMDirc_testing").getChannelClients().size());
        assertEquals(2, parser.getChannel("#DMDirc_testing2").getChannelClients().size());

        verify(test).onChannelQuit(same(parser), (Date) anyObject(),
                same(parser.getChannel("#DMDirc_testing")), same(cci),
                eq("Bye bye, cruel world"));
    }
    
    @Test
    public void testGlobalQuit() throws CallbackNotFoundException {
        final TestParser parser = new TestParser();

        parser.injectConnectionStrings();

        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":server 353 nick = #DMDirc_testing :@nick +luser");
        parser.injectLine(":server 366 nick #DMDirc_testing :End of /NAMES list.");
        parser.injectLine(":nick JOIN #DMDirc_testing2");
        parser.injectLine(":server 353 nick = #DMDirc_testing2 :@nick +luser2");
        parser.injectLine(":server 366 nick #DMDirc_testing2 :End of /NAMES list.");
        
        final QuitListener test = mock(QuitListener.class);
        parser.getCallbackManager().addCallback(QuitListener.class, test);
        
        assertEquals(2, parser.getChannel("#DMDirc_testing").getChannelClients().size());

        final ClientInfo client = parser.getClient("luser");
        
        parser.injectLine(":luser!foo@barsville QUIT :Bye bye, cruel world");
        
        assertEquals(1, parser.getChannel("#DMDirc_testing").getChannelClients().size());
        assertEquals(2, parser.getChannel("#DMDirc_testing2").getChannelClients().size());

        verify(test).onQuit(same(parser), (Date) anyObject(),
                same(client), eq("Bye bye, cruel world"));
    }
    
    @Test
    public void testEmptyQuit() throws CallbackNotFoundException {
        final TestParser parser = new TestParser();

        parser.injectConnectionStrings();

        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":server 353 nick = #DMDirc_testing :@nick +luser");
        parser.injectLine(":server 366 nick #DMDirc_testing :End of /NAMES list.");
        
        final QuitListener test = mock(QuitListener.class);
        parser.getCallbackManager().addCallback(QuitListener.class, test);
        
        assertEquals(2, parser.getChannel("#DMDirc_testing").getChannelClients().size());

        final ClientInfo client = parser.getClient("luser");
        
        parser.injectLine(":luser!foo@barsville QUIT");
        
        assertEquals(1, parser.getChannel("#DMDirc_testing").getChannelClients().size());
        
        verify(test).onQuit(same(parser), (Date) anyObject(), same(client), eq(""));
    }    

}
