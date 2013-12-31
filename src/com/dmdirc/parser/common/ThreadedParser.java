/*
 * Copyright (c) 2006-2014 DMDirc Developers
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

package com.dmdirc.parser.common;

import com.dmdirc.parser.interfaces.Parser;

/**
 * This class represents a Parser that runs inside a thread (which should be
 * most parsers!)
 */
public abstract class ThreadedParser implements Parser {

    /** Parser Control Thread. */
    protected Thread controlThread;

    /** {@inheritDoc} */
    @Override
    public void connect() {
        synchronized (this) {
            if (controlThread != null) {
                // To ensure correct internal state, parsers must be recreated for
                // new connections rather than being recycled.
                throw new UnsupportedOperationException("This parser has already been running.");
            } else {
                controlThread = new Thread(new Runnable(){
                    /** {@inheritDoc} */
                    @Override
                    public void run() {
                        ThreadedParser.this.run();
                    }
                }, "Parser Thread");
                controlThread.start();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void disconnect(final String message) {
        synchronized (this) {
            if (controlThread != null) {
                controlThread.interrupt();
            }
        }
    }

    /**
     * Get the control thread instance if one exists.
     *
     * @return controlThread for this parser.
     */
    public Thread getControlThread() {
        return controlThread;
    }

    /**
     * Entry point for the control thread for this parser.
     */
    protected abstract void run();
}
