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

package com.dmdirc.parser.irc.outputqueue;

import com.dmdirc.parser.common.QueuePriority;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * This class handles the Parser output Queue.
 */
public abstract class OutputQueue {

    /** PrintWriter for sending output. */
    private PrintWriter out;
    /** Is queueing enabled? */
    private boolean queueEnabled = true;
    /** Are we discarding all futher input? */
    private boolean discarding;
    /** The output queue! */
    private final BlockingQueue<QueueItem> queue;
    /** The thread on which we will send items. */
    private Thread sendingThread;

    /**
     * Creates a new instance of {@link OutputQueue} that will sort items using the given
     * comparator.
     *
     * @param itemComparator The comparator to use to sort queued items.
     */
    protected OutputQueue(final Comparator<QueueItem> itemComparator) {
        queue = new PriorityBlockingQueue<>(10, itemComparator);
    }

    /**
     * Set the output stream for this queue.
     *
     * @param outputStream Output Stream to use.
     */
    public void setOutputStream(final OutputStream outputStream) {
        out = new PrintWriter(outputStream, true);
    }

    /**
     * Is output queueing enabled?
     *
     * @return true if output queueing is enabled.
     */
    public boolean isQueueEnabled() {
        return queueEnabled;
    }

    /**
     * Set if queueing is enabled.
     * if this is changed from enabled to disabled, all currently queued items
     * will be sent immediately!
     *
     * @param queueEnabled new value for queueEnabled
     */
    public void setQueueEnabled(final boolean queueEnabled) {
        if (out == null) {
            throw new NullPointerException("No output stream has been set.");
        }

        final boolean old = this.queueEnabled;
        this.queueEnabled = queueEnabled;

        // If the new value is not the same as the old one, and we used to be enabled
        // then flush the queue.
        if (old != queueEnabled && old) {
            if (sendingThread != null) {
                sendingThread.interrupt();
                sendingThread = null;
            }

            while (!queue.isEmpty()) {
                try {
                    out.printf("%s\r\n", queue.take().getLine());
                } catch (InterruptedException ex) {
                    // Do nothing, we'll try again.
                }
            }
        }
    }

    /**
     * Direct access to the queue of items waiting to be sent.
     *
     * @return This queue's backing queue.
     */
    public BlockingQueue<QueueItem> getQueue() {
        return queue;
    }

    /**
     * Should we be discarding?
     *
     * @param newValue true to enable discarding.
     */
    public void setDiscarding(final boolean newValue) {
        discarding = newValue;
    }

    /**
     * Are we discarding?
     *
     * @return true if discarding
     */
    public boolean isDiscarding() {
        return discarding;
    }

    /**
     * Clears any pending items.
     */
    public void clearQueue() {
        queueEnabled = false;
        if (sendingThread != null) {
            sendingThread.interrupt();
            sendingThread = null;
        }
        queue.clear();
    }

    /**
     * Get the number of items currently in the queue.
     *
     * @return Number of items in the queue.
     */
    public int queueCount() {
        return queue.size();
    }

    /**
     * Send the given line.
     *
     * <p>If queueing is enabled, this will queue it, else it will send it immediately.
     *
     * @param line Line to send
     */
    public void sendLine(final String line) {
        sendLine(line, QueuePriority.NORMAL);
    }

    /**
     * Send the given line.
     *
     * <p>If queueing is enabled, this will queue it, else it will send it immediately.
     *
     * @param line Line to send
     * @param priority Priority of item (ignored if queue is disabled)
     */
    public void sendLine(final String line, final QueuePriority priority) {
        if (discarding) {
            return;
        }

        if (queueEnabled && priority == QueuePriority.IMMEDIATE) {
            send(line);
        } else {
            if (sendingThread == null || !sendingThread.isAlive()) {
                sendingThread = new Thread(this::handleQueuedItems, "IRC Parser queue handler");
                sendingThread.start();
            }

            enqueue(line, priority);
        }
    }

    /**
     * Sends queued items to the output channel, blocking or waiting as necessary.
     *
     * <p>This is a long running method that will be executed in a separate thread.
     */
    protected abstract void handleQueuedItems();

    /**
     * Enqueues a new line to be sent.
     *
     * @param line The raw line to be sent to the server.
     * @param priority The priority at which the line should be sent.
     */
    protected void enqueue(final String line, final QueuePriority priority) {
        queue.add(QueueItem.create(line, priority));
    }

    /**
     * Sends a line immediately to the server.
     *
     * @param line The line to be sent.
     */
    protected void send(final String line) {
        if (out == null) {
            throw new IllegalStateException("No output stream has been set.");
        }

        out.printf("%s\r\n", line);
    }


}
