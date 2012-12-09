/*
 * Copyright (c) 2006-2012 DMDirc Developers
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

package com.dmdirc.parser.irc;

import com.dmdirc.parser.common.AwayState;
import com.dmdirc.parser.interfaces.callbacks.AwayStateListener;

/**
 * Process an Away/Back message.
 */
public class ProcessAway extends IRCProcessor {

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param manager ProcessingManager that is in charge of this IRCProcessor
     */
    protected ProcessAway(final IRCParser parser, final ProcessingManager manager) {
        super(parser, manager);
    }

    /**
     * Process an Away/Back message.
     *
     * @param sParam Type of line to process ("305", "306" etc)
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String[] token) {
        if ("AWAY".equals(sParam)) {
            final IRCClientInfo iClient = getClientInfo(token[0]);
            if (iClient != null) {
                final AwayState oldState = iClient.getAwayState();

                final String reason = (token.length > 2) ? token[token.length - 1] : "";
                iClient.setAwayReason(reason);
                iClient.setAwayState(reason.isEmpty() ? AwayState.HERE : AwayState.AWAY);

                if (iClient == parser.getLocalClient()) {
                    callAwayState(oldState, iClient.getAwayState(), iClient.getAwayReason());
                }
            }
        } else if ("301".equals(sParam)) { // WHO Response
            final IRCClientInfo iClient = getClientInfo(token[3]);
            if (iClient != null) {
                iClient.setAwayReason(token[token.length - 1]);
            }
        } else { // IRC HERE/BACK response
            final AwayState oldState = parser.getLocalClient().getAwayState();
            parser.getLocalClient().setAwayState("306".equals(sParam) ? AwayState.AWAY : AwayState.HERE);
            callAwayState(oldState, parser.getLocalClient().getAwayState(), parser.getLocalClient().getAwayReason());
        }
    }

    /**
     * Callback to all objects implementing the onAwayState Callback.
     *
     * @see IAwayState
     * @param oldState Old Away State
     * @param currentState Current Away State
     * @param reason Best guess at away reason
     * @return true if a method was called, false otherwise
     */
    protected boolean callAwayState(final AwayState oldState, final AwayState currentState, final String reason) {
        return getCallbackManager().getCallbackType(AwayStateListener.class).call(oldState, currentState, reason);
    }

    /**
     * What does this IRCProcessor handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"301", "305", "306", "AWAY"};
    }
}
