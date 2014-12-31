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

import com.dmdirc.parser.irc.CapabilityState;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.ProcessingManager;
import com.dmdirc.parser.irc.TimestampedIRCProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Process CAP extension.
 * There are currently no callbacks related to this, as it is just to tell
 * the server what we support.
 *
 * For all the capabilities we support, we are always able to handle lines#
 * either with or without the capability enabled, so we don't actually need to
 * keep much/any state here, which makes this easy.
 *
 * See: http://www.leeh.co.uk/draft-mitchell-irc-capabilities-02.html
 * See: http://ircv3.atheme.org/specification/capability-negotiation-3.1
 */
public class ProcessCap extends TimestampedIRCProcessor {
    /** Have we handled the pre-connect cap request? */
    private boolean hasCapped;
    /** List of supported capabilities. */
    private final Collection<String> supportedCapabilities = new ArrayList<>();

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param manager ProcessingManager that is in charge of this IRCProcessor
     */
    public ProcessCap(final IRCParser parser, final ProcessingManager manager) {
        super(parser, manager, "CAP");

        // IRCv3.1 Standard
        supportedCapabilities.add("multi-prefix");
        supportedCapabilities.add("userhost-in-names");
        supportedCapabilities.add("away-notify");
        supportedCapabilities.add("account-notify");
        supportedCapabilities.add("extended-join");
        supportedCapabilities.add("self-message");

        // Freenode
        // supportedCapabilities.add("identify-msg");

        // DFBnc
        supportedCapabilities.add("dfbnc.com/tsirc");
    }

    /**
     * Process CAP responses.
     *
     * @param sParam Type of line to process ("CAP")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final Date date, final String sParam, final String... token) {
        // We will only automatically handle the first ever pre-001 CAP LS
        // response.
        // After that, the user may be sending stuff themselves so we do
        // nothing.
        if (!hasCapped && !parser.got001 && token.length > 4 && "LS".equalsIgnoreCase(token[3])) {
            final String[] caps = token[token.length - 1].split(" ");
            for (final String cap : caps) {
                if (cap.isEmpty()) { continue; }
                final String capability = cap.charAt(0) == '=' ? cap.substring(1).toLowerCase() : cap.toLowerCase();

                parser.addCapability(capability);

                if (supportedCapabilities.contains(capability)) {
                    // Send cap requests as individual lines, as some servers
                    // only appear to accept them one at a time.
                    parser.sendRawMessage("CAP REQ :" + capability);
                }
            }

            // If this is the last of the LS responses, set hasCapped to true
            // so that we don't try this again, and send "CAP END"
            // We will accept any of the following to be the end of the list:
            //     :DFBnc.Server CAP Dataforce LS :some caps
            //     :DFBnc.Server CAP Dataforce LS
            // but not:
            //     :DFBnc.Server CAP Dataforce LS *
            if (token.length == 4 || token.length == 5 && !"*".equals(token[4])) {
                hasCapped = true;
                parser.sendRawMessage("CAP END");
            }
        } else if ("ACK".equalsIgnoreCase(token[3]) || "CLEAR".equalsIgnoreCase(token[3])) {
            // Process the list.
            final String[] caps = token[token.length - 1].split(" ");
            for (final String cap : caps) {
                if (cap.isEmpty()) { continue; }
                final CapabilityState modifier = CapabilityState.fromModifier(cap.charAt(0));

                final String capability = modifier == CapabilityState.INVALID ? cap.toLowerCase() : cap.substring(1).toLowerCase();
                final CapabilityState state = modifier == CapabilityState.INVALID ? CapabilityState.ENABLED : modifier;

                if (state == CapabilityState.NEED_ACK) {
                    parser.sendRawMessage("CAP ACK :" + capability);
                    parser.setCapabilityState(capability, CapabilityState.ENABLED);
                } else {
                    parser.setCapabilityState(capability, state);
                }
            }
        }
    }

}
