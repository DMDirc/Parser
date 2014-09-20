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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrefixModeManagerTest {

    private PrefixModeManager manager;

    @Before
    public void setup() {
        manager = new PrefixModeManager();
    }

    @Test
    public void testGetModeFor() {
        manager.add('m', '/');
        assertEquals('m', manager.getModeFor('/'));
    }

    @Test
    public void testGetPrefixFor() {
        manager.add('m', '/');
        assertEquals('/', manager.getPrefixFor('m'));
    }

    @Test
    public void testIsPrefixMode() {
        manager.add('m', '/');
        assertTrue(manager.isPrefixMode('m'));
        assertFalse(manager.isPrefixMode('/'));
        assertFalse(manager.isPrefixMode('o'));
    }

    @Test
    public void testIsPrefix() {
        manager.add('m', '/');
        assertFalse(manager.isPrefix('m'));
        assertTrue(manager.isPrefix('/'));
        assertFalse(manager.isPrefix('+'));
    }

    @Test
    public void testGetPrefixesFor() {
        manager.add('m', '/');
        manager.add('n', '+');
        assertEquals("//++//", manager.getPrefixesFor("mmnnmm"));
        assertEquals("", manager.getPrefixesFor(""));
        assertEquals("/", manager.getPrefixesFor("m"));
        assertEquals("+/", manager.getPrefixesFor("nm"));
    }

    @Test
    public void testGetModes() {
        assertEquals(0, manager.getModes().length());
        manager.add('m', '/');
        assertEquals(1, manager.getModes().length());
        assertEquals("m", manager.getModes());
        manager.add('n', '+');
        assertEquals(2, manager.getModes().length());
        assertEquals("mn", manager.getModes());
        manager.clear();
        assertEquals(0, manager.getModes().length());
    }

    @Test
    public void testIsOpped() {
        assertFalse(manager.isOpped(""));
        manager.add('k', '_');

        // If there isn't a 'v' mode, then any mode equals opped
        assertFalse(manager.isOpped(""));
        assertTrue(manager.isOpped("k"));

        manager.add('v', '+');
        assertFalse(manager.isOpped(""));
        assertFalse(manager.isOpped("k"));
        assertFalse(manager.isOpped("v"));

        manager.add('o', '@');
        assertFalse(manager.isOpped(""));
        assertFalse(manager.isOpped("k"));
        assertFalse(manager.isOpped("v"));
        assertTrue(manager.isOpped("o"));

        manager.add('z', '*');
        assertFalse(manager.isOpped(""));
        assertFalse(manager.isOpped("k"));
        assertFalse(manager.isOpped("v"));
        assertTrue(manager.isOpped("o"));
        assertTrue(manager.isOpped("z"));
    }

    @Test
    public void testInsertMode() {
        manager.add('m', '/');
        manager.add('n', '+');
        manager.add('o', '@');

        // Already exist in modes
        assertEquals("mn", manager.insertMode("mn", 'm'));
        assertEquals("m", manager.insertMode("m", 'm'));

        // Most important ones go at the start
        assertEquals("om", manager.insertMode("m", 'o'));
        assertEquals("nm", manager.insertMode("m", 'n'));
        assertEquals("onm", manager.insertMode("nm", 'o'));

        // Inserting elsewhere
        assertEquals("om", manager.insertMode("o", 'm'));
        assertEquals("onm", manager.insertMode("on", 'm'));
        assertEquals("onm", manager.insertMode("om", 'n'));
        assertEquals("nm", manager.insertMode("n", 'm'));

        // Build up a string from scratch
        assertEquals("onm",
                manager.insertMode(manager.insertMode(manager.insertMode("", 'n'), 'm'), 'o'));
    }

    @Test
    public void testRemoveMode() {
        manager.add('m', '/');
        manager.add('n', '+');
        manager.add('o', '@');

        assertEquals("", manager.removeMode("", 'm'));
        assertEquals("", manager.removeMode("m", 'm'));
        assertEquals("no", manager.removeMode("no", 'm'));
        assertEquals("no", manager.removeMode("mno", 'm'));
        assertEquals("mo", manager.removeMode("mno", 'n'));
    }

    @Test
    public void testCompareImportantModes() {
        // No modes, must be equal
        assertEquals(0, manager.compareImportantModes("", ""));

        manager.add('m', '/');
        assertEquals(0, manager.compareImportantModes("", ""));
        assertTrue(manager.compareImportantModes("m", "") > 0);
        assertTrue(manager.compareImportantModes("", "m") < 0);

        manager.add('n', '+');
        assertEquals(0, manager.compareImportantModes("", ""));
        assertTrue(manager.compareImportantModes("m", "") > 0);
        assertTrue(manager.compareImportantModes("", "m") < 0);
        assertTrue(manager.compareImportantModes("n", "") > 0);
        assertTrue(manager.compareImportantModes("", "n") < 0);
        assertTrue(manager.compareImportantModes("n", "m") > 0);
        assertTrue(manager.compareImportantModes("m", "n") < 0);
        assertTrue(manager.compareImportantModes("nm", "m") > 0);
        assertTrue(manager.compareImportantModes("m", "nm") < 0);

        manager.add('o', '@');
        assertEquals(0, manager.compareImportantModes("", ""));
        assertTrue(manager.compareImportantModes("nm", "m") > 0);
        assertTrue(manager.compareImportantModes("m", "nm") < 0);
        assertTrue(manager.compareImportantModes("o", "nm") > 0);
        assertTrue(manager.compareImportantModes("nm", "om") < 0);
        assertTrue(manager.compareImportantModes("nm", "on") < 0);
    }

}
