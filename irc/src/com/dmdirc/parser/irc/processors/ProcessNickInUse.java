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

import com.dmdirc.parser.interfaces.callbacks.NickInUseListener;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.ProcessingManager;

import java.util.Date;

/**
 * Process a NickInUse message.
 * Parser implements handling of this if Pre-001 and no other handler found,
 * adding the NickInUse handler (addNickInUse) after 001 is prefered over before.<br><br>
 * <br>
 * If the first nickname is in use, and a NickInUse message is recieved before 001, we
 * will attempt to use the altnickname instead.<br>
 * If this also fails, we will start prepending _ (or the value of me.cPrepend) to the main nickname.
 */
public class ProcessNickInUse extends IRCProcessor {

    /**
     * Create a new instance of the ProcessNickInUse Object.
     *
     * @param parser IRCParser That owns this object
     * @param manager ProcessingManager that is in charge of this object
     */
    public ProcessNickInUse(final IRCParser parser, final ProcessingManager manager) {
        super(parser, manager, "433");
    }

    /**
     * Process a NickInUse message.
     * Parser implements handling of this if Pre-001 and no other handler found,
     * adding the NickInUse handler (addNickInUse) after 001 is prefered over before.<br><br>
     * <br>
     * If the first nickname is in use, and a NickInUse message is recieved before 001, we
     * will attempt to use the altnickname instead.<br>
     * If this also fails, we will start prepending _ (or the value of me.cPrepend) to the main nickname.
     *
     * @param sParam Type of line to process ("433")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String... token) {
        callNickInUse(token[3]);
    }

    /**
     * Callback to all objects implementing the NickInUse Callback.
     *
     * @param nickname Nickname that was wanted.
     * @see NickInUseListener
     */
    protected void callNickInUse(final String nickname) {
        getCallback(NickInUseListener.class)
                .onNickInUse(parser, new Date(), nickname);
    }

}
