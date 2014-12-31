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

import com.dmdirc.parser.common.ChannelListModeItem;
import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.ClientInfo;
import com.dmdirc.parser.interfaces.callbacks.ChannelModeChangeListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelNonUserModeChangeListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelSingleModeChangeListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelUserModeChangeListener;
import com.dmdirc.parser.interfaces.callbacks.UserModeChangeListener;
import com.dmdirc.parser.interfaces.callbacks.UserModeDiscoveryListener;
import com.dmdirc.parser.irc.IRCChannelClientInfo;
import com.dmdirc.parser.irc.IRCChannelInfo;
import com.dmdirc.parser.irc.IRCClientInfo;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.ModeManager;
import com.dmdirc.parser.irc.PrefixModeManager;
import com.dmdirc.parser.irc.ProcessingManager;

import java.util.Calendar;
import java.util.Date;

/**
 * Process a Mode line.
 */
public class ProcessMode extends IRCProcessor {

    /** The manager to use to access prefix modes. */
    private final PrefixModeManager prefixModeManager;
    /** Mode manager to use for user modes. */
    private final ModeManager userModeManager;
    /** Mode manager to use for channel modes. */
    private final ModeManager chanModeManager;

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param prefixModeManager The manager to use to access prefix modes.
     * @param userModeManager Mode manager to use for user modes.
     * @param chanModeManager Mode manager to use for channel modes.
     * @param manager ProcessingManager that is in charge of this IRCProcessor
     */
    public ProcessMode(final IRCParser parser, final PrefixModeManager prefixModeManager,
            final ModeManager userModeManager, final ModeManager chanModeManager,
            final ProcessingManager manager) {
        super(parser, manager, "MODE", "324", "221");
        this.prefixModeManager = prefixModeManager;
        this.userModeManager = userModeManager;
        this.chanModeManager = chanModeManager;
    }

    /**
     * Process a Mode Line.
     *
     * @param sParam Type of line to process ("MODE", "324")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String... token) {
        final String[] sModestr;
        final String sChannelName;
        switch (sParam) {
            case "324":
                sChannelName = token[3];
                sModestr = new String[token.length - 4];
                System.arraycopy(token, 4, sModestr, 0, token.length - 4);
                break;
            case "221":
                processUserMode(sParam, token, new String[]{token[token.length - 1]}, true);
                return;
            default:
                sChannelName = token[2];
                sModestr = new String[token.length - 3];
                System.arraycopy(token, 3, sModestr, 0, token.length - 3);
                break;
        }

        if (isValidChannelName(sChannelName)) {
            processChanMode(sParam, token, sModestr, sChannelName);
        } else {
            processUserMode(sParam, token, sModestr, false);
        }
    }

    /**
     * Method to trim spaces from strings.
     *
     * @param str String to trim
     * @return String without spaces on the ends
     */
    private String trim(final String str) {
        return str.trim();
    }

