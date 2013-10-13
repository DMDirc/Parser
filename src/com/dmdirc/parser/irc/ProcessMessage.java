/*
 * Copyright (c) 2006-2013 DMDirc Developers
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

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param manager ProcessingManager that is in charge of this IRCProcessor
     */
    protected ProcessMessage(final IRCParser parser, final ProcessingManager manager) {
        super(parser, manager);
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
    public void process(final Date date, final String sParam, final String[] token) {
        // Ignore people!
        String sMessage = "";
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
        if (token[0].indexOf('!') == -1 && token[1].equalsIgnoreCase("NOTICE") && token[2].equalsIgnoreCase("AUTH")) {
            try {
                parser.getProcessingManager().process(date, "Notice Auth", token);
            } catch (ProcessorNotFoundException e) {
            }
            return;
        }

        IRCChannelClientInfo iChannelClient = null;
        IRCChannelInfo iChannel = null;
        IRCClientInfo iClient = null;
        // "nick!user@host PRIVMSG #Channel" should be processed as "nick!user@host PRIVMSG #Channel :"
        if (token.length < 4) {
            sMessage = "";
        } else {
            sMessage = token[token.length - 1];
        }
        String[] bits = sMessage.split(" ", 2);
        final Character char1 = Character.valueOf((char) 1);
        String sCTCP = "";
        boolean isAction = false;
        boolean isCTCP = false;

        if (sMessage.length() > 1) {
            // Actions are special CTCPs
            // Bits is the message been split into 2 parts
            //         the first word and the rest
            if (sParam.equalsIgnoreCase("PRIVMSG") && bits[0].equalsIgnoreCase(char1 + "ACTION") && Character.valueOf(sMessage.charAt(sMessage.length() - 1)).equals(char1)) {
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
        if (token[0].charAt(0) == ':' && token[0].length() > 1) {
            token[0] = token[0].substring(1);
        }

        iClient = getClientInfo(token[0]);
        // Facilitate DMDIRC Formatter
        if ((IRCParser.ALWAYS_UPDATECLIENT && iClient != null) && iClient.getHostname().isEmpty()) {
            iClient.setUserBits(token[0], false);
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
        final boolean hasModePrefix = parser.prefixMap.containsKey(modePrefix) && !parser.prefixModes.containsKey(modePrefix);
        final String targetName = hasModePrefix ? token[2].substring(1) : token[2];

        if (isValidChannelName(targetName)) {
            iChannel = getChannel(targetName);
            if (iChannel == null) {
                // callErrorInfo(new ParserError(ParserError.ERROR_WARNING, "Got message for channel ("+targetName+") that I am not on.", parser.getLastLine()));
                return;
            }
            if (iClient != null) {
                iChannelClient = iChannel.getChannelClient(iClient);
            }
            if (sParam.equalsIgnoreCase("PRIVMSG")) {
                if (isAction) {
                    callChannelAction(date, iChannel, iChannelClient, sMessage, token[0]);
                } else {
                    if (isCTCP) {
                        callChannelCTCP(date, iChannel, iChannelClient, sCTCP, sMessage, token[0]);
                    } else if (hasModePrefix) {
                        callChannelModeMessage(date, modePrefix, iChannel, iChannelClient, sMessage, token[0]);
                    } else {
                        callChannelMessage(date, iChannel, iChannelClient, sMessage, token[0]);
                    }
                }
            } else if (sParam.equalsIgnoreCase("NOTICE")) {
                if (isCTCP) {
                    callChannelCTCPReply(date, iChannel, iChannelClient, sCTCP, sMessage, token[0]);
                } else if (hasModePrefix) {
                    callChannelModeNotice(date, modePrefix, iChannel, iChannelClient, sMessage, token[0]);
                } else {
                    callChannelNotice(date, iChannel, iChannelClient, sMessage, token[0]);
                }
            }
        } else if (parser.getStringConverter().equalsIgnoreCase(token[2], parser.getMyNickname())) {
            if (sParam.equalsIgnoreCase("PRIVMSG")) {
                if (isAction) {
                    callPrivateAction(date, sMessage, token[0]);
                } else {
                    if (isCTCP) {
                        callPrivateCTCP(date, sCTCP, sMessage, token[0]);
                    } else {
                        callPrivateMessage(date, sMessage, token[0]);
                    }
                }
            } else if (sParam.equalsIgnoreCase("NOTICE")) {
                if (isCTCP) {
                    callPrivateCTCPReply(date, sCTCP, sMessage, token[0]);
                } else {
                    if (token[0].indexOf('@') == -1) {
                        callServerNotice(date, sMessage, token[0]);
                    } else {
                        callPrivateNotice(date, sMessage, token[0]);
                    }
                }
            }
        } else {
            callDebugInfo(IRCParser.DEBUG_INFO, "Message for Other (" + token[2] + ")");
            if (sParam.equalsIgnoreCase("PRIVMSG")) {
                if (isAction) {
                    callUnknownAction(date, sMessage, token[2], token[0]);
                } else {
                    if (isCTCP) {
                        callUnknownCTCP(date, sCTCP, sMessage, token[2], token[0]);
                    } else {
                        callUnknownMessage(date, sMessage, token[2], token[0]);
                    }
                }
            } else if (sParam.equalsIgnoreCase("NOTICE")) {
                if (isCTCP) {
                    callUnknownCTCPReply(date, sCTCP, sMessage, token[2], token[0]);
                } else {
                    if (token[0].indexOf('@') == -1) {
                        callUnknownServerNotice(date, sMessage, token[2], token[0]);
                    } else {
                        callUnknownNotice(date, sMessage, token[2], token[0]);
                    }
                }
            }
        }
    }

    /**
     * Callback to all objects implementing the ChannelAction Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.ChannelActionListener
     * @param date The date of this line
     * @param cChannel Channel where the action was sent to
     * @param cChannelClient ChannelClient who sent the action (may be null if server)
     * @param sMessage action contents
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callChannelAction(final Date date, final ChannelInfo cChannel, final ChannelClientInfo cChannelClient, final String sMessage, final String sHost) {
        return getCallbackManager().getCallbackType(ChannelActionListener.class).call(date, cChannel, cChannelClient, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the ChannelCTCP Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.ChannelCtcpListener
     * @param date The date of this line
     * @param cChannel Channel where CTCP was sent
     * @param cChannelClient ChannelClient who sent the message (may be null if server)
     * @param sType Type of CTCP (VERSION, TIME etc)
     * @param sMessage Additional contents
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callChannelCTCP(final Date date, final ChannelInfo cChannel, final ChannelClientInfo cChannelClient, final String sType, final String sMessage, final String sHost) {
        return getCallbackManager().getCallbackType(ChannelCtcpListener.class).call(date, cChannel, cChannelClient, sType, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the ChannelCTCPReply Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.ChannelCtcpReplyListener
     * @param date The date of this line
     * @param cChannel Channel where CTCPReply was sent
     * @param cChannelClient ChannelClient who sent the message (may be null if server)
     * @param sType Type of CTCPRReply (VERSION, TIME etc)
     * @param sMessage Reply Contents
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callChannelCTCPReply(final Date date, final ChannelInfo cChannel, final ChannelClientInfo cChannelClient, final String sType, final String sMessage, final String sHost) {
        return getCallbackManager().getCallbackType(ChannelCtcpReplyListener.class).call(date, cChannel, cChannelClient, sType, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the ChannelMessage Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.ChannelMessageListener
     * @param date The date of this line
     * @param cChannel Channel where the message was sent to
     * @param cChannelClient ChannelClient who sent the message (may be null if server)
     * @param sMessage Message contents
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callChannelMessage(final Date date, final ChannelInfo cChannel, final ChannelClientInfo cChannelClient, final String sMessage, final String sHost) {
        return getCallbackManager().getCallbackType(ChannelMessageListener.class).call(date, cChannel, cChannelClient, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the ChannelNotice Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.ChannelNoticeListener
     * @param date The date of this line
     * @param cChannel Channel where the notice was sent to
     * @param cChannelClient ChannelClient who sent the notice (may be null if server)
     * @param sMessage notice contents
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callChannelNotice(final Date date, final ChannelInfo cChannel, final ChannelClientInfo cChannelClient, final String sMessage, final String sHost) {
        return getCallbackManager().getCallbackType(ChannelNoticeListener.class).call(date, cChannel, cChannelClient, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the ChannelModeNotice Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.ChannelModeNoticeListener
     * @param date The date of this line
     * @param prefix Prefix that was used to send this notice.
     * @param cChannel Channel where the notice was sent to
     * @param cChannelClient ChannelClient who sent the notice (may be null if server)
     * @param sMessage notice contents
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callChannelModeNotice(final Date date, final char prefix, final ChannelInfo cChannel, final ChannelClientInfo cChannelClient, final String sMessage, final String sHost) {
        return getCallbackManager().getCallbackType(ChannelModeNoticeListener.class).call(date, cChannel, prefix, cChannelClient, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the ChannelModeMessage Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.ChannelModeMessageListener
     * @param date The date of this line
     * @param prefix Prefix that was used to send this notice.
     * @param cChannel Channel where the notice was sent to
     * @param cChannelClient ChannelClient who sent the notice (may be null if server)
     * @param sMessage message contents
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callChannelModeMessage(final Date date, final char prefix, final ChannelInfo cChannel, final ChannelClientInfo cChannelClient, final String sMessage, final String sHost) {
        return getCallbackManager().getCallbackType(ChannelModeMessageListener.class).call(date, cChannel, prefix, cChannelClient, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the PrivateAction Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.PrivateActionListener
     * @param date The date of this line
     * @param sMessage action contents
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callPrivateAction(final Date date, final String sMessage, final String sHost) {
        return getCallbackManager().getCallbackType(PrivateActionListener.class).call(date, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the PrivateCTCP Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.PrivateCtcpListener
     * @param date The date of this line
     * @param sType Type of CTCP (VERSION, TIME etc)
     * @param sMessage Additional contents
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callPrivateCTCP(final Date date, final String sType, final String sMessage, final String sHost) {
        return getCallbackManager().getCallbackType(PrivateCtcpListener.class).call(date, sType, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the PrivateCTCPReply Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.PrivateCtcpReplyListener
     * @param date The date of this line
     * @param sType Type of CTCPRReply (VERSION, TIME etc)
     * @param sMessage Reply Contents
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callPrivateCTCPReply(final Date date, final String sType, final String sMessage, final String sHost) {
        return getCallbackManager().getCallbackType(PrivateCtcpReplyListener.class).call(date, sType, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the PrivateMessage Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.PrivateMessageListener
     * @param date The date of this line
     * @param sMessage Message contents
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callPrivateMessage(final Date date, final String sMessage, final String sHost) {
        return getCallbackManager().getCallbackType(PrivateMessageListener.class).call(date, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the PrivateNotice Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.PrivateNoticeListener
     * @param date The date of this line
     * @param sMessage Notice contents
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callPrivateNotice(final Date date, final String sMessage, final String sHost) {
        return getCallbackManager().getCallbackType(PrivateNoticeListener.class).call(date, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the ServerNotice Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.ServerNoticeListener
     * @param date The date of this line
     * @param sMessage Notice contents
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callServerNotice(final Date date, final String sMessage, final String sHost) {
        return getCallbackManager().getCallbackType(ServerNoticeListener.class).call(date, sMessage, sHost);
    }

    /**
     * Callback to all objects implementing the UnknownAction Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.UnknownActionListener
     * @param date The date of this line
     * @param sMessage Action contents
     * @param sTarget Actual target of action
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callUnknownAction(final Date date, final String sMessage, final String sTarget, final String sHost) {
        return getCallbackManager().getCallbackType(UnknownActionListener.class).call(date, sMessage, sTarget, sHost);
    }

    /**
     * Callback to all objects implementing the UnknownCTCP Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.UnknownCtcpListener
     * @param date The date of this line
     * @param sType Type of CTCP (VERSION, TIME etc)
     * @param sMessage Additional contents
     * @param sTarget Actual Target of CTCP
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callUnknownCTCP(final Date date, final String sType, final String sMessage, final String sTarget, final String sHost) {
        return getCallbackManager().getCallbackType(UnknownCtcpListener.class).call(date, sType, sMessage, sTarget, sHost);
    }

    /**
     * Callback to all objects implementing the UnknownCTCPReply Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.UnknownCtcpReplyListener
     * @param date The date of this line
     * @param sType Type of CTCPRReply (VERSION, TIME etc)
     * @param sMessage Reply Contents
     * @param sTarget Actual Target of CTCPReply
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callUnknownCTCPReply(final Date date, final String sType, final String sMessage, final String sTarget, final String sHost) {
        return getCallbackManager().getCallbackType(UnknownCtcpReplyListener.class).call(date, sType, sMessage, sTarget, sHost);
    }

    /**
     * Callback to all objects implementing the UnknownMessage Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.UnknownMessageListener
     * @param date The date of this line
     * @param sMessage Message contents
     * @param sTarget Actual target of message
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callUnknownMessage(final Date date, final String sMessage, final String sTarget, final String sHost) {
        return getCallbackManager().getCallbackType(UnknownMessageListener.class).call(date, sMessage, sTarget, sHost);
    }

    /**
     * Callback to all objects implementing the UnknownNotice Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.UnknownNoticeListener
     * @param date The date of this line
     * @param sMessage Notice contents
     * @param sTarget Actual target of notice
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callUnknownNotice(final Date date, final String sMessage, final String sTarget, final String sHost) {
        return getCallbackManager().getCallbackType(UnknownNoticeListener.class).call(date, sMessage, sTarget, sHost);
    }

    /**
     * Callback to all objects implementing the UnknownNotice Callback.
     *
     * @see com.dmdirc.parser.interfaces.callbacks.UnknownServerNoticeListener
     * @param date The date of this line
     * @param sMessage Notice contents
     * @param sTarget Actual target of notice
     * @param sHost Hostname of sender (or servername)
     * @return true if a method was called, false otherwise
     */
    protected boolean callUnknownServerNotice(final Date date, final String sMessage, final String sTarget, final String sHost) {
        return getCallbackManager().getCallbackType(UnknownServerNoticeListener.class).call(date, sMessage, sTarget, sHost);
    }

    /**
     * What does this IRCProcessor handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"PRIVMSG", "NOTICE"};
    }
}
