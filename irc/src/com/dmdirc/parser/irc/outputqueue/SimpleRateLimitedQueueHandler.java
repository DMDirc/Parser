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

/**
 * This is a simple rate limiting queue.
 * If more than 4 items are added in 4 seconds it will start limiting.
 * The first 4 items will be sent un-limited and then limiting will commence at
 * a rate of 1 per second.
 */
public class SimpleRateLimitedQueueHandler extends QueueHandler {

    /** Current count. */
    private int count;
    /** Time last item was added. */
    private long lastItemTime;
    /** Are we limiting? */
    private boolean isLimiting;
    /** How many items are allowed before limiting? */
    private int items = 4;
    /** How many microseconds do we care about when checking for items? */
    private int limitTime = 4000;
    /** How long to wait in between each item when limiting? */
    private int waitTime = 3000;
    /** Always update the lastItemTime or only if its been > limitTime? */
    private boolean alwaysUpdateTime = true;

    /**
     * Create a new SimpleRateLimitedQueueHandler.
     *
     * @param outputQueue Owner of this Queue Handler
     * @param out Output Stream to use
     */
    public SimpleRateLimitedQueueHandler(
            final OutputQueue outputQueue,
            final PrintWriter out) {
        super(outputQueue, QueueComparators.byPriorityThenNumber(), out);
    }

    /**
     * Get a QueueHandlerFactory that produces PriorityQueueHandlers.
     *
     * @return a QueueHandlerFactory that produces PrirortyQueueHandlers.
     */
    public static QueueHandlerFactory getFactory() {
        return SimpleRateLimitedQueueHandler::new;
    }

    /**
     * Get the number of items needed to activate rate limiting.
     *
     * @return Number of items needed to activate rate limiting.
     */
    public int getItems() {
        return items;
    }

    /**
     * Set the number of items needed to activate rate limiting.
     *
     * @param items Number of items needed to activate rate limiting.
     */
    public void setItems(final int items) {
        this.items = items;
    }

    /**
     * Get the length of time that is used when checking for rate limiting. (If
     * more than getItems() number of lines are added less that this time apart
     * from each other then rate limiting is activated.)
     *
     * @return Number of items needed to activate rate limiting.
     */
    public int getLimitTime() {
        return limitTime;
    }

    /**
     * Set the length of time that is used when checking for rate limiting. (If
     * more than getItems() number of lines are added less that this time apart
     * from each other then rate limiting is activated.)
     *
     * @param limitTime Number of items needed to activate rate limiting.
     */
    public void setLimitTime(final int limitTime) {
        this.limitTime = limitTime;
    }

    /**
     * Get the length of time that we wait inbetween lines when limiting.
     *
     * @return length of time that we wait inbetween lines when limiting.
     */
    public int getWaitTime() {
        return waitTime;
    }

    /**
     * Set the length of time that we wait inbetween lines when limiting.
     *
     * @param waitTime length of time that we wait inbetween lines when limiting.
     */
    public void setWaitTime(final int waitTime) {
        this.waitTime = waitTime;
    }

    /**
     * Will the internal "lastItemTime" be updated every time an item is added,
     * or only after limitTime has passed?
     *
     * If true, assuming the default settings) items sent at 0, 3, 6, 9 will
     * activate rate limiting, if false it would need to be 0, 1, 2, 3.
     *
     * @return is LastItemTime always updated?
     */
    public boolean getAlwaysUpdateTime() {
        return alwaysUpdateTime;
    }

    /**
     * Set if the internal "lastItemTime" should be updated every time an item
     * is added, or only after limitTime has passed?
     *
     * If true, assuming the default settings) items sent at 0, 3, 6, 9 will
     * activate rate limiting, if false it would need to be 0, 1, 2, 3.
     *
     * @param alwaysUpdateTime Should LastItemTime always updated?
     */
    public void setAlwaysUpdateTime(final boolean alwaysUpdateTime) {
        this.alwaysUpdateTime = alwaysUpdateTime;
    }

    /**
     * Are we currently limiting?
     *
     * @return True if limiting is active.
     */
    public boolean isLimiting() {
        return isLimiting;
    }

    @Override
    public QueueItem getQueueItem(final String line, final QueuePriority priority) {
        // Was the last line added less than limitTime ago?
        synchronized (this) {
            final boolean overTime = lastItemTime + limitTime > System.currentTimeMillis();
            if (overTime) {
                // If we are not currently limiting, and this is the items-th item
                // added in the last limitTime, start limiting.
                if (!isLimiting && ++count >= items) {
                    isLimiting = true;
                    count = 0;
                }
            } else if (isLimiting) {
                // It has been longer than limitTime and we are still shown as
                // limiting, check to see if the queue is empty or not, if it is
                // disable limiting.
                if (outputQueue.getQueue().isEmpty()) {
                    isLimiting = false;
                }
            } else {
                // If it has been more than limitTime seconds since the last line
                // and we are not currently limiting, reset the count.
                count = 0;
            }

            if (alwaysUpdateTime || overTime) {
                lastItemTime = System.currentTimeMillis();
            }
        }

        return super.getQueueItem(line, priority);
    }

    @Override
    public void run() {
        try {
            while (outputQueue.isQueueEnabled()) {
                final QueueItem item = outputQueue.getQueue().take();

                sendLine(item.getLine());

                final boolean doSleep;
                synchronized (this) {
                    doSleep = isLimiting;
                    if (isLimiting && outputQueue.getQueue().isEmpty()) {
                        isLimiting = false;
                    }
                }

                if (doSleep) {
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ex) { /* Do Nothing. */ }
                }
            }
        } catch (InterruptedException ex) {
            // Do nothing
        }
    }
}
