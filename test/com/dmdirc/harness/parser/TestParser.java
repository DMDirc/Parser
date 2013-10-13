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

package com.dmdirc.harness.parser;

import com.dmdirc.parser.common.ChildImplementations;
import com.dmdirc.parser.common.MyInfo;
import com.dmdirc.parser.common.QueuePriority;
import com.dmdirc.parser.interfaces.Parser;
import com.dmdirc.parser.irc.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

@ChildImplementations({
    IRCChannelClientInfo.class,
    IRCChannelInfo.class,
    IRCClientInfo.class
})
public class TestParser extends IRCParser implements Parser {

    public final List<String> sentLines = new ArrayList<>();

    public String nick = "nick";

    public String network = null;

    public TestParser() {
        super(buildURI());
        currentSocketState = SocketState.OPEN;
        setPingTimerFraction(10);
        setPingTimerInterval(60000);
    }

    public TestParser(MyInfo myDetails, URI address) {
        super(myDetails, address);
        currentSocketState = SocketState.OPEN;
        setPingTimerFraction(10);
        setPingTimerInterval(60000);
    }

    private static URI buildURI() {
        try {
            return new URI("irc://host:1234/");
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    @Override
    protected boolean doSendString(String line, QueuePriority priority, boolean fromParser) {
        sentLines.add(line);
        return true;
    }

    public String[] getLine(int index) {
        return tokeniseLine(sentLines.get(index));
    }

    public void injectLine(String line) {
        processLine(new IRCReader.ReadLine(line, IRCParser.tokeniseLine(line)));
    }

    public void injectConnectionStrings() {
        final String[] lines = new String[]{
            "NOTICE AUTH :Blah, blah",
            ":server 001 " + nick + " :Welcome to the Testing IRC Network, " + nick,
            ":server 002 " + nick + " :Your host is server.net, running version foo",
            ":server 003 " + nick + " :This server was created Sun Jan 6 2008 at 17:34:54 CET",
            ":server 004 " + nick + " server.net foo dioswkgxRXInP bRIeiklmnopstvrDcCNuMT bklov",
            ":server 005 " + nick + " WHOX WALLCHOPS WALLVOICES USERIP PREFIX=(ov)@+ " +
                    (network == null ? "" : "NETWORK=" + network + " ") +
                    ":are supported by this server",
            ":server 005 " + nick + " MAXNICKLEN=15 TOPICLEN=250 AWAYLEN=160 MODES=6 " +
                    "CHANMODES=bIeR,k,l,imnpstrDducCNMT :are supported by this server",
        };

        sendConnectionStrings();

        for (String line : lines) {
            injectLine(line);
        }

        sentLines.clear();
    }

    @Override
    protected void pingTimerTask(final Timer timer) {
        // Do nothing
    }

    @Override
    public void run() {
        injectConnectionStrings();
    }

    public void runSuper() {
        super.run();
    }

}
