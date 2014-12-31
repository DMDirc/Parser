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

package com.dmdirc.parser.irc;

import com.dmdirc.parser.interfaces.Encoder;
import com.dmdirc.parser.irc.IRCReader.ReadLine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class IRCReaderTest {

    /** Reads and verifies a single line. */
    @Test
    public void testReadLine() throws IOException {
        final InputStream stream = new ByteArrayInputStream("test\r\n".getBytes());
        final Encoder encoder = mock(Encoder.class);

        final IRCReader reader = new IRCReader(stream, encoder);
        final ReadLine line = reader.readLine();

        assertEquals("test", line.getLine());
    }

    /** Reads and verifies a single line with only a trailing LF. */
    @Test
    public void testReadLineWithOnlyLF() throws IOException {
        final InputStream stream = new ByteArrayInputStream("test\n".getBytes());
        final Encoder encoder = mock(Encoder.class);

        final IRCReader reader = new IRCReader(stream, encoder);
        final ReadLine line = reader.readLine();

        assertEquals("test", line.getLine());
    }

    /** Reads and verifies a single line with a trailing parameter. */
    @Test
    public void testReadLineWithParameter() throws IOException {
        final InputStream stream = new ByteArrayInputStream("te t :foo\r\n".getBytes());
        final Encoder encoder = mock(Encoder.class);

        when(encoder.encode((String) isNull(), (String) isNull(),
                (byte[]) anyObject(), anyInt(), eq(3)))
                .thenReturn("encoded");

        final IRCReader reader = new IRCReader(stream, encoder);
        final ReadLine line = reader.readLine();

        assertArrayEquals(new String[]{"te", "t", "encoded",},
                line.getTokens());
    }

    /** Reads and verifies a single line with a trailing parameter. */
    @Test
    public void testReadLineWithMultipleSpaces() throws IOException {
        final InputStream stream = new ByteArrayInputStream("foo bar  baz  :qux  baz\r\n".getBytes());
        final Encoder encoder = mock(Encoder.class);

        when(encoder.encode((String) isNull(), (String) isNull(),
                (byte[]) anyObject(), eq(15), eq(8)))
                .thenReturn("qux  baz");

        final IRCReader reader = new IRCReader(stream, encoder);
        final ReadLine line = reader.readLine();

        assertArrayEquals(new String[]{"foo", "bar", "baz", "qux  baz",}, line.getTokens());
    }

    /** Verifies that a source is extracted properly. */
    @Test
    public void testGetSource() throws IOException {
        final InputStream stream = new ByteArrayInputStream(":src x :o\r\n".getBytes());
        final Encoder encoder = mock(Encoder.class);

        new IRCReader(stream, encoder).readLine();

        verify(encoder).encode(eq("src"), anyString(),
                (byte[]) anyObject(), anyInt(), anyInt());
    }

    /** Verifies that no source is passed if the source is empty. */
    @Test
    public void testGetEmptySource() throws IOException {
        final InputStream stream = new ByteArrayInputStream(": rc x :o\r\n".getBytes());
        final Encoder encoder = mock(Encoder.class);

        new IRCReader(stream, encoder).readLine();

        verify(encoder).encode((String) isNull(), anyString(),
                (byte[]) anyObject(), anyInt(), anyInt());
    }

    /** Verifies that a destination is extracted properly. */
    @Test
    public void testGetDestination() throws IOException {
        final InputStream stream = new ByteArrayInputStream(":src x y :z\r\n".getBytes());
        final Encoder encoder = mock(Encoder.class);

        new IRCReader(stream, encoder).readLine();

        verify(encoder).encode(anyString(), eq("y"), (byte[]) anyObject(),
                anyInt(), anyInt());
    }

    /** Verifies that no destination is extracted if there's no source. */
    @Test
    public void testGetDestinationNoSource() throws IOException {
        final InputStream stream = new ByteArrayInputStream("_src x y :z\r\n".getBytes());
        final Encoder encoder = mock(Encoder.class);

        new IRCReader(stream, encoder).readLine();

        verify(encoder).encode(anyString(), (String) isNull(),
                (byte[]) anyObject(), anyInt(), anyInt());
    }

    /** Verifies that no destination is extracted if there are too few args. */
    @Test
    public void testGetDestinationTooShort() throws IOException {
        final InputStream stream = new ByteArrayInputStream(":src x :abz\r\n".getBytes());
        final Encoder encoder = mock(Encoder.class);

        new IRCReader(stream, encoder).readLine();

        verify(encoder).encode(anyString(), (String) isNull(),
                (byte[]) anyObject(), anyInt(), anyInt());
    }

    /** Verifies that a numeric's destination is extracted properly. */
    @Test
    public void testGetDestinationWithNumeric() throws IOException {
        final InputStream stream = new ByteArrayInputStream(":src 1 x y z :x\r\n".getBytes());
        final Encoder encoder = mock(Encoder.class);

        new IRCReader(stream, encoder).readLine();

        verify(encoder).encode(anyString(), eq("y"), (byte[]) anyObject(), anyInt(), anyInt());
    }

    /** Verifies that the close call is proxied to the stream. */
    @Test
    public void testClose() throws IOException {
        final InputStream stream = mock(InputStream.class);
        final Encoder encoder = mock(Encoder.class);

        new IRCReader(stream, encoder).close();

        verify(stream).close();
    }

    /** Verifies that the reader works with improperly coded unicode. */
    @Test
    public void testHandlesBadCoding() throws IOException {
        final InputStream stream = mock(InputStream.class);
        final Encoder encoder = mock(Encoder.class);

        when(stream.read()).thenReturn((int) ':', (int) 's', (int) 'r', (int) 'c', (int) ' ',
                (int) '1', (int) ' ', 0xF6, (int) ' ', (int) 'y', (int) ' ', (int) 'z', (int) ' ',
                (int) ':', (int) 'x', (int) '\r', (int) '\n');

        when(encoder.encode(anyString(), anyString(),
                (byte[]) any(), anyInt(), anyInt())).thenReturn("x");

        final ReadLine line = new IRCReader(stream, encoder, Charset.forName("UTF-8")).readLine();

        Assert.assertArrayEquals(new String[]{":src", "1", "\uFFFD", "y", "z", "x"}, line.getTokens());
    }

    /** Verify tokeniser with TSIRC Time Stamp */
    @Test
    public void testReadLineTSIRC() throws IOException {
        final ReadLine line = new ReadLine("", IRCParser.tokeniseLine("@123@:test ing"));

        assertEquals(":test", line.getTokens()[0]);
        assertEquals("ing", line.getTokens()[1]);
        assertEquals("123", line.getTags().get("tsirc date"));
        assertNull(line.getTags().get("time"));
    }

    /** Verify tokeniser with IRCv3 tags */
    @Test
    public void testReadLineIRCv3TS() throws IOException {
        final SimpleDateFormat servertime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        final ReadLine line = new ReadLine("", IRCParser.tokeniseLine("@time="+servertime.format(new Date(123))+";something=else;foobar; :test ing"));

        assertEquals(":test", line.getTokens()[0]);
        assertEquals("ing", line.getTokens()[1]);
        assertNull(line.getTags().get("tsirc date"));
        assertEquals(servertime.format(new Date(123)), line.getTags().get("time"));
    }


    /** Verify tokeniser with a numeric tag timestamp */
    @Test
    public void testReadLineNumericTag() throws IOException {
        final ReadLine line = new ReadLine("", IRCParser.tokeniseLine("@123 :test ing"));

        assertEquals(":test", line.getTokens()[0]);
        assertEquals("ing", line.getTokens()[1]);
        assertTrue(line.getTags().containsKey("123"));
    }


    /** Verify line with TSIRC Time Stamp */
    @Test
    public void testReaderTSIRC() throws IOException {
        final InputStream stream = new ByteArrayInputStream("@123@:test ing\r\n".getBytes());
        final Encoder encoder = mock(Encoder.class);

        final IRCReader reader = new IRCReader(stream, encoder);
        final ReadLine line = reader.readLine();

        assertEquals(":test", line.getTokens()[0]);
        assertEquals("ing", line.getTokens()[1]);
        assertEquals("123", line.getTags().get("tsirc date"));
        assertNull(line.getTags().get("time"));
    }

    /** Verify line with IRCv3 timestamp */
    @Test
    public void testReaderIRCv3TS() throws IOException {
        final SimpleDateFormat servertime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        final InputStream stream = new ByteArrayInputStream(("@time="+servertime.format(new Date(123))+";something=else;foobar; :test ing\r\n").getBytes());
        final Encoder encoder = mock(Encoder.class);

        final IRCReader reader = new IRCReader(stream, encoder);
        final ReadLine line = reader.readLine();

        assertEquals(":test", line.getTokens()[0]);
        assertEquals("ing", line.getTokens()[1]);
        assertNull(line.getTags().get("tsirc date"));
        assertEquals(servertime.format(new Date(123)), line.getTags().get("time"));
    }

    /** Verify line with a numeric tag timestamp */
    @Test
    public void testReaderNumericTag() throws IOException {
        final InputStream stream = new ByteArrayInputStream("@123 :test ing\r\n".getBytes());
        final Encoder encoder = mock(Encoder.class);

        final IRCReader reader = new IRCReader(stream, encoder);
        final ReadLine line = reader.readLine();

        assertEquals(":test", line.getTokens()[0]);
        assertEquals("ing", line.getTokens()[1]);
        assertTrue(line.getTags().containsKey("123"));
    }
}
