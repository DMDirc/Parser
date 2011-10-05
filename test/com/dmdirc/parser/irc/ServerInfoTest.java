/*
 * Copyright (c) 2006-2011 DMDirc Developers
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

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;
import static org.junit.Assert.*;

public class ServerInfoTest {
    
    @Test
    public void testHost() throws URISyntaxException {
        final IRCServerInfo si = new IRCServerInfo(new URI("ircs", "pass1", "host0",
                5, null, null, null));
        assertEquals("host0", si.getHost());
    }

    @Test
    public void testPort() throws URISyntaxException {
        final IRCServerInfo si = new IRCServerInfo(new URI("ircs", "pass1", "host0",
                5, null, null, null));
        assertEquals(5, si.getPort());
    }

    @Test
    public void testNoPort() throws URISyntaxException {
        final IRCServerInfo si = new IRCServerInfo(new URI("ircs", "pass1", "host0",
                -1, null, null, null));
        assertEquals(6667, si.getPort());
    }

    @Test
    public void testPassword() throws URISyntaxException {
        final IRCServerInfo si = new IRCServerInfo(new URI("ircs", "pass1", "host0",
                5, null, null, null));
        assertEquals("pass1", si.getPassword());

    }

    @Test
    public void testNoPassword() throws URISyntaxException {
        final IRCServerInfo si = new IRCServerInfo(new URI("ircs", null, "host0",
                5, null, null, null));
        assertEquals("", si.getPassword());

    }

    @Test
    public void testSSL() throws URISyntaxException {
        final IRCServerInfo si = new IRCServerInfo(new URI("ircs", "pass1", "host0",
                5, null, null, null));
        assertTrue(si.isSSL());
    }

    @Test
    public void testNonSSL() throws URISyntaxException {
        final IRCServerInfo si = new IRCServerInfo(new URI("irc", "pass1", "host0",
                5, null, null, null));
        assertFalse(si.isSSL());
    }
    
    @Test
    public void testUseSocks() {
        final IRCServerInfo si = new IRCServerInfo("host0", 5, "pass1");
        assertFalse(si.getUseSocks());
        si.setUseSocks(true);
        assertTrue(si.getUseSocks());
    }
    
    @Test
    public void testProxyHost() {
        final IRCServerInfo si = new IRCServerInfo("host0", 5, "pass1");
        si.setProxyHost("foo");
        assertEquals("foo", si.getProxyHost());
    }
    
    @Test
    public void testProxyPort() {
        final IRCServerInfo si = new IRCServerInfo("host0", 5, "pass1");
        si.setProxyPort(1024);
        assertEquals(1024, si.getProxyPort());
    }
    
}
