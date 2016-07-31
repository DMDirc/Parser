/*
 * Copyright (c) 2006-2016 DMDirc Developers
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

package com.dmdirc.parser.irc.outputqueue;

import com.dmdirc.parser.common.QueuePriority;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static org.junit.Assert.assertEquals;

public class PriorityOutputQueueTest {

    private BufferedReader reader;
    private BufferedOutputStream outputStream;
    private PriorityOutputQueue outputQueue;

    @Before
    public void setup() throws IOException {
        PipedInputStream pipeInput = new PipedInputStream();
        reader = new BufferedReader(new InputStreamReader(pipeInput));
        outputStream = new BufferedOutputStream(new PipedOutputStream(pipeInput));
        outputQueue = new PriorityOutputQueue();
    }

    @Test(expected = IllegalStateException.class)
    public void testThrowsIfOutputStreamNotSet() {
        outputQueue.sendLine("testing", QueuePriority.IMMEDIATE);
    }

    @Test
    public void testSendsLinesToOutput() throws IOException {
        outputQueue.setOutputStream(outputStream);
        outputQueue.sendLine("test 123");
        outputQueue.sendLine("456...");
        assertEquals("test 123", reader.readLine());
        assertEquals("456...", reader.readLine());
    }

    @Test
    public void testDiscarding() throws IOException {
        outputQueue.setOutputStream(outputStream);
        outputQueue.setDiscarding(true);
        outputQueue.sendLine("test 123");
        outputQueue.setDiscarding(false);
        outputQueue.sendLine("456...");
        assertEquals("456...", reader.readLine());
    }

}