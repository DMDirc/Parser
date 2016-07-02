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

import com.google.auto.value.AutoValue;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Queued Item.
 */
@AutoValue
public abstract class QueueItem {

    /** Static counter to assign IDs to items. */
    // TODO: Move this into OutputQueue or somewhere not static.
    private static final AtomicLong itemCounter = new AtomicLong(0);

    /**
     * Get the line that is to be sent to the server.
     *
     * @return the line to be sent.
     */
    public abstract String getLine();

    /**
     * Gets the time at which this item was queued.
     *
     * @return the time this item was queued.
     */
    public abstract LocalDateTime getTime();

    /**
     * Get the number of this item.
     *
     * @return the number of this item (representing the order it was queued in).
     */
    public abstract long getItemNumber();

    /**
     * Get the priority of this item.
     *
     * @return the item priority
     */
    public abstract QueuePriority getPriority();

    @Override
    public String toString() {
        return String.format("[%s %s] %s", getPriority(), getTime(), getLine());
    }

    /**
     * Creates a new {@link QueueItem}.
     *
     * @param line The line to be sent to the server.
     * @param priority The priority of the line.
     * @return A new item to be submitted to the queue.
     */
    public static QueueItem create(final String line, final QueuePriority priority) {
        return create(Clock.systemDefaultZone(), line, priority);
    }

    /**
     * Creates a new {@link QueueItem}.
     *
     * @param clock The clock to use to get the current time.
     * @param line The line to be sent to the server.
     * @param priority The priority of the line.
     * @return A new item to be submitted to the queue.
     */
    public static QueueItem create(
            final Clock clock, final String line, final QueuePriority priority) {
        return new AutoValue_QueueItem(line, LocalDateTime.now(clock),
                itemCounter.getAndIncrement(), priority);
    }

}
