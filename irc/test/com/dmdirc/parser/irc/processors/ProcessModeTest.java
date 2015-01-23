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

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Ignore
public class ProcessModeTest {
    
    @Test
    public void testBasicUmodes() throws CallbackNotFoundException {
        final TestParser parser = new TestParser();

        parser.injectConnectionStrings();

        parser.injectLine(":server 221 nick iw");

        assertTrue(parser.getLocalClient().getModes().indexOf('i') > -1);
        assertTrue(parser.getLocalClient().getModes().indexOf('w') > -1);
    }
    
    @Test
    public void testAlteringUmodes() throws CallbackNotFoundException {
        final TestParser parser = new TestParser();

        parser.injectConnectionStrings();

        parser.injectLine(":server 221 nick iw");
        parser.injectLine(":server MODE nick :-iw+ox");

        assertTrue(parser.getLocalClient().getModes().indexOf('o') > -1);
        assertTrue(parser.getLocalClient().getModes().indexOf('x') > -1);
        assertEquals(-1, parser.getLocalClient().getModes().indexOf('i'));
        assertEquals(-1, parser.getLocalClient().getModes().indexOf('w'));
    }
    
    @Test
    public void testChannelUmodes() {
        final TestParser parser = new TestParser();

        parser.injectConnectionStrings();
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":server 353 nick = #DMDirc_testing :@nick +luser");
        parser.injectLine(":server 366 nick #DMDirc_testing :End of /NAMES list");

        final ChannelClientInfo cci = parser.getClient("luser").getChannelClients().get(0);

        parser.injectLine(":server MODE #DMDirc_testing +v luser");
        assertEquals("v", cci.getAllModes());
        assertEquals("+", cci.getAllModesPrefix());

        parser.injectLine(":server MODE #DMDirc_testing +o luser");
        assertEquals("ov", cci.getAllModes());
        assertEquals("@+", cci.getAllModesPrefix());

        parser.injectLine(":server MODE #DMDirc_testing +bov moo luser luser");
        assertEquals("ov", cci.getAllModes());

        parser.injectLine(":server MODE #DMDirc_testing -bov moo luser luser");
        assertEquals("", cci.getAllModes());
        assertEquals("", cci.getAllModesPrefix());
    }
    
    @Test
    public void testUnknownUser1() {
        final TestParser parser = new TestParser();

        parser.injectConnectionStrings();
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":server 353 nick = #DMDirc_testing :@nick +luser");
        parser.injectLine(":server 366 nick #DMDirc_testing :End of /NAMES list");

        parser.injectLine(":luser!me@my MODE #DMDirc_testing +v :moo");
        
        assertNotNull(parser.getClient("moo"));
        assertEquals(0, parser.getClient("moo").getChannelCount());
        
        assertEquals("Parser should update ident when it sees a MODE line",
                "me", parser.getClient("luser").getUsername());
        assertEquals("Parser should update host when it sees a MODE line",
                "my", parser.getClient("luser").getHostname());
    }
    
    @Test
    public void testChannelModes() {
        final TestParser parser = new TestParser();

        parser.injectConnectionStrings();
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":server 353 nick = #DMDirc_testing :@nick +luser");
        parser.injectLine(":server 366 nick #DMDirc_testing :End of /NAMES list");
        parser.injectLine(":server 324 nick #DMDirc_testing +Zstnl 1234");

        assertEquals("1234", parser.getChannel("#DMDirc_testing").getMode('l'));
        
        String modes = parser.getChannel("#DMDirc_testing").getModes().split(" ")[0];
        assertEquals(6, modes.length());
        assertEquals('+', modes.charAt(0));
        assertTrue(modes.indexOf('Z') > -1);
        assertTrue(modes.indexOf('s') > -1);
        assertTrue(modes.indexOf('t') > -1);
        assertTrue(modes.indexOf('n') > -1);
        assertTrue(modes.indexOf('l') > -1);
        
        parser.injectLine(":server MODE #DMDirc_testing :-Z");
        
        modes = parser.getChannel("#DMDirc_testing").getModes().split(" ")[0];
        assertEquals(5, modes.length());
        assertEquals('+', modes.charAt(0));
        assertTrue(modes.indexOf('s') > -1);
        assertTrue(modes.indexOf('t') > -1);
        assertTrue(modes.indexOf('n') > -1);
        assertTrue(modes.indexOf('l') > -1);        
    }

}