    /**
     * Process Chan modes.
     *
     * @param sParam String representation of parameter to parse
     * @param token IRCTokenised Array of the incomming line
     * @param sModestr The modes and params
     * @param sChannelName Channel these modes are for
     */
    public void processChanMode(final String sParam, final String[] token, final String[] sModestr, final String sChannelName) {
        final StringBuilder sFullModeStr = new StringBuilder();

        final IRCChannelInfo iChannel = getChannel(sChannelName);
        if (iChannel == null) {
            return;
        }
        // Get the current channel modes
        String nCurrent = "";
        if (!"324".equals(sParam)) {
            nCurrent = iChannel.getMode();
        }

        final IRCChannelClientInfo setterCCI = iChannel.getChannelClient(token[0]);
        // Facilitate dmdirc formatter
        if (IRCParser.ALWAYS_UPDATECLIENT && setterCCI != null && setterCCI.getClient().getHostname().isEmpty()) {
            setterCCI.getClient().setUserBits(token[0], false);
        }

        // Loop through the mode string, and add/remove modes/params where they are needed
        char cPositive = '+';
        boolean bPositive = true;
        long nValue = 0;
        int nParam = 1;
        final StringBuilder sNonUserModeStrParams = new StringBuilder();
        final StringBuilder sNonUserModeStr = new StringBuilder();
        for (int i = 0; i < sModestr[0].length(); ++i) {
            final Character cMode = sModestr[0].charAt(i);
            if (cMode.equals(":".charAt(0))) {
                continue;
            }

            sNonUserModeStr.append(cMode);
            if (cMode.equals("+".charAt(0))) {
                cPositive = '+';
                bPositive = true;
            } else if (cMode.equals("-".charAt(0))) {
                cPositive = '-';
                bPositive = false;
            } else {
                final boolean bBooleanMode;
                final String sModeParam;
                if (chanModeManager.isMode(cMode)) {
                    bBooleanMode = true;
                } else if (parser.chanModesOther.containsKey(cMode)) {
                    nValue = parser.chanModesOther.get(cMode);
                    bBooleanMode = false;
                } else if (prefixModeManager.isPrefixMode(cMode)) {
                    // (de) OP/Voice someone
                    if (sModestr.length <= nParam) {
                        parser.callErrorInfo(new ParserError(ParserError.ERROR_FATAL + ParserError.ERROR_USER, "Broken Modes. Parameter required but not given.", parser.getLastLine()));
                        return;
                    }
                    sModeParam = sModestr[nParam++];
                    callDebugInfo(IRCParser.DEBUG_INFO, "User Mode: %c / %s {Positive: %b}",
                            cMode, sModeParam, bPositive);
                    final IRCChannelClientInfo iChannelClientInfo = iChannel.getChannelClient(sModeParam);
                    if (iChannelClientInfo == null) {
                        // Client not known?
                        callDebugInfo(IRCParser.DEBUG_INFO, "User Mode for client not on channel." +
                                " Ignoring (%s)", sModeParam);
                        continue;
                    }
                    callDebugInfo(IRCParser.DEBUG_INFO, "\tOld Mode Value: %s",
                            iChannelClientInfo.getAllModes());
                    if (bPositive) {
                        iChannelClientInfo.addMode(cMode);
                    } else {
                        iChannelClientInfo.removeMode(cMode);
                    }
                    callChannelUserModeChanged(iChannel, iChannelClientInfo, setterCCI, token[0],
                            (bPositive ? "+" : "-") + cMode);
                    continue;
                } else {
                    // unknown mode - add as boolean
                    chanModeManager.add(cMode);
                    bBooleanMode = true;
                }

                if (bBooleanMode) {
                    callDebugInfo(IRCParser.DEBUG_INFO, "Boolean Mode: %c {Positive: %b}", cMode, bPositive);

                    if (bPositive) {
                        nCurrent = chanModeManager.insertMode(nCurrent, cMode);
                    } else {
                        nCurrent = chanModeManager.removeMode(nCurrent, cMode);
                    }
                } else {

                    if ((bPositive || nValue == IRCParser.MODE_LIST ||
                            (nValue & IRCParser.MODE_UNSET) == IRCParser.MODE_UNSET) &&
                            sModestr.length <= nParam) {
                        parser.callErrorInfo(new ParserError(ParserError.ERROR_FATAL + ParserError.ERROR_USER, "Broken Modes. Parameter required but not given.", parser.getLastLine()));
                        continue;
                    }

                    if (nValue == IRCParser.MODE_LIST) {
                        // List Mode
                        sModeParam = sModestr[nParam++];
                        sNonUserModeStrParams.append(' ').append(sModeParam);
                        final long nTemp = Calendar.getInstance().getTimeInMillis() / 1000;
                        iChannel.setListModeParam(cMode, new ChannelListModeItem(sModeParam, token[0], nTemp), bPositive);
                        callDebugInfo(IRCParser.DEBUG_INFO, "List Mode: %c [%s] {Positive: %b}", cMode, sModeParam, bPositive);
                        if (!"324".equals(sParam)) {
                            getCallback(ChannelSingleModeChangeListener.class)
                                    .onChannelSingleModeChanged(parser, new Date(), iChannel,
                                            setterCCI, token[0], cPositive + cMode + " " +
                                                    sModeParam);
                        }
                    } else {
                        // Mode with a parameter
                        if (bPositive) {
                            // +Mode - always needs a parameter to set
                            sModeParam = sModestr[nParam++];
                            sNonUserModeStrParams.append(' ').append(sModeParam);
                            callDebugInfo(IRCParser.DEBUG_INFO, "Set Mode: %c [%s] {Positive: %b}", cMode, sModeParam, bPositive);
                            iChannel.setModeParam(cMode, sModeParam);
                            if (!"324".equals(sParam)) {
                                getCallback(ChannelSingleModeChangeListener.class)
                                        .onChannelSingleModeChanged(parser, new Date(), iChannel,
                                                setterCCI, token[0], cPositive + cMode + " " +
                                                        sModeParam);
                            }
                        } else {
                            // -Mode - parameter isn't always needed, we need to check
                            if ((nValue & IRCParser.MODE_UNSET) == IRCParser.MODE_UNSET) {
                                sModeParam = sModestr[nParam++];
                                sNonUserModeStrParams.append(' ').append(sModeParam);
                            } else {
                                sModeParam = "";
                            }
                            callDebugInfo(IRCParser.DEBUG_INFO, "Unset Mode: %c [%s] {Positive: %b}", cMode, sModeParam, bPositive);
                            iChannel.setModeParam(cMode, "");
                            if (!"324".equals(sParam)) {
                                getCallback(ChannelSingleModeChangeListener.class)
                                        .onChannelSingleModeChanged(parser, new Date(), iChannel,
                                                setterCCI, token[0], trim(cPositive + cMode + " "
                                                        + sModeParam));
                            }
                        }
                    }
                }
            }
        }

        // Call Callbacks
        for (String aSModestr : sModestr) {
            sFullModeStr.append(aSModestr).append(' ');
        }

        iChannel.setMode(nCurrent);
        if ("324".equals(sParam)) {
            callChannelModeChanged(iChannel, null, "", sFullModeStr.toString().trim());
        } else {
            callChannelModeChanged(iChannel, setterCCI, token[0], sFullModeStr.toString().trim());
            getCallback(ChannelNonUserModeChangeListener.class)
                    .onChannelNonUserModeChanged(parser, new Date(), iChannel, setterCCI, token[0],
                            trim(sNonUserModeStr.toString() + sNonUserModeStrParams));
        }
    }

