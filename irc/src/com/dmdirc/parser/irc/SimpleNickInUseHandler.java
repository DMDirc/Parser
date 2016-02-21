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

package com.dmdirc.parser.irc;

import com.dmdirc.parser.events.DebugInfoEvent;
import com.dmdirc.parser.events.NickInUseEvent;
import com.dmdirc.parser.interfaces.Parser;

import java.time.LocalDateTime;

import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;

/**
 * Simple nick in use handler that tries the alternative nickname, then prepends a character until
 * it gets a nickname.
 */
@Listener(references = References.Strong)
public class SimpleNickInUseHandler {

    private final String altNickname;
    private final char prependChar;
    private boolean triedAlt;

    SimpleNickInUseHandler(final String altNickname, final char prependChar) {
        this.altNickname = altNickname;
        this.prependChar = prependChar;
    }

    @Handler
    public void onNickInUse(final NickInUseEvent event) {
        final IRCParser parser = (IRCParser) event.getParser();
        callDebugInfo(parser, IRCParser.DEBUG_INFO, "No Nick in use Handler.");
        if (!parser.got001) {
            callDebugInfo(parser, IRCParser.DEBUG_INFO, "Using inbuilt handler");
            // If this is before 001 we will try and get a nickname, else we will leave the
            // nick as-is
            if (triedAlt) {
                final String magicAltNick = prependChar + parser.getMyInfo().getNickname();
                if (parser.getStringConverter().equalsIgnoreCase(parser.thinkNickname,
                        altNickname)
                        && !altNickname.equalsIgnoreCase(magicAltNick)) {
                    parser.thinkNickname = parser.getMyInfo().getNickname();
                }
                parser.getLocalClient().setNickname(prependChar + parser.thinkNickname);
            } else {
                parser.getLocalClient().setNickname(altNickname);
                triedAlt = true;
            }
        }
    }

    private void callDebugInfo(final Parser parser, final int level, final String data) {
        parser.getCallbackManager().publish(
                new DebugInfoEvent(parser, LocalDateTime.now(), level, data));
    }

    /**
     * Installs a new {@link SimpleNickInUseHandler} on the given parser.
     *
     * @param parser The parser to install the handler on.
     * @param altNickname The alternate nickname to try using.
     * @param prependChar The character to prepend if the alt nickname is in use.
     */
    public static void install(
            final Parser parser, final String altNickname, final char prependChar) {
        parser.getCallbackManager().subscribe(new SimpleNickInUseHandler(altNickname, prependChar));
    }

}
