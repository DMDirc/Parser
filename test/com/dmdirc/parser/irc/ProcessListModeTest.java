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
import com.dmdirc.parser.common.ChannelListModeItem;
import java.util.Collection;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProcessListModeTest {

    private void testListModes(String numeric1, String numeric2, char mode) {
        final TestParser parser = new TestParser();
        parser.injectConnectionStrings();

        parser.injectLine(":nick JOIN #D");
        parser.injectLine(":server " + numeric1 + " nick #D ban1!ident@.host bansetter1 1001");
        parser.injectLine(":server " + numeric1 + " nick #D ban2!*@.host bansetter2 1002");
        parser.injectLine(":server " + numeric1 + " nick #D ban3!ident@* bansetter3 1003");
        parser.injectLine(":server " + numeric2 + " nick #D :End of Channel Something List");

        final Collection<ChannelListModeItem> items
                = parser.getChannel("#D").getListMode(mode);

        assertEquals(3, items.size());
        boolean gotOne = false, gotTwo = false, gotThree = false;

        for (ChannelListModeItem item : items) {
            if (item.getItem().equals("ban1!ident@.host")) {
                assertEquals("bansetter1", item.getOwner());
                assertEquals(1001L, item.getTime());
                assertFalse(gotOne);
                gotOne = true;
            } else if (item.getItem().equals("ban2!*@.host")) {
                assertEquals("bansetter2", item.getOwner());
                assertEquals(1002L, item.getTime());
                assertFalse(gotTwo);
                gotTwo = true;
            } else if (item.toString().equals("ban3!ident@*")) {
                assertEquals("bansetter3", item.getOwner());
                assertEquals(1003L, item.getTime());
                assertFalse(gotThree);
                gotThree = true;
            }
        }

        assertTrue(gotOne);
        assertTrue(gotTwo);
        assertTrue(gotThree);
    }

    @Test
    public void testNormalBans() {
        testListModes("367", "368", 'b');
    }

    @Test
    public void testInvexList() {
        testListModes("346", "347", 'I');
    }

    @Test
    public void testExemptList() {
        testListModes("348", "349", 'e');
    }

    @Test
    public void testReopList() {
        testListModes("344", "345", 'R');
    }

}
