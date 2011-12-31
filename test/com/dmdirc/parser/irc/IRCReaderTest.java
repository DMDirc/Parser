/*
 * Copyright (c) 2006-2012 DMDirc Developers
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

import com.dmdirc.parser.irc.IRCReader.ReadLine;
import com.dmdirc.parser.interfaces.Encoder;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class IRCReaderTest {

    /** Reads and verifies a single line. */
    @Test
    public void testReadLine() throws IOException {
        final InputStream stream = mock(InputStream.class);
        final Encoder encoder = mock(Encoder.class);

        when(stream.read()).thenReturn((int) 't', (int) 'e', (int) 's',
                (int) 't', (int) '\r', (int) '\n');

        final IRCReader reader = new IRCReader(stream, encoder);
        final ReadLine line = reader.readLine();

        assertEquals("test", line.getLine());
    }

    /** Reads and verifies a single line with only a trailing LF. */
    @Test
    public void testReadLineWithOnlyLF() throws IOException {
        final InputStream stream = mock(InputStream.class);
        final Encoder encoder = mock(Encoder.class);

        when(stream.read()).thenReturn((int) 't', (int) 'e', (int) 's',
                (int) 't', (int) '\n');

        final IRCReader reader = new IRCReader(stream, encoder);
        final ReadLine line = reader.readLine();

        assertEquals("test", line.getLine());
    }

    /** Reads and verifies a single line with a trailing parameter. */
    @Test
    public void testReadLineWithParameter() throws IOException {
        final InputStream stream = mock(InputStream.class);
        final Encoder encoder = mock(Encoder.class);

        when(stream.read()).thenReturn((int) 't', (int) 'e', (int) ' ',
                (int) 't', (int) ' ', (int) ':', (int) 'f', (int) 'o',
                (int) 'o', (int) '\r', (int) '\n');
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
        final InputStream stream = mock(InputStream.class);
        final Encoder encoder = mock(Encoder.class);

        when(stream.read()).thenReturn((int) 'f', (int) 'o', (int) 'o',
                (int) ' ', (int) 'b', (int) 'a', (int) 'r',
                (int) ' ', (int) ' ', (int) 'b', (int) 'a', (int) 'z',
                (int) ' ', (int) ' ', (int) ':', (int) 'q', (int) 'u',
                (int) 'x', (int) ' ', (int) ' ', (int) 'b', (int) 'a', (int) 'z',
                (int) '\r', (int) '\n');
        when(encoder.encode((String) isNull(), (String) isNull(),
                (byte[]) anyObject(), eq(15), eq(8)))
                .thenReturn("qux  baz");

        final IRCReader reader = new IRCReader(stream, encoder);
        final ReadLine line = reader.readLine();

        assertArrayEquals(new String[]{"foo", "bar", "baz", "qux  baz",},
                line.getTokens());
    }

    /** Verifies that a source is extracted properly. */
    @Test
    public void testGetSource() throws IOException {
        final InputStream stream = mock(InputStream.class);
        final Encoder encoder = mock(Encoder.class);

        when(stream.read()).thenReturn((int) ':', (int) 's', (int) 'r',
                (int) 'c', (int) ' ', (int) 'x', (int) ' ', (int) ':',
                (int) 'o', (int) '\r', (int) '\n');

        new IRCReader(stream, encoder).readLine();

        verify(encoder).encode(eq("src"), anyString(),
                (byte[]) anyObject(), anyInt(), anyInt());
    }

    /** Verifies that no source is passed if the source is empty. */
    @Test
    public void testGetEmptySource() throws IOException {
        final InputStream stream = mock(InputStream.class);
        final Encoder encoder = mock(Encoder.class);

        when(stream.read()).thenReturn((int) ':', (int) ' ', (int) 'r',
                (int) 'c', (int) ' ', (int) 'x', (int) ' ', (int) ':',
                (int) 'o', (int) '\r', (int) '\n');

        new IRCReader(stream, encoder).readLine();

        verify(encoder).encode((String) isNull(), anyString(),
                (byte[]) anyObject(), anyInt(), anyInt());
    }

    /** Verifies that a destination is extracted properly. */
    @Test
    public void testGetDestination() throws IOException {
        final InputStream stream = mock(InputStream.class);
        final Encoder encoder = mock(Encoder.class);

        when(stream.read()).thenReturn((int) ':', (int) 's', (int) 'r',
                (int) 'c', (int) ' ', (int) 'x', (int) ' ', (int) 'y',
                (int) ' ', (int) ':', (int) 'z', (int) '\r', (int) '\n');

        new IRCReader(stream, encoder).readLine();

        verify(encoder).encode(anyString(), eq("y"), (byte[]) anyObject(),
                anyInt(), anyInt());
    }

    /** Verifies that no destination is extracted if there's no source. */
    @Test
    public void testGetDestinationNoSource() throws IOException {
        final InputStream stream = mock(InputStream.class);
        final Encoder encoder = mock(Encoder.class);

        when(stream.read()).thenReturn((int) '_', (int) 's', (int) 'r',
                (int) 'c', (int) ' ', (int) 'x', (int) ' ', (int) 'y',
                (int) ' ', (int) ':', (int) 'z', (int) '\r', (int) '\n');

        new IRCReader(stream, encoder).readLine();

        verify(encoder).encode(anyString(), (String) isNull(),
                (byte[]) anyObject(), anyInt(), anyInt());
    }

    /** Verifies that no destination is extracted if there are too few args. */
    @Test
    public void testGetDestinationTooShort() throws IOException {
        final InputStream stream = mock(InputStream.class);
        final Encoder encoder = mock(Encoder.class);

        when(stream.read()).thenReturn((int) ':', (int) 's', (int) 'r',
                (int) 'c', (int) ' ', (int) 'x', (int) ' ', (int) ':',
                (int) 'a', (int) 'b', (int) 'z', (int) '\r', (int) '\n');

        new IRCReader(stream, encoder).readLine();

        verify(encoder).encode(anyString(), (String) isNull(),
                (byte[]) anyObject(), anyInt(), anyInt());
    }

    /** Verifies that a numeric's destination is extracted properly. */
    @Test
    public void testGetDestinationWithNumeric() throws IOException {
        final InputStream stream = mock(InputStream.class);
        final Encoder encoder = mock(Encoder.class);

        when(stream.read()).thenReturn((int) ':', (int) 's', (int) 'r',
                (int) 'c', (int) ' ', (int) '1', (int) ' ', (int) 'x',
                (int) ' ', (int) 'y', (int) ' ', (int) 'z', (int) ' ',
                (int) ':', (int) 'x', (int) '\r', (int) '\n');

        new IRCReader(stream, encoder).readLine();

        verify(encoder).encode(anyString(), eq("y"),
                (byte[]) anyObject(), anyInt(), anyInt());
    }

    /** Verifies that the close call is proxied to the stream. */
    @Test
    public void testClose() throws IOException {
        final InputStream stream = mock(InputStream.class);
        final Encoder encoder = mock(Encoder.class);

        new IRCReader(stream, encoder).close();

        verify(stream).close();
    }

}
