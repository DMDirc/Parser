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

package com.dmdirc.parser.events;

import com.dmdirc.parser.common.AwayState;
import com.dmdirc.parser.interfaces.Parser;

import java.time.LocalDateTime;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Called when we go away, or come back.
 */
public class AwayStateEvent extends ParserEvent {

    private final AwayState oldState;
    private final AwayState newState;
    private final String reason;

    public AwayStateEvent(final Parser parser, final LocalDateTime date, final AwayState oldState,
            final AwayState newState, @Nullable final String reason) {
        super(parser, date);
        this.oldState = checkNotNull(oldState);
        this.newState = checkNotNull(newState);
        this.reason = reason;
    }

    public AwayState getOldState() {
        return oldState;
    }

    public AwayState getNewState() {
        return newState;
    }

    @Nullable
    public String getReason() {
        return reason;
    }
}
