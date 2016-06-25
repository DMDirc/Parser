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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * This class handles the Parser output Queue.
 */
public class OutputQueue {

    /** PrintWriter for sending output. */
    private PrintWriter out;
    /** Is queueing enabled? */
    private boolean queueEnabled = true;
    /** Are we discarding all futher input? */
    private boolean discarding = false;
    /** The output queue! */
    private final BlockingQueue<QueueItem> queue = new PriorityBlockingQueue<>();
    /** Object for synchronising access to the {@link #queueHandler}. */
    private final Object queueHandlerLock = new Object();
    /** Thread for the sending queue. */
    private QueueHandler queueHandler;
    /** The QueueHandlerFactory for this OutputQueue. */
    private QueueHandlerFactory queueHandlerFactory = PriorityQueueHandler.getFactory();

    /**
     * Set the output stream for this queue.
     *
     * @param outputStream Output Stream to use.
     */
    public void setOutputStream(final OutputStream outputStream) {
        this.out = new PrintWriter(outputStream, true);
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
     * Set the QueueHandlerFactory.
     * Changing this will not change an existing QueueHandler unless queueing is
     * disabled and reenabled.
     * If this is called before the first lien of output is queued then there is
     * no need to disable and reenable the queue.
     *
     * @param factory New QueueHandlerFactory to use.
     */
    public void setQueueHandlerFactory(final QueueHandlerFactory factory) {
        queueHandlerFactory = factory;
    }

    /**
     * Get the QueueHandlerFactory.
     *
     * @return The current QueueHandlerFactory.
     */
    public QueueHandlerFactory getQueueHandlerFactory() {
        return queueHandlerFactory;
    }

    /**
     * Get the QueueHandler.
     *
     * @return The current QueueHandler if there is one, else null.
     */
    public QueueHandler getQueueHandler() {
        return queueHandler;
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
            synchronized (queueHandlerLock) {
                if (queueHandler != null) {
                    queueHandler.interrupt();
                    queueHandler = null;
                }
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
     * Clear the queue and stop the thread that is sending stuff.
     */
    public void clearQueue() {
        this.queueEnabled = false;

        synchronized (queueHandlerLock) {
            if (queueHandler != null) {
                queueHandler.interrupt();
                queueHandler = null;
            }
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
     * If queueing is enabled, this will queue it, else it will send it
     * immediately.
     *
     * @param line Line to send
     */
    public void sendLine(final String line) {
        sendLine(line, QueuePriority.NORMAL);
    }

    /**
     * Send the given line.
     * If queueing is enabled, this will queue it, else it will send it
     * immediately.
     *
     * @param line Line to send
     * @param priority Priority of item (ignored if queue is disabled)
     */
    public void sendLine(final String line, final QueuePriority priority) {
        if (discarding) { return; }
        if (out == null) {
            throw new NullPointerException("No output stream has been set.");
        }

        if (queueEnabled && priority != QueuePriority.IMMEDIATE) {
            synchronized (queueHandlerLock) {
                if (queueHandler == null || !queueHandler.isAlive()) {
                    queueHandler = queueHandlerFactory.getQueueHandler(this, out);
                    queueHandler.start();
                }

                queue.add(queueHandler.getQueueItem(line, priority));
            }
        } else {
            out.printf("%s\r\n", line);
        }
    }
}
