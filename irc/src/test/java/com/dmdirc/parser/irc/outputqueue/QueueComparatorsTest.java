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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QueueComparatorsTest {

    private static final Clock NOW = Clock.fixed(Instant.ofEpochMilli(100 * 1000),
            ZoneId.systemDefault());

    private static final Clock NOW_MINUS_FIVE = Clock.fixed(Instant.ofEpochMilli(95 * 1000),
            ZoneId.systemDefault());

    private static final Clock NOW_MINUS_TEN = Clock.fixed(Instant.ofEpochMilli(90 * 1000),
            ZoneId.systemDefault());

    private final QueueItem currentLowPriority1 = QueueItem.create(
            NOW_MINUS_FIVE, "", QueuePriority.LOW);

    private final QueueItem currentLowPriority2 = QueueItem.create(
            NOW_MINUS_FIVE, "", QueuePriority.LOW);

    private final QueueItem currentNormalPriority = QueueItem.create(
            NOW_MINUS_FIVE, "", QueuePriority.NORMAL);

    private final QueueItem currentHighPriority = QueueItem.create(
            NOW_MINUS_FIVE, "", QueuePriority.HIGH);

    private final QueueItem oldLowPriority = QueueItem.create(
            NOW_MINUS_TEN, "", QueuePriority.LOW);

    @Test
    public void testComparesByPriority() {
        assertEquals(0, QueueComparators.byPriorityThenNumber().compare(
                currentLowPriority1,
                currentLowPriority1));
        assertTrue(QueueComparators.byPriorityThenNumber().compare(
                currentNormalPriority,
                currentLowPriority2) < 0);
        assertTrue(QueueComparators.byPriorityThenNumber().compare(
                currentHighPriority,
                oldLowPriority) < 0);
        assertTrue(QueueComparators.byPriorityThenNumber().compare(
                currentNormalPriority,
                currentHighPriority) > 0);
    }

    @Test
    public void testComparesByNumber() {
        // If the priority is the same, then the lowest number comes first.
        assertTrue(QueueComparators.byPriorityThenNumber().compare(
                currentLowPriority1,
                currentLowPriority2) < 0);
        assertTrue(QueueComparators.byPriorityThenNumber().compare(
                currentLowPriority2,
                currentLowPriority1) > 0);
    }

    @Test
    public void testStarvedItemsFirst() {
        // If an item is starved then it gets queued first
        assertTrue(QueueComparators.byPriorityThenNumber(NOW, Duration.ofSeconds(7)).compare(
                oldLowPriority,
                currentHighPriority) < 0);
        assertTrue(QueueComparators.byPriorityThenNumber(NOW, Duration.ofSeconds(7)).compare(
                currentLowPriority1,
                oldLowPriority) > 0);
    }

}