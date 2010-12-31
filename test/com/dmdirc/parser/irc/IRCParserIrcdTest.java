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
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class IRCParserIrcdTest {

    private final String ircd, expected;

    public IRCParserIrcdTest(String ircd, String expected) {
        this.ircd = ircd;
        this.expected = expected;
    }

    @Test
    public void testIrcdDetection() {
        final TestParser parser = new TestParser();

        String[] strings = {
            ":server 001 nick :Welcome to the Testing IRC Network, nick",
            ":server 002 nick :Your host is server.net, running version %s",
            ":server 003 nick :This server was created Sun Jan 6 2008 at 17:34:54 CET",
            ":server 004 nick server.net %s dioswkgxRXInP biklmnopstvrDcCNuMT bklov"
        };

        for (String line : strings) {
            parser.injectLine(String.format(line, ircd));
        }

        assertEquals(ircd, parser.getServerSoftware());
        assertEquals(expected.toLowerCase(), parser.getServerSoftwareType().toLowerCase());
    }

    @Parameterized.Parameters
    public static List<String[]> data() {
        final String[][] tests = {
            {"u2.10.12.10+snircd(1.3.4)", "snircd"},
            {"u2.10.12.12", "ircu"},
            {"hyperion-1.0.2b", "hyperion"},
            {"hybrid-7.2.3", "hybrid"},
            {"Unreal3.2.6", "unreal"},
            {"bahamut-1.8(04)", "bahamut"},
        };

        return Arrays.asList(tests);
    }

}
