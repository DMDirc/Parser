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
import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.callbacks.ChannelActionListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelCtcpListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelCtcpReplyListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelMessageListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelModeMessageListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelModeNoticeListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelNoticeListener;
import com.dmdirc.parser.interfaces.callbacks.PrivateActionListener;
import com.dmdirc.parser.interfaces.callbacks.PrivateCtcpListener;
import com.dmdirc.parser.interfaces.callbacks.PrivateCtcpReplyListener;
import com.dmdirc.parser.interfaces.callbacks.PrivateMessageListener;
import com.dmdirc.parser.interfaces.callbacks.PrivateNoticeListener;
import com.dmdirc.parser.interfaces.callbacks.ServerNoticeListener;
import com.dmdirc.parser.interfaces.callbacks.UnknownActionListener;
import com.dmdirc.parser.interfaces.callbacks.UnknownCtcpListener;
import com.dmdirc.parser.interfaces.callbacks.UnknownCtcpReplyListener;
import com.dmdirc.parser.interfaces.callbacks.UnknownMessageListener;
import com.dmdirc.parser.interfaces.callbacks.UnknownNoticeListener;
import com.dmdirc.parser.interfaces.callbacks.UnknownServerNoticeListener;
import com.dmdirc.parser.irc.IRCChannelClientInfo;
import com.dmdirc.parser.irc.IRCChannelInfo;
import com.dmdirc.parser.irc.IRCClientInfo;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.PrefixModeManager;
import com.dmdirc.parser.irc.ProcessingManager;
import com.dmdirc.parser.irc.ProcessorNotFoundException;
import com.dmdirc.parser.irc.TimestampedIRCProcessor;

import java.util.Date;
import java.util.regex.PatternSyntaxException;

/**
 * Process PRIVMSGs and NOTICEs.
 * This horrible handles PRIVMSGs and NOTICE<br>
 * This inclues CTCPs and CTCPReplies<br>
 * It handles all 3 targets (Channel, Private, Unknown)<br>
 * Actions are handled here aswell separately from CTCPs.<br>
 * Each type has 5 Calls, making 15 callbacks handled here.
 */
public class ProcessMessage extends TimestampedIRCProcessor {

    /** The manager to use to access prefix modes. */
    private final PrefixModeManager prefixModeManager;

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param prefixModeManager The manager to use to access prefix modes.
     * @param manager ProcessingManager that is in charge of this IRCProcessor
     */
    public ProcessMessage(final IRCParser parser, final PrefixModeManager prefixModeManager,
            final ProcessingManager manager) {
        super(parser, manager, "PRIVMSG", "NOTICE");
        this.prefixModeManager = prefixModeManager;
    }

