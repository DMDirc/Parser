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

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Comparator;

/**
 * Provides comparators for comparing {@link QueueItem}s.
 */
public final class QueueComparators {

    private QueueComparators() {
        // Shouldn't be used
    }

    /**
     * Provides a comparator that checks priority and item number.
     *
     * @return a comparator that compares items by their priority, and then their item number.
     */
    public static Comparator<QueueItem> byPriorityThenNumber() {
        return Comparator.comparing(QueueItem::getPriority)
                .thenComparing(QueueItem::getItemNumber);
    }

    /**
     * Provides a comparator that checks priority and item number, with special handling for
     * starved items.
     *
     * <p>If an item has been queued for longer than {@code starvationThreshold}, its priority
     * will be effectively ignored (i.e., it will just be compared on item number). This ensures
     * that lower priority items are not kept in the queue indefinitely if there is a constant
     * stream of higher priority items.
     *
     * @param starvationThreshold The time an item must be queued for to be considered starved.
     * @return a comparator that compares items by their priority unless they are starved, and then
     * their item number.
     */
    public static Comparator<QueueItem> byPriorityThenNumber(
            final TemporalAmount starvationThreshold) {
        return byPriorityThenNumber(Clock.systemDefaultZone(), starvationThreshold);
    }

    /**
     * Provides a comparator that checks priority and item number, with special handling for
     * starved items.
     *
     * <p>If an item has been queued for longer than {@code starvationThreshold}, its priority
     * will be effectively ignored (i.e., it will just be compared on item number). This ensures
     * that lower priority items are not kept in the queue indefinitely if there is a constant
     * stream of higher priority items.
     *
     * @param clock The clock to use to get the current time.
     * @param starvationThreshold The time an item must be queued for to be considered starved.
     * @return a comparator that compares items by their priority unless they are starved, and then
     * their item number.
     */
    public static Comparator<QueueItem> byPriorityThenNumber(
            final Clock clock,
            final TemporalAmount starvationThreshold) {
        return Comparator.<QueueItem, QueuePriority>comparing(
                item -> getPriorityIfNotStarved(item, clock, starvationThreshold))
                .thenComparing(QueueItem::getItemNumber);
    }

    /**
     * Gets the priority of the item if it was created within the given timespan, or
     * {@link QueuePriority#IMMEDIATE} if the item is older. This allows items that are otherwise
     * being starved to be sorted to the front of a queue.
     *
     * @param item The item to get the priority of.
     * @param clock The clock to use to get the current time.
     * @param timespan The timespan after which an item will be considered starved.
     * @return The priority of the item, or {@link QueuePriority#IMMEDIATE}.
     */
    private static QueuePriority getPriorityIfNotStarved(
            final QueueItem item, final Clock clock, final TemporalAmount timespan) {
        if (item.getTime().isAfter(LocalDateTime.now(clock).minus(timespan))) {
            return item.getPriority();
        } else {
            return QueuePriority.IMMEDIATE;
        }
    }

}
