/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dmdirc.parser.irc.outputqueue;

import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;

/**
 * A QueueFactory produces QueueHandlers for OutputQueue.
 *
 * @author shane
 */
public interface QueueFactory {

    /**
     * Get a new QueueHandler instance as needed.
     *
     * @param outputQueue the OutputQueue that will own this QueueHandler
     * @param queue The queue to handle.
     * @param out Where to send crap.
     * @return the new queue handler object.
     */
    QueueHandler getQueueHandler(final OutputQueue outputQueue,
            final BlockingQueue<QueueItem> queue, final PrintWriter out);

}
