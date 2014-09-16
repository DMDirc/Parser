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

package com.dmdirc.parser.irc.processors;

import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.common.QueuePriority;
import com.dmdirc.parser.interfaces.callbacks.NetworkDetectedListener;
import com.dmdirc.parser.irc.CapabilityState;
import com.dmdirc.parser.irc.IRCEncoding;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.ProcessingManager;
import com.dmdirc.parser.irc.ProcessorNotFoundException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Process ISUPPORT lines.
 */
public class Process004005 extends IRCProcessor {

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param manager ProcessingManager that is in charge of this IRCProcessor
     */
    public Process004005(final IRCParser parser, final ProcessingManager manager) {
        super(parser, manager);
    }

    /**
     * Process ISUPPORT lines.
     *
     * @param sParam Type of line to process ("005", "004")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String[] token) {
        switch (sParam) {
            case "002":
                process002();
                break;
            case "003":
                process003(token);
                break;
            case "004":
                process004(token);
                break;
            case "005":
                process005(token);
                break;
        }
    }

    /**
     * What does this IRCProcessor handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"002", "003", "004", "005"};
    }

    /**
     * Callback to all objects implementing the GotNetwork Callback.
     * This takes no params of its own, but works them out itself.
     *
     * @see NetworkDetectedListener
     */
    private void callGotNetwork() {
        final String networkName = parser.networkName;
        final String ircdVersion = parser.getServerSoftware();
        final String ircdType = parser.getServerSoftwareType();

        getCallbackManager().getCallbackType(NetworkDetectedListener.class)
                .call(networkName, ircdVersion, ircdType);
    }

    /**
     * Processes a 002 line.
     */
    private void process002() {
        final Pattern pattern = Pattern.compile("running(?: version)? (.*)$");
        final Matcher matcher = pattern.matcher(parser.getLastLine());
        if (matcher.find()) {
            parser.h005Info.put("002IRCD", matcher.group(1));
        }
    }

    /**
     * Processes a 003 line.
     *
     * @param token The tokenised line.
     */
    private void process003(final String[] token) {
        parser.h005Info.put("003IRCD", token[token.length - 1]);
    }

    /**
     * Processes a 004 line.
     *
     * @param token The tokenised line.
     */
    private void process004(final String[] token) {
        final boolean multiParam = token.length > 4;
        int i = multiParam ? 4 : 1;
        final String[] bits = multiParam ? token : token[3].split(" ");

        if (bits.length > i) {
            parser.h005Info.put("004IRCD", bits[i]);
            i++;
        }

        if (bits.length > i && bits[i].matches("^\\d+$")) {
            // some IRCDs put a timestamp where the usermodes should be
            // (issues 4140. 4181 and 4183) so check to see if this is
            // numeric only, and if so, skip it.
            i++;
        }

        if (bits.length > i + 1) {
            parser.h005Info.put("USERMODES", bits[i]);
            parser.h005Info.put("USERCHANMODES", bits[i + 1]);
            i += 2;
        }

        if (bits.length > i) {
            // INSPIRCD includes an extra param
            parser.h005Info.put("USERCHANPARAMMODES", bits[i]);
        }

        parser.parseUserModes();
    }

    /**
     * Processes a 005 line.
     *
     * @param token The tokenised line.
     */
    private void process005(final String[] token) {
        for (int i = 3; i < token.length; i++) {
            final String[] bits = token[i].split("=", 2);

            if (bits[0].isEmpty()) {
                continue;
            }

            final boolean isNegation = bits[0].charAt(0) == '-';
            final String key = (isNegation ? bits[0].substring(1) : bits[0]).toUpperCase();
            final String value = bits.length == 2 ? bits[1] : "";

            callDebugInfo(IRCParser.DEBUG_INFO, "%s => %s", key, value);

            if (isNegation) {
                parser.h005Info.remove(key);
            } else {
                parser.h005Info.put(key, value);
            }

            switch (key) {
                case "NETWORK":
                    processNetworkToken(isNegation, value);
                    break;
                case "CASEMAPPING":
                    processCaseMappingToken(isNegation, value);
                    break;
                case "CHANTYPES":
                    processChanTypesToken(isNegation, value);
                    break;
                case "PREFIX":
                    processPrefixToken();
                    break;
                case "CHANMODES":
                    processChanModesToken();
                    break;
                case "NAMESX":
                    processNamesxToken(key);
                    break;
                case "UHNAMES":
                    processUhnamesToken(key);
                    break;
                case "LISTMODE":
                    processListmodeToken(value);
                    break;
                case "TIMESTAMPEDIRC":
                    processTimestampedIrcToken();
                    break;
            }
        }
    }

