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

import com.dmdirc.parser.common.CallbackObject;
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

import java.util.Calendar;

/**
 * Process a Mode line.
 */
public class ProcessMode extends IRCProcessor {

    /**
     * Create a new instance of the IRCProcessor Object.
     *
     * @param parser IRCParser That owns this IRCProcessor
     * @param manager ProcessingManager that is in charge of this IRCProcessor
     */
    protected ProcessMode(final IRCParser parser, final ProcessingManager manager) {
        super(parser, manager);
    }

    /**
     * Process a Mode Line.
     *
     * @param sParam Type of line to process ("MODE", "324")
     * @param token IRCTokenised line to process
     */
    @Override
    public void process(final String sParam, final String[] token) {
        String[] sModestr;
        String sChannelName;
        if (sParam.equals("324")) {
            sChannelName = token[3];
            sModestr = new String[token.length - 4];
            System.arraycopy(token, 4, sModestr, 0, token.length - 4);
        } else if (sParam.equals("221")) {
            processUserMode(sParam, token, new String[]{token[token.length - 1]}, true);
            return;
        } else {
            sChannelName = token[2];
            sModestr = new String[token.length - 3];
            System.arraycopy(token, 3, sModestr, 0, token.length - 3);
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
        String sNonUserModeStr = "";
        String sNonUserModeStrParams = "";
        String sModeParam;
        String sTemp;
        int nParam = 1;
        long nTemp = 0, nValue = 0, nCurrent = 0;
        boolean bPositive = true, bBooleanMode = true;
        char cPositive = '+';
        IRCChannelInfo iChannel;
        IRCChannelClientInfo iChannelClientInfo;
        IRCClientInfo iClient;
        IRCChannelClientInfo setterCCI;

        CallbackObject cbSingle = null;
        CallbackObject cbNonUser = null;

        if (!sParam.equals("324")) {
            cbSingle = getCallbackManager().getCallbackType(ChannelSingleModeChangeListener.class);
            cbNonUser = getCallbackManager().getCallbackType(ChannelNonUserModeChangeListener.class);
        }

        iChannel = getChannel(sChannelName);
        if (iChannel == null) {
            return;
        }
        // Get the current channel modes
        if (!sParam.equals("324")) {
            nCurrent = iChannel.getMode();
        }

        setterCCI = iChannel.getChannelClient(token[0]);
        // Facilitate dmdirc formatter
        if ((IRCParser.ALWAYS_UPDATECLIENT && setterCCI != null) && setterCCI.getClient().getHostname().isEmpty()) {
            setterCCI.getClient().setUserBits(token[0], false);
        }

        // Loop through the mode string, and add/remove modes/params where they are needed
        for (int i = 0; i < sModestr[0].length(); ++i) {
            final Character cMode = sModestr[0].charAt(i);
            if (cMode.equals(":".charAt(0))) {
                continue;
            }

            sNonUserModeStr = sNonUserModeStr + cMode;
            if (cMode.equals("+".charAt(0))) {
                cPositive = '+';
                bPositive = true;
            } else if (cMode.equals("-".charAt(0))) {
                cPositive = '-';
                bPositive = false;
            } else {
                if (parser.chanModesBool.containsKey(cMode)) {
                    nValue = parser.chanModesBool.get(cMode);
                    bBooleanMode = true;
                } else if (parser.chanModesOther.containsKey(cMode)) {
                    nValue = parser.chanModesOther.get(cMode);
                    bBooleanMode = false;
                } else if (parser.prefixModes.containsKey(cMode)) {
                    // (de) OP/Voice someone
                    if (sModestr.length <= nParam) {
                        parser.callErrorInfo(new ParserError(ParserError.ERROR_FATAL + ParserError.ERROR_USER, "Broken Modes. Parameter required but not given.", parser.getLastLine()));
                        return;
                    }
                    sModeParam = sModestr[nParam++];
                    nValue = parser.prefixModes.get(cMode);
                    callDebugInfo(IRCParser.DEBUG_INFO, "User Mode: %c / %d [%s] {Positive: %b}", cMode, nValue, sModeParam, bPositive);
                    iChannelClientInfo = iChannel.getChannelClient(sModeParam);
                    if (iChannelClientInfo == null) {
                        // Client not known?
                        iClient = getClientInfo(sModeParam);
                        if (iClient == null) {
                            iClient = new IRCClientInfo(parser, sModeParam);
                            parser.addClient(iClient);
                        }
                        iChannelClientInfo = iChannel.addClient(iClient);
                    }
                    callDebugInfo(IRCParser.DEBUG_INFO, "\tOld Mode Value: %d", iChannelClientInfo.getChanMode());
                    if (bPositive) {
                        iChannelClientInfo.setChanMode(iChannelClientInfo.getChanMode() | nValue);
                        sTemp = "+";
                    } else {
                        iChannelClientInfo.setChanMode(iChannelClientInfo.getChanMode() ^ (iChannelClientInfo.getChanMode() & nValue));
                        sTemp = "-";
                    }
                    sTemp = sTemp + cMode;
                    callChannelUserModeChanged(iChannel, iChannelClientInfo, setterCCI, token[0], sTemp);
                    continue;
                } else {
                    // unknown mode - add as boolean
                    parser.chanModesBool.put(cMode, parser.nextKeyCMBool);
                    nValue = parser.nextKeyCMBool;
                    bBooleanMode = true;
                    parser.nextKeyCMBool = parser.nextKeyCMBool * 2;
                }

                if (bBooleanMode) {
                    callDebugInfo(IRCParser.DEBUG_INFO, "Boolean Mode: %c [%d] {Positive: %b}", cMode, nValue, bPositive);

                    if (bPositive) {
                        nCurrent = nCurrent | nValue;
                    } else {
                        nCurrent = nCurrent ^ (nCurrent & nValue);
                    }
                } else {

                    if ((bPositive || nValue == IRCParser.MODE_LIST || ((nValue & IRCParser.MODE_UNSET) == IRCParser.MODE_UNSET)) && (sModestr.length <= nParam)) {
                        parser.callErrorInfo(new ParserError(ParserError.ERROR_FATAL + ParserError.ERROR_USER, "Broken Modes. Parameter required but not given.", parser.getLastLine()));
                        continue;
                    }

                    if (nValue == IRCParser.MODE_LIST) {
                        // List Mode
                        sModeParam = sModestr[nParam++];
                        sNonUserModeStrParams = sNonUserModeStrParams + " " + sModeParam;
                        nTemp = (Calendar.getInstance().getTimeInMillis() / 1000);
                        iChannel.setListModeParam(cMode, new ChannelListModeItem(sModeParam, token[0], nTemp), bPositive);
                        callDebugInfo(IRCParser.DEBUG_INFO, "List Mode: %c [%s] {Positive: %b}", cMode, sModeParam, bPositive);
                        if (cbSingle != null) {
                            cbSingle.call(iChannel, setterCCI, token[0], cPositive + cMode + " " + sModeParam);
                        }
                    } else {
                        // Mode with a parameter
                        if (bPositive) {
                            // +Mode - always needs a parameter to set
                            sModeParam = sModestr[nParam++];
                            sNonUserModeStrParams = sNonUserModeStrParams + " " + sModeParam;
                            callDebugInfo(IRCParser.DEBUG_INFO, "Set Mode: %c [%s] {Positive: %b}", cMode, sModeParam, bPositive);
                            iChannel.setModeParam(cMode, sModeParam);
                            if (cbSingle != null) {
                                cbSingle.call(iChannel, setterCCI, token[0], cPositive + cMode + " " + sModeParam);
                            }
                        } else {
                            // -Mode - parameter isn't always needed, we need to check
                            if ((nValue & IRCParser.MODE_UNSET) == IRCParser.MODE_UNSET) {
                                sModeParam = sModestr[nParam++];
                                sNonUserModeStrParams = sNonUserModeStrParams + " " + sModeParam;
                            } else {
                                sModeParam = "";
                            }
                            callDebugInfo(IRCParser.DEBUG_INFO, "Unset Mode: %c [%s] {Positive: %b}", cMode, sModeParam, bPositive);
                            iChannel.setModeParam(cMode, "");
                            if (cbSingle != null) {
                                cbSingle.call(iChannel, setterCCI, token[0], trim(cPositive + cMode + " " + sModeParam));
                            }
                        }
                    }
                }
            }
        }

        // Call Callbacks
        for (int i = 0; i < sModestr.length; ++i) {
            sFullModeStr.append(sModestr[i]).append(" ");
        }

        iChannel.setMode(nCurrent);
        if (sParam.equals("324")) {
            callChannelModeChanged(iChannel, null, "", sFullModeStr.toString().trim());
        } else {
            callChannelModeChanged(iChannel, setterCCI, token[0], sFullModeStr.toString().trim());
        }
        if (cbNonUser != null) {
            cbNonUser.call(iChannel, setterCCI, token[0], trim(sNonUserModeStr + sNonUserModeStrParams));
        }
    }

    /**
     * Process user modes.
     *
     * @param sParam String representation of parameter to parse
     * @param token IRCTokenised Array of the incomming line
     * @param clearOldModes Clear old modes before applying these modes (used by 221)
     */
    private void processUserMode(final String sParam, final String[] token, final String[] sModestr, final boolean clearOldModes) {
        long nCurrent = 0, nValue = 0;
        boolean bPositive = true;

        final IRCClientInfo iClient = getClientInfo(token[2]);

        if (iClient == null) {
            return;
        }

        if (clearOldModes) {
            nCurrent = 0;
        } else {
            nCurrent = iClient.getUserMode();
        }

        for (int i = 0; i < sModestr[0].length(); ++i) {
            final Character cMode = sModestr[0].charAt(i);
            if (cMode.equals("+".charAt(0))) {
                bPositive = true;
            } else if (cMode.equals("-".charAt(0))) {
                bPositive = false;
            } else if (cMode.equals(":".charAt(0))) {
                continue;
            } else {
                if (parser.userModes.containsKey(cMode)) {
                    nValue = parser.userModes.get(cMode);
                } else {
                    // Unknown mode
                    callErrorInfo(new ParserError(ParserError.ERROR_WARNING, "Got unknown user mode " + cMode + " - Added", parser.getLastLine()));
                    parser.userModes.put(cMode, parser.nNextKeyUser);
                    nValue = parser.nNextKeyUser;
                    parser.nNextKeyUser = parser.nNextKeyUser * 2;
                }
                // Usermodes are always boolean
                callDebugInfo(IRCParser.DEBUG_INFO, "User Mode: %c [%d] {Positive: %b}", cMode, nValue, bPositive);
                if (bPositive) {
                    nCurrent = nCurrent | nValue;
                } else {
                    nCurrent = nCurrent ^ (nCurrent & nValue);
                }
            }
        }

        iClient.setUserMode(nCurrent);
        if (sParam.equals("221")) {
            callUserModeDiscovered(iClient, sModestr[0]);
        } else {
            callUserModeChanged(iClient, token[0], sModestr[0]);
        }
    }

    /**
     * Callback to all objects implementing the ChannelModeChanged Callback.
     *
     * @see IChannelModeChanged
     * @param cChannel Channel where modes were changed
     * @param cChannelClient Client chaning the modes (null if server)
     * @param sHost Host doing the mode changing (User host or server name)
     * @param sModes Exact String parsed
     * @return true if a method was called, false otherwise
     */
    protected boolean callChannelModeChanged(final ChannelInfo cChannel, final ChannelClientInfo cChannelClient, final String sHost, final String sModes) {
        return getCallbackManager().getCallbackType(ChannelModeChangeListener.class).call(cChannel, cChannelClient, sHost, sModes);
    }

    /**
     * Callback to all objects implementing the ChannelUserModeChanged Callback.
     *
     * @see IChannelUserModeChanged
     * @param cChannel Channel where modes were changed
     * @param cChangedClient Client being changed
     * @param cSetByClient Client chaning the modes (null if server)
     * @param sMode String representing mode change (ie +o)
     * @param sHost Host doing the mode changing (User host or server name)
     * @return true if a method was called, false otherwise
     */
    protected boolean callChannelUserModeChanged(final ChannelInfo cChannel, final ChannelClientInfo cChangedClient, final ChannelClientInfo cSetByClient, final String sHost, final String sMode) {
        return getCallbackManager().getCallbackType(ChannelUserModeChangeListener.class).call(cChannel, cChangedClient, cSetByClient, sHost, sMode);
    }

    /**
     * Callback to all objects implementing the UserModeChanged Callback.
     *
     * @see IUserModeChanged
     * @param cClient Client that had the mode changed (almost always us)
     * @param sSetby Host that set the mode (us or servername)
     * @param sModes The modes set.
     * @return true if a method was called, false otherwise
     */
    protected boolean callUserModeChanged(final ClientInfo cClient, final String sSetby, final String sModes) {
        return getCallbackManager().getCallbackType(UserModeChangeListener.class).call(cClient, sSetby, sModes);
    }

    /**
     * Callback to all objects implementing the UserModeDiscovered Callback.
     *
     * @see IUserModeDiscovered
     * @param cClient Client that had the mode changed (almost always us)
     * @param sModes The modes set.
     * @return true if a method was called, false otherwise
     */
    protected boolean callUserModeDiscovered(final ClientInfo cClient, final String sModes) {
        return getCallbackManager().getCallbackType(UserModeDiscoveryListener.class).call(cClient, sModes);
    }

    /**
     * What does this IRCProcessor handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"MODE", "324", "221"};
    }
}
