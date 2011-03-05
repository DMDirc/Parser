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

package com.dmdirc.parser.irc.outputqueue;

import com.dmdirc.parser.common.QueuePriority;

/**
 * Queued Item.
 */
public class QueueItem implements Comparable<QueueItem> {

    /** Global Item Number. */
    private static long number;
    /** Line to send. */
    private final String line;
    /** Time this line was added. */
    private final long time;
    /** Item Number. */
    private final long itemNumber;
    /** What is the priority of this line? */
    private final QueuePriority priority;
    /** Our handler. */
    private final QueueHandler handler;

    /**
     * Create a new QueueItem.
     *
     * @param handler Handler for this QueueItem
     * @param line Line to send
     * @param priority Priority for the queue item
     */
    public QueueItem(final QueueHandler handler, final String line, final QueuePriority priority) {
        this.handler = handler;
        this.line = line;
        this.priority = priority;

        this.time = System.currentTimeMillis();
        this.itemNumber = number++;
    }

    /**
     * Get the value of line.
     *
     * @return the value of line
     */
    public String getLine() {
        return line;
    }

    /**
     * Get the value of time.
     *
     * @return the value of time
     */
    public long getTime() {
        return time;
    }

    /**
     * Get the number of this item.
     *
     * @return the value of itemNumber
     */
    public long getItemNumber() {
        return itemNumber;
    }

    /**
     * Get the value of priority.
     *
     * @return the value of priority
     */
    public QueuePriority getPriority() {
        return priority;
    }

    /**
     * Compare objects.
     * This will use the compareQueueItem method of the current QueueHandler.
     *
     * @param o Object to compare to
     * @return Position of this item in reference to the given item.
     */
    @Override
    public int compareTo(final QueueItem o) {
        return handler.compare(this, o);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("[%s %d] %s", priority, time, line);
    }
}
