/*
 * Copyright (c) 2006-2017 DMDirc Developers
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

import com.dmdirc.parser.common.IgnoreList;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class RegexStringListTest {

    @Test
    public void testAdd() {
        final IgnoreList list = new IgnoreList();
        list.add("a!b@c");
        list.add("a!b@c");
        list.add("A!B@c");

        assertEquals(1, list.count());
        assertEquals("a!b@c", list.get(0));
    }

    @Test
    public void testAddAll() {
        final List<String> tempList = new ArrayList<>();
        tempList.add("a!b@c");
        tempList.add("a!b@c");
        tempList.add("A!B@C");
        tempList.add("Data.*");

        final IgnoreList list = new IgnoreList(tempList);

        assertEquals(2, list.count());
        assertEquals("a!b@c", list.get(0));
        assertEquals("Data.*", list.get(1));
    }

    @Test
    public void testRmove() {
        final List<String> tempList = new ArrayList<>();
        tempList.add("a!b@c");
        tempList.add("Data.*");

        final IgnoreList list = new IgnoreList(tempList);
        list.remove(0);
        list.remove(17);

        assertEquals(1, list.count());
        assertEquals("Data.*", list.get(0));
    }

    @Test
    public void testClear() {
        final List<String> tempList = new ArrayList<>();
        tempList.add("a!b@c");
        tempList.add("Data.*");

        final IgnoreList list = new IgnoreList(tempList);
        list.clear();

        assertEquals(0, list.count());
    }

    @Test
    public void testMatches() {
        final List<String> tempList = new ArrayList<>();
        tempList.add("a!b@c");
        tempList.add("Data.*");

        final IgnoreList list = new IgnoreList(tempList);

        assertEquals(0, list.matches("a!b@c"));
        assertEquals(-1, list.matches("foo"));
        assertEquals(1, list.matches("Dataforce"));
        assertFalse(list.matches(100, "Dataforce"));
        assertTrue(list.matches(1, "Dataforce"));
    }
    
    @Test
    public void testGet() {
        final IgnoreList list = new IgnoreList();
        list.add("a!b@c");
        assertEquals("a!b@c", list.get(0));
        assertEquals("", list.get(7));
    }

    @Test
    public void testSet() {
        final IgnoreList list = new IgnoreList();
        list.add("a!b@c");
        list.set(0, "moo");
        list.set(1, "bar");
        assertEquals(1, list.count());
        assertEquals("moo", list.get(0));
    }

}