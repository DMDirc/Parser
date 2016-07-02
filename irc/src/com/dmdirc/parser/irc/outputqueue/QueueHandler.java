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

package com.dmdirc.parser.irc.outputqueue;

import com.dmdirc.parser.common.QueuePriority;

import java.io.PrintWriter;
import java.util.Comparator;

/**
 * Sending queue.
 */
public abstract class QueueHandler extends Thread {

    /** The output queue that owns us. */
    protected OutputQueue outputQueue;
    /** Comparator to use to sort queue items. */
    private final Comparator<QueueItem> comparator;
    /** Where to send the output. */
    private final PrintWriter out;

    /**
     * Create a new Queue Thread.
     *
     * @param outputQueue the OutputQueue that owns us.
     * @param comparator Comparator to use to sort items in the queue.
     * @param out Writer to send to.
     */
    public QueueHandler(
            final OutputQueue outputQueue,
            final Comparator<QueueItem> comparator,
            final PrintWriter out) {
        super("IRC Parser queue handler");

        this.comparator = comparator;
        this.out = out;
        this.outputQueue = outputQueue;
    }

    /**
     * Send the given item.
     *
     * @param line Line to send.
     */
    public void sendLine(final String line) {
        out.printf("%s\r\n", line);
    }

    /**
     * Get a new QueueItem for the given line and priority.
     * By default this will just create a new QueueItem with the given
     * parameters, but QueueHandlers are free to override it if they need to
     * instead produce subclasses of QueueItem or do anything else with the
     * data given.
     *
     * @param line Line to send
     * @param priority Priority of the line.
     * @return A QueueItem for teh given parameters
     */
    public QueueItem getQueueItem(final String line, final QueuePriority priority) {
        return new QueueItem(line, priority);
    }

    /**
     * Gets the comparator to use to sort queue items.
     *
     * @return The comparator to use to sort queue items.
     */
    public Comparator<QueueItem> getQueueItemComparator() {
        return comparator;
    }

    /**
     * This is the main even loop of the queue.
     * It needs to handle pulling items out of the queue and calling
     * sendLine.
     *
     * It also needs to handle any delays in sending that it deems needed.
     */
    @Override
    public abstract void run();

}