    /**
     * Processes a 'NETWORK' token received in a 005.
     *
     * @param isNegation Whether the token was negated or not.
     * @param value The value of the token.
     */
    private void processNetworkToken(final boolean isNegation, final String value) {
        if (!isNegation) {
            parser.networkName = value;
            callGotNetwork();
        }
    }

    /**
     * Processes a 'CASEMAPPING' token received in a 005.
     *
     * @param isNegation Whether the token was negated or not.
     * @param value The value of the token.
     */
    private void processCaseMappingToken(final boolean isNegation, final String value) {
        if (!isNegation) {
            IRCEncoding encoding = IRCEncoding.RFC1459;

            try {
                encoding = IRCEncoding.valueOf(value.toUpperCase().replace('-', '_'));
            } catch (IllegalArgumentException ex) {
                parser.callErrorInfo(new ParserError(ParserError.ERROR_WARNING,
                        "Unknown casemapping: '" + value + "' - assuming rfc1459",
                        parser.getLastLine()));
            }

            final boolean encodingChanged = parser.getStringConverter().getEncoding() != encoding;
            parser.setEncoding(encoding);

            if (encodingChanged && parser.knownClients() == 1) {
                // This means that the casemapping is not rfc1459
                // We have only added ourselves so far (from 001)
                // We can fix the hashtable easily.
                parser.removeClient(parser.getLocalClient());
                parser.addClient(parser.getLocalClient());
            }
        }
    }

    /**
     * Processes a 'CHANTYPES' token received in a 005.
     *
     * @param isNegation Whether the token was negated or not.
     * @param value The value of the token.
     */
    private void processChanTypesToken(final boolean isNegation, final String value) {
        if (isNegation) {
            parser.resetChanPrefix();
        } else {
            parser.setChanPrefix(value);
        }
    }

    /**
     * Processes a 'PREFIX' token received in a 005.
     */
    private void processPrefixToken() {
        parser.parsePrefixModes();
    }

    /**
     * Processes a 'CHANMODES' token received in a 005.
     */
    private void processChanModesToken() {
        parser.parseChanModes();
    }

    /**
     * Processes a 'NAMEX' token received in a 005.
     *
     * @param key The NAMEX token that was received.
     */
    private void processNamesxToken(final String key) {
        if (parser.getCapabilityState("multi-prefix") != CapabilityState.ENABLED) {
            parser.sendString("PROTOCTL " + key);
        }
    }

    /**
     * Processes a 'UHNAMES' token received in a 005.
     *
     * @param key The UHNAMES token that was received.
     */
    private void processUhnamesToken(final String key) {
        if (parser.getCapabilityState("userhost-in-names") != CapabilityState.ENABLED) {
            parser.sendString("PROTOCTL " + key);
        }
    }

    /**
     * Processes a 'LISTMODE' token received in a 005. The LISTMODE token
     * indicates support for a new way of describing list modes (such as +b).
     * See the proposal at http://shanemcc.co.uk/irc/#listmode.
     *
     * @param value The value of the token.
     */
    private void processListmodeToken(final String value) {
        final String[] handles = new String[2];
        handles[0] = value; // List mode item
        final String endValue = Integer.toString(Integer.parseInt(value) + 1);
        parser.h005Info.put("LISTMODEEND", endValue);
        handles[1] = endValue; // List mode end
        try {
            parser.getProcessingManager().addProcessor(handles,
                    parser.getProcessingManager().getProcessor("__LISTMODE__"));
        } catch (ProcessorNotFoundException e) {
        }
    }

    /**
     * Processes a 'TIMESTAMPEDIRC' token received in a 005. The TIMESTAMPEDIRC
     * protocol allows servers (or proxies) to send historical events with a
     * corresponding timestamp, allowing the client to catch up on events that
     * happened prior to them connecting. See the proposal at
     * http://shanemcc.co.uk/irc/#timestamping.
     */
    private void processTimestampedIrcToken() {
        if (parser.getCapabilityState("dfbnc.com/tsirc") != CapabilityState.ENABLED) {
            parser.sendString("TIMESTAMPEDIRC ON", QueuePriority.HIGH);
        }
    }
}
