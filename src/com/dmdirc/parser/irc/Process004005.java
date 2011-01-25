/*
 * Copyright (c) 2006-2011 Chris Smith, Shane Mc Cormack, Gregory Holmes
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

import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.interfaces.callbacks.NetworkDetectedListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Process ISUPPORT lines.
 */
public class Process004005 extends IRCProcessor {

    /**
     * Process ISUPPORT lines.
     *
     * @param sParam Type of line to process ("005", "004")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String[] token) {
        if ("002".equals(sParam)) {
            final Pattern pattern = Pattern.compile("running(?: version)? (.*)$");
            final Matcher matcher = pattern.matcher(myParser.getLastLine());
            if (matcher.find()) {
                myParser.h005Info.put("002IRCD", matcher.group(1));
            }
        } else if ("003".equals(sParam)) {
            myParser.h005Info.put("003IRCD", token[token.length - 1]);
        } else if ("004".equals(sParam)) {
            // 004
            final boolean multiParam = token.length > 4;
            int i = multiParam ? 4 : 1;
            final String[] bits = multiParam ? token : token[3].split(" ");

            myParser.h005Info.put("004IRCD", bits[i++]);

            if (bits[i].matches("^\\d+$")) {
                // some IRCDs put a timestamp where the usermodes should be
                // (issues 4140. 4181 and 4183) so check to see if this is
                // numeric only, and if so, skip it.
                i++;
            }

            myParser.h005Info.put("USERMODES", bits[i++]);
            myParser.h005Info.put("USERCHANMODES", bits[i++]);

            if (bits.length > i) {
                // INSPIRCD includes an extra param
                myParser.h005Info.put("USERCHANPARAMMODES", bits[i++]);
            }

            myParser.parseUserModes();
        } else if ("005".equals(sParam)) {
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
                    myParser.h005Info.remove(key);
                } else {
                    myParser.h005Info.put(key, value);
                }

                if ("NETWORK".equals(key) && !isNegation) {
                    myParser.networkName = value;
                    callGotNetwork();
                } else if ("CASEMAPPING".equals(key) && !isNegation) {
                    IRCEncoding encoding = IRCEncoding.RFC1459;

                    try {
                        encoding = IRCEncoding.valueOf(value.toUpperCase().replace('-', '_'));
                    } catch (IllegalArgumentException ex) {
                        myParser.callErrorInfo(new ParserError(
                                ParserError.ERROR_WARNING,
                                "Unknown casemapping: '" + value + "' - assuming rfc1459",
                                myParser.getLastLine()));
                    }

                    final boolean encodingChanged = myParser.getStringConverter().getEncoding() != encoding;
                    myParser.setEncoding(encoding);

                    if (encodingChanged && myParser.knownClients() == 1) {
                        // This means that the casemapping is not rfc1459
                        // We have only added ourselves so far (from 001)
                        // We can fix the hashtable easily.
                        myParser.removeClient(myParser.getLocalClient());
                        myParser.addClient(myParser.getLocalClient());
                    }
                } else if ("CHANTYPES".equals(key)) {
                    myParser.parseChanPrefix();
                } else if ("PREFIX".equals(key)) {
                    myParser.parsePrefixModes();
                } else if ("CHANMODES".equals(key)) {
                    myParser.parseChanModes();
                } else if ("NAMESX".equals(key) || "UHNAMES".equals(key)) {
                    myParser.sendString("PROTOCTL " + key);
                } else if ("LISTMODE".equals(key)) {
                    // Support for potential future decent mode listing in the protocol
                    //
                    // See my proposal: http://shanemcc.co.uk/irc/#listmode
                    // Add listmode handler
                    String[] handles = new String[2];
                    handles[0] = value; // List mode item
                    final String endValue = "" + (Integer.parseInt(value) + 1);
                    myParser.h005Info.put("LISTMODEEND", endValue);
                    handles[1] = endValue; // List mode end
                    // Add listmode handlers
                    try {
                        myParser.getProcessingManager().addProcessor(handles, myParser.getProcessingManager().getProcessor("__LISTMODE__"));
                    } catch (ProcessorNotFoundException e) { }
                }
            }
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
     * @see IGotNetwork
     * @return true if a method was called, false otherwise
     */
    protected boolean callGotNetwork() {
        final String networkName = myParser.networkName;
        final String ircdVersion = myParser.getServerSoftware();
        final String ircdType = myParser.getServerSoftwareType();

        return getCallbackManager().getCallbackType(NetworkDetectedListener.class).call(networkName, ircdVersion, ircdType);
    }

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param manager ProcessingManager that is in charge of this IRCProcessor
     */
    protected Process004005(final IRCParser parser, final ProcessingManager manager) {
        super(parser, manager);
    }

}
