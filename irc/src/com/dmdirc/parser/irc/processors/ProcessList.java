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

package com.dmdirc.parser.irc.processors;

import com.dmdirc.parser.events.GroupListEndEvent;
import com.dmdirc.parser.events.GroupListEntryEvent;
import com.dmdirc.parser.events.GroupListStartEvent;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.ProcessingManager;

import java.util.Date;

/**
 * Process a list response.
 */
public class ProcessList extends IRCProcessor {

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param manager ProcessingManager that is in charge of this IRCProcessor
     */
    public ProcessList(final IRCParser parser, final ProcessingManager manager) {
        super(parser, manager, "321", "322", "323");
    }

    /**
     * Process a list response.
     *
     * @param sParam Type of line to process
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String... token) {
        // :port80b.se.quakenet.org 321 MD87 Channel :Users  Name
        // :port80b.se.quakenet.org 322 MD87 #DMDirc 10 :
        // :port80b.se.quakenet.org 323 MD87 :End of /LIST
        switch (sParam) {
            case "321":
                getCallbackManager().publish(new GroupListStartEvent(parser, new Date()));
                break;
            case "322":
                getCallbackManager().publish(new GroupListEntryEvent(parser, new Date(), token[3],
                        Integer.parseInt(token[4]), token[5]));
                break;
            case "323":
                getCallbackManager().publish(new GroupListEndEvent(parser, new Date()));
                break;
        }
    }

}