    /**
     * Process PRIVMSGs and NOTICEs.
     * This horrible thing handles PRIVMSGs and NOTICES<br>
     * This inclues CTCPs and CTCPReplies<br>
     * It handles all 3 targets (Channel, Private, Unknown)<br>
     * Actions are handled here aswell separately from CTCPs.<br>
     * Each type has 5 Calls, making 15 callbacks handled here.
     *
     * @param sParam Type of line to process ("NOTICE", "PRIVMSG")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final Date date, final String sParam, final String... token) {
        // Ignore people!
        String sMessage;
        if (token[0].charAt(0) == ':') {
            sMessage = token[0].substring(1);
        } else {
            sMessage = token[0];
        }
        // We use sMessage to be the users host (first token in the line)
        try {
            if (parser.getIgnoreList().matches(sMessage) > -1) {
                return;
            }
        } catch (PatternSyntaxException pse) {
            final ParserError pe = new ParserError(ParserError.ERROR_WARNING + ParserError.ERROR_USER, "Error with ignore list regex: " + pse, parser.getLastLine());
            pe.setException(pse);
            callErrorInfo(pe);
        }

        // Lines such as:
        // "nick!user@host PRIVMSG"
        // are invalid, stop processing.
        if (token.length < 3) {
            return;
        }

        // Is this actually a notice auth?
        if (token[0].indexOf('!') == -1 && "NOTICE".equalsIgnoreCase(token[1]) &&
                "AUTH".equalsIgnoreCase(token[2])) {
            try {
                parser.getProcessingManager().process(date, "Notice Auth", token);
            } catch (ProcessorNotFoundException e) {
            }
            return;
        }

        // "nick!user@host PRIVMSG #Channel" should be processed as "nick!user@host PRIVMSG #Channel :"
        if (token.length < 4) {
            sMessage = "";
        } else {
            sMessage = token[token.length - 1];
        }
        String[] bits = sMessage.split(" ", 2);
        String sCTCP = "";
        boolean isAction = false;
        boolean isCTCP = false;

        if (sMessage.length() > 1) {
            // Actions are special CTCPs
            // Bits is the message been split into 2 parts
            //         the first word and the rest
            final Character char1 = (char) 1;
            if ("PRIVMSG".equalsIgnoreCase(sParam) && bits[0].equalsIgnoreCase(char1 + "ACTION") && Character.valueOf(sMessage.charAt(sMessage.length() - 1)).equals(char1)) {
                isAction = true;
                if (bits.length > 1) {
                    sMessage = bits[1];
                    sMessage = sMessage.substring(0, sMessage.length() - 1);
                } else {
                    sMessage = "";
                }
            }
            // If the message is not an action, check if it is another type of CTCP
            // CTCPs have Character(1) at the start/end of the line
            if (!isAction && Character.valueOf(sMessage.charAt(0)).equals(char1) && Character.valueOf(sMessage.charAt(sMessage.length() - 1)).equals(char1)) {
                isCTCP = true;
                // Bits is the message been split into 2 parts, the first word and the rest
                // Some CTCPs have messages and some do not
                if (bits.length > 1) {
                    sMessage = bits[1];
                } else {
                    sMessage = "";
                }
                // Remove the leading char1
                bits = bits[0].split(char1.toString(), 2);
                sCTCP = bits[1];
                // remove the trailing char1
                if (sMessage.isEmpty()) {
                    sCTCP = sCTCP.split(char1.toString(), 2)[0];
                } else {
                    sMessage = sMessage.split(char1.toString(), 2)[0];
                }
                callDebugInfo(IRCParser.DEBUG_INFO, "CTCP: \"%s\" \"%s\"",
                        sCTCP, sMessage);
            }
        }

        // Remove the leading : from the host.
        final String firstToken;
        if (token[0].charAt(0) == ':' && token[0].length() > 1) {
            firstToken = token[0].substring(1);
        } else {
            firstToken = token[0];
        }

        final IRCClientInfo iClient = getClientInfo(token[0]);
        // Facilitate DMDIRC Formatter
        if (IRCParser.ALWAYS_UPDATECLIENT && iClient != null && iClient.getHostname().isEmpty()) {
            iClient.setUserBits(firstToken, false);
        }

        // Fire the appropriate callbacks.
        // OnChannel* Callbacks are fired if the target was a channel
        // OnPrivate* Callbacks are fired if the target was us
        // OnUnknown* Callbacks are fired if the target was neither of the above
        // Actions and CTCPs are send as PRIVMSGS
        // CTCPReplies are sent as Notices

        // Check if we have a Mode Prefix for channel targets.
        // Non-Channel messages still use the whole token, even if the first char
        // is a prefix.
        // CTCP and CTCPReplies that are aimed at a channel with a prefix are
        // handled as if the prefix wasn't used. This can be changed in the future
        // if desired.
        final char modePrefix = token[2].charAt(0);
        final boolean hasModePrefix = prefixModeManager.isPrefix(modePrefix);
        final String targetName = hasModePrefix ? token[2].substring(1) : token[2];

        if (isValidChannelName(targetName)) {
            final IRCChannelInfo iChannel = getChannel(targetName);
            if (iChannel == null) {
                // callErrorInfo(new ParserError(ParserError.ERROR_WARNING, "Got message for channel ("+targetName+") that I am not on.", parser.getLastLine()));
                return;
            }
            IRCChannelClientInfo iChannelClient = null;
            if (iClient != null) {
                iChannelClient = iChannel.getChannelClient(iClient);
            }
            if ("PRIVMSG".equalsIgnoreCase(sParam)) {
                if (isAction) {
                    callChannelAction(date, iChannel, iChannelClient, sMessage, firstToken);
                } else {
                    if (isCTCP) {
                        callChannelCTCP(date, iChannel, iChannelClient, sCTCP, sMessage, firstToken);
                    } else if (hasModePrefix) {
                        callChannelModeMessage(date, modePrefix, iChannel, iChannelClient, sMessage, firstToken);
                    } else {
                        callChannelMessage(date, iChannel, iChannelClient, sMessage, firstToken);
                    }
                }
            } else if ("NOTICE".equalsIgnoreCase(sParam)) {
                if (isCTCP) {
                    callChannelCTCPReply(date, iChannel, iChannelClient, sCTCP, sMessage, firstToken);
                } else if (hasModePrefix) {
                    callChannelModeNotice(date, modePrefix, iChannel, iChannelClient, sMessage, firstToken);
                } else {
                    callChannelNotice(date, iChannel, iChannelClient, sMessage, firstToken);
                }
            }
        } else if (parser.getStringConverter().equalsIgnoreCase(token[2], parser.getMyNickname())) {
            if ("PRIVMSG".equalsIgnoreCase(sParam)) {
                if (isAction) {
                    callPrivateAction(date, sMessage, firstToken);
                } else {
                    if (isCTCP) {
                        callPrivateCTCP(date, sCTCP, sMessage, firstToken);
                    } else {
                        callPrivateMessage(date, sMessage, firstToken);
                    }
                }
            } else if ("NOTICE".equalsIgnoreCase(sParam)) {
                if (isCTCP) {
                    callPrivateCTCPReply(date, sCTCP, sMessage, firstToken);
                } else {
                    if (firstToken.indexOf('@') == -1) {
                        callServerNotice(date, sMessage, firstToken);
                    } else {
                        callPrivateNotice(date, sMessage, firstToken);
                    }
                }
            }
        } else {
            callDebugInfo(IRCParser.DEBUG_INFO, "Message for Other (" + token[2] + ')');
            if ("PRIVMSG".equalsIgnoreCase(sParam)) {
                if (isAction) {
                    callUnknownAction(date, sMessage, token[2], firstToken);
                } else {
                    if (isCTCP) {
                        callUnknownCTCP(date, sCTCP, sMessage, token[2], firstToken);
                    } else {
                        callUnknownMessage(date, sMessage, token[2], firstToken);
                    }
                }
            } else if ("NOTICE".equalsIgnoreCase(sParam)) {
                if (isCTCP) {
                    callUnknownCTCPReply(date, sCTCP, sMessage, token[2], firstToken);
                } else {
                    if (firstToken.indexOf('@') == -1) {
                        callUnknownServerNotice(date, sMessage, token[2], firstToken);
                    } else {
                        callUnknownNotice(date, sMessage, token[2], firstToken);
                    }
                }
            }
        }
    }

    /**
     * Callback to all objects implementing the ChannelAction Callback.
     *
     * @see ChannelActionListener
     * @param date The date of this line
     * @param cChannel Channel where the action was sent to
     * @param cChannelClient ChannelClient who sent the action (may be null if server)
     * @param sMessage action contents
     * @param sHost Hostname of sender (or servername)
     */
    protected void callChannelAction(final Date date, final ChannelInfo cChannel,
            final ChannelClientInfo cChannelClient, final String sMessage, final String sHost) {
        getCallback(ChannelActionListener.class)
                .onChannelAction(parser, date, cChannel, cChannelClient, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the ChannelCTCP Callback.
     *
     * @see ChannelCtcpListener
     * @param date The date of this line
     * @param cChannel Channel where CTCP was sent
     * @param cChannelClient ChannelClient who sent the message (may be null if server)
     * @param sType Type of CTCP (VERSION, TIME etc)
     * @param sMessage Additional contents
     * @param sHost Hostname of sender (or servername)
     */
    protected void callChannelCTCP(final Date date, final ChannelInfo cChannel,
            final ChannelClientInfo cChannelClient, final String sType, final String sMessage,
            final String sHost) {
        getCallback(ChannelCtcpListener.class)
                .onChannelCTCP(parser, date, cChannel, cChannelClient, sType, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the ChannelCTCPReply Callback.
     *
     * @see ChannelCtcpReplyListener
     * @param date The date of this line
     * @param cChannel Channel where CTCPReply was sent
     * @param cChannelClient ChannelClient who sent the message (may be null if server)
     * @param sType Type of CTCPRReply (VERSION, TIME etc)
     * @param sMessage Reply Contents
     * @param sHost Hostname of sender (or servername)
     */
    protected void callChannelCTCPReply(final Date date, final ChannelInfo cChannel,
            final ChannelClientInfo cChannelClient, final String sType, final String sMessage,
            final String sHost) {
        getCallback(ChannelCtcpReplyListener.class)
                .onChannelCTCPReply(parser, date, cChannel, cChannelClient, sType, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the ChannelMessage Callback.
     *
     * @see ChannelMessageListener
     * @param date The date of this line
     * @param cChannel Channel where the message was sent to
     * @param cChannelClient ChannelClient who sent the message (may be null if server)
     * @param sMessage Message contents
     * @param sHost Hostname of sender (or servername)
     */
    protected void callChannelMessage(final Date date, final ChannelInfo cChannel,
            final ChannelClientInfo cChannelClient, final String sMessage, final String sHost) {
        getCallback(ChannelMessageListener.class)
                .onChannelMessage(parser, date, cChannel, cChannelClient, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the ChannelNotice Callback.
     *
     * @see ChannelNoticeListener
     * @param date The date of this line
     * @param cChannel Channel where the notice was sent to
     * @param cChannelClient ChannelClient who sent the notice (may be null if server)
     * @param sMessage notice contents
     * @param sHost Hostname of sender (or servername)
     */
    protected void callChannelNotice(final Date date, final ChannelInfo cChannel,
            final ChannelClientInfo cChannelClient, final String sMessage, final String sHost) {
        getCallback(ChannelNoticeListener.class)
                .onChannelNotice(parser, date, cChannel, cChannelClient, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the ChannelModeNotice Callback.
     *
     * @see ChannelModeNoticeListener
     * @param date The date of this line
     * @param prefix Prefix that was used to send this notice.
     * @param cChannel Channel where the notice was sent to
     * @param cChannelClient ChannelClient who sent the notice (may be null if server)
     * @param sMessage notice contents
     * @param sHost Hostname of sender (or servername)
     */
    protected void callChannelModeNotice(final Date date, final char prefix,
            final ChannelInfo cChannel, final ChannelClientInfo cChannelClient,
            final String sMessage, final String sHost) {
        getCallback(ChannelModeNoticeListener.class)
                .onChannelModeNotice(parser, date, cChannel, prefix, cChannelClient, sMessage,
                        sHost);
    }

    /**
     * Callback to all objects implementing the ChannelModeMessage Callback.
     *
     * @see ChannelModeMessageListener
     * @param date The date of this line
     * @param prefix Prefix that was used to send this notice.
     * @param cChannel Channel where the notice was sent to
     * @param cChannelClient ChannelClient who sent the notice (may be null if server)
     * @param sMessage message contents
     * @param sHost Hostname of sender (or servername)
     */
    protected void callChannelModeMessage(final Date date, final char prefix,
            final ChannelInfo cChannel, final ChannelClientInfo cChannelClient,
            final String sMessage, final String sHost) {
        getCallback(ChannelModeMessageListener.class)
                .onChannelModeMessage(parser, date, cChannel, prefix, cChannelClient, sMessage,
                        sHost);
    }

    /**
     * Callback to all objects implementing the PrivateAction Callback.
     *
     * @see PrivateActionListener
     * @param date The date of this line
     * @param sMessage action contents
     * @param sHost Hostname of sender (or servername)
     */
    protected void callPrivateAction(final Date date, final String sMessage, final String sHost) {
        getCallback(PrivateActionListener.class)
                .onPrivateAction(parser, date, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the PrivateCTCP Callback.
     *
     * @see PrivateCtcpListener
     * @param date The date of this line
     * @param sType Type of CTCP (VERSION, TIME etc)
     * @param sMessage Additional contents
     * @param sHost Hostname of sender (or servername)
     */
    protected void callPrivateCTCP(final Date date, final String sType, final String sMessage,
            final String sHost) {
        getCallback(PrivateCtcpListener.class)
                .onPrivateCTCP(parser, date, sType, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the PrivateCTCPReply Callback.
     *
     * @see PrivateCtcpReplyListener
     * @param date The date of this line
     * @param sType Type of CTCPRReply (VERSION, TIME etc)
     * @param sMessage Reply Contents
     * @param sHost Hostname of sender (or servername)
     */
    protected void callPrivateCTCPReply(final Date date, final String sType, final String sMessage,
            final String sHost) {
        getCallback(PrivateCtcpReplyListener.class)
                .onPrivateCTCPReply(parser, date, sType, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the PrivateMessage Callback.
     *
     * @see PrivateMessageListener
     * @param date The date of this line
     * @param sMessage Message contents
     * @param sHost Hostname of sender (or servername)
     */
    protected void callPrivateMessage(final Date date, final String sMessage, final String sHost) {
        getCallback(PrivateMessageListener.class)
                .onPrivateMessage(parser, date, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the PrivateNotice Callback.
     *
     * @see PrivateNoticeListener
     * @param date The date of this line
     * @param sMessage Notice contents
     * @param sHost Hostname of sender (or servername)
     */
    protected void callPrivateNotice(final Date date, final String sMessage, final String sHost) {
        getCallback(PrivateNoticeListener.class)
                .onPrivateNotice(parser, date, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the ServerNotice Callback.
     *
     * @see ServerNoticeListener
     * @param date The date of this line
     * @param sMessage Notice contents
     * @param sHost Hostname of sender (or servername)
     */
    protected void callServerNotice(final Date date, final String sMessage, final String sHost) {
        getCallback(ServerNoticeListener.class)
                .onServerNotice(parser, date, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the UnknownAction Callback.
     *
     * @see UnknownActionListener
     * @param date The date of this line
     * @param sMessage Action contents
     * @param sTarget Actual target of action
     * @param sHost Hostname of sender (or servername)
     */
    protected void callUnknownAction(final Date date, final String sMessage, final String sTarget,
            final String sHost) {
        getCallback(UnknownActionListener.class)
                .onUnknownAction(parser, date, sMessage, sTarget, sHost);
    }

    /**
     * Callback to all objects implementing the UnknownCTCP Callback.
     *
     * @see UnknownCtcpListener
     * @param date The date of this line
     * @param sType Type of CTCP (VERSION, TIME etc)
     * @param sMessage Additional contents
     * @param sTarget Actual Target of CTCP
     * @param sHost Hostname of sender (or servername)
     */
    protected void callUnknownCTCP(final Date date, final String sType, final String sMessage,
            final String sTarget, final String sHost) {
        getCallback(UnknownCtcpListener.class)
                .onUnknownCTCP(parser, date, sType, sMessage, sTarget, sHost);
    }

    /**
     * Callback to all objects implementing the UnknownCTCPReply Callback.
     *
     * @see UnknownCtcpReplyListener
     * @param date The date of this line
     * @param sType Type of CTCPRReply (VERSION, TIME etc)
     * @param sMessage Reply Contents
     * @param sTarget Actual Target of CTCPReply
     * @param sHost Hostname of sender (or servername)
     */
    protected void callUnknownCTCPReply(final Date date, final String sType, final String sMessage,
            final String sTarget, final String sHost) {
        getCallback(UnknownCtcpReplyListener.class)
                .onUnknownCTCPReply(parser, date, sType, sMessage, sTarget, sHost);
    }

    /**
     * Callback to all objects implementing the UnknownMessage Callback.
     *
     * @see UnknownMessageListener
     * @param date The date of this line
     * @param sMessage Message contents
     * @param sTarget Actual target of message
     * @param sHost Hostname of sender (or servername)
     */
    protected void callUnknownMessage(final Date date, final String sMessage, final String sTarget,
            final String sHost) {
        getCallback(UnknownMessageListener.class)
                .onUnknownMessage(parser, date, sMessage, sTarget, sHost);
    }

    /**
     * Callback to all objects implementing the UnknownNotice Callback.
     *
     * @see UnknownNoticeListener
     * @param date The date of this line
     * @param sMessage Notice contents
     * @param sTarget Actual target of notice
     * @param sHost Hostname of sender (or servername)
     */
    protected void callUnknownNotice(final Date date, final String sMessage, final String sTarget,
            final String sHost) {
        getCallback(UnknownNoticeListener.class)
                .onUnknownNotice(parser, date, sMessage, sTarget, sHost);
    }

    /**
     * Callback to all objects implementing the UnknownNotice Callback.
     *
     * @see UnknownServerNoticeListener
     * @param date The date of this line
     * @param sMessage Notice contents
     * @param sTarget Actual target of notice
     * @param sHost Hostname of sender (or servername)
     */
    protected void callUnknownServerNotice(final Date date, final String sMessage,
            final String sTarget, final String sHost) {
        getCallback(UnknownServerNoticeListener.class)
                .onUnknownServerNotice(parser, date, sMessage, sTarget, sHost);
    }

}
