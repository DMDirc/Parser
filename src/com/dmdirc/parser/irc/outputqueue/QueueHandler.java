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

package com.dmdirc.parser.irc.outputqueue;

import com.dmdirc.parser.common.QueuePriority;

import java.io.PrintWriter;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;

/**
 * Sending queue.
 */
public abstract class QueueHandler extends Thread implements Comparator<QueueItem> {

    /** Queue we are handling. */
    protected final BlockingQueue<QueueItem> queue;
    /** The output queue that owns us. */
    protected OutputQueue outputQueue;
    /** Where to send the output. */
    private final PrintWriter out;

    /**
     * Create a new Queue Thread.
     *
     * @param outputQueue the OutputQueue that owns us.
     * @param queue Queue to handle
     * @param out Writer to send to.
     */
    public QueueHandler(final OutputQueue outputQueue, final BlockingQueue<QueueItem> queue, final PrintWriter out) {
        super("IRC Parser queue handler");

        this.queue = queue;
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
        return new QueueItem(this, line, priority);
    }

    /**
     * Compare two QueueItems for sorting purposes.
     * This is called by the default QueueItem in its compareTo method. The
     * calling object will be the first parameter, the object to compare it to
     * will be second.
     * This allows QueueHandlers to sort items differently if needed.
     *
     * The default implementation works as follows:
     * Compare based on priorty firstly, if the priorities are the same,
     * compare based on the order the items were added to the queue.
     *
     * If an item has been in the queue longer than 10 seconds, it will not
     * check its priority and soley position itself based on adding order.
     *
     * @param mainObject Main object we are comparing against.
     * @param otherObject Object we are comparing to.
     * @return A QueueItem for teh given parameters
     */
    @Override
    public int compare(final QueueItem mainObject, final QueueItem otherObject) {
        if (mainObject.getTime() < 10 * 1000 && mainObject.getPriority().compareTo(otherObject.getPriority()) != 0) {
            return mainObject.getPriority().compareTo(otherObject.getPriority());
        }

        if (mainObject.getItemNumber() > otherObject.getItemNumber()) {
            return 1;
        } else if (mainObject.getItemNumber() < otherObject.getItemNumber()) {
            return -1;
        } else {
            // This can't happen.
            return 0;
        }
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