    /**
     * Process user modes.
     *
     * @param sParam String representation of parameter to parse
     * @param token IRCTokenised Array of the incomming line
     * @param clearOldModes Clear old modes before applying these modes (used by 221)
     */
    private void processUserMode(final String sParam, final String[] token, final String[] sModestr,
            final boolean clearOldModes) {
        final IRCClientInfo iClient = getClientInfo(token[2]);

        if (iClient == null) {
            return;
        }

        String nCurrent;
        if (clearOldModes) {
            nCurrent = "";
        } else {
            nCurrent = iClient.getUserMode();
        }

        boolean bPositive = true;
        for (int i = 0; i < sModestr[0].length(); ++i) {
            final Character cMode = sModestr[0].charAt(i);
            if (cMode.equals("+".charAt(0))) {
                bPositive = true;
            } else if (cMode.equals("-".charAt(0))) {
                bPositive = false;
            } else if (!cMode.equals(":".charAt(0))) {
                if (!userModeManager.isMode(cMode)) {
                    // Unknown mode
                    callErrorInfo(new ParserError(ParserError.ERROR_WARNING, "Got unknown user mode " + cMode + " - Added", parser.getLastLine()));
                    userModeManager.add(cMode);
                }
                // Usermodes are always boolean
                callDebugInfo(IRCParser.DEBUG_INFO, "User Mode: %c {Positive: %b}", cMode, bPositive);
                if (bPositive) {
                    nCurrent = userModeManager.insertMode(nCurrent, cMode);
                } else {
                    nCurrent = userModeManager.removeMode(nCurrent, cMode);
                }
            }
        }

        iClient.setUserMode(nCurrent);
        if ("221".equals(sParam)) {
            callUserModeDiscovered(iClient, sModestr[0]);
        } else {
            callUserModeChanged(iClient, token[0], sModestr[0]);
        }
    }

    /**
     * Callback to all objects implementing the ChannelModeChanged Callback.
     *
     * @see ChannelModeChangeListener
     * @param cChannel Channel where modes were changed
     * @param cChannelClient Client chaning the modes (null if server)
     * @param sHost Host doing the mode changing (User host or server name)
     * @param sModes Exact String parsed
     */
    protected void callChannelModeChanged(final ChannelInfo cChannel,
            final ChannelClientInfo cChannelClient, final String sHost, final String sModes) {
        getCallback(ChannelModeChangeListener.class)
                .onChannelModeChanged(parser, new Date(), cChannel, cChannelClient, sHost, sModes);
    }

    /**
     * Callback to all objects implementing the ChannelUserModeChanged Callback.
     *
     * @see ChannelUserModeChangeListener
     * @param cChannel Channel where modes were changed
     * @param cChangedClient Client being changed
     * @param cSetByClient Client chaning the modes (null if server)
     * @param sHost Host doing the mode changing (User host or server name)
     * @param sMode String representing mode change (ie +o)
     */
    protected void callChannelUserModeChanged(final ChannelInfo cChannel,
            final ChannelClientInfo cChangedClient, final ChannelClientInfo cSetByClient,
            final String sHost, final String sMode) {
        getCallback(ChannelUserModeChangeListener.class)
                .onChannelUserModeChanged(parser, new Date(), cChannel, cChangedClient,
                        cSetByClient, sHost, sMode);
    }

    /**
     * Callback to all objects implementing the UserModeChanged Callback.
     *
     * @see UserModeChangeListener
     * @param cClient Client that had the mode changed (almost always us)
     * @param sSetby Host that set the mode (us or servername)
     * @param sModes The modes set.
     */
    protected void callUserModeChanged(final ClientInfo cClient, final String sSetby,
            final String sModes) {
        getCallback(UserModeChangeListener.class)
                .onUserModeChanged(parser, new Date(), cClient, sSetby, sModes);
    }

    /**
     * Callback to all objects implementing the UserModeDiscovered Callback.
     *
     * @see UserModeDiscoveryListener
     * @param cClient Client that had the mode changed (almost always us)
     * @param sModes The modes set.
     */
    protected void callUserModeDiscovered(final ClientInfo cClient, final String sModes) {
        getCallback(UserModeDiscoveryListener.class)
                .onUserModeDiscovered(parser, new Date(), cClient, sModes);
    }

}
