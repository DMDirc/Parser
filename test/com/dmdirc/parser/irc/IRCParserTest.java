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

import com.dmdirc.parser.common.MyInfo;
import com.dmdirc.parser.common.ParserError;
import com.dmdirc.harness.parser.TestParser;
import com.dmdirc.parser.interfaces.Parser;
import com.dmdirc.parser.interfaces.callbacks.AuthNoticeListener;
import com.dmdirc.parser.common.CallbackNotFoundException;
import com.dmdirc.parser.interfaces.callbacks.CallbackInterface;
import com.dmdirc.parser.interfaces.callbacks.ChannelKickListener;
import com.dmdirc.parser.interfaces.callbacks.ConnectErrorListener;
import com.dmdirc.parser.interfaces.callbacks.ErrorInfoListener;
import com.dmdirc.parser.interfaces.callbacks.NumericListener;
import com.dmdirc.parser.interfaces.callbacks.ServerErrorListener;
import com.dmdirc.parser.interfaces.callbacks.ServerReadyListener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;

import javax.net.ssl.TrustManager;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class IRCParserTest {

    private static interface TestCallback extends CallbackInterface { }

    @Test(expected=CallbackNotFoundException.class)
    public void testIssue42() {
        // Invalid callback names are silently ignored instead of raising exceptions
        final IRCParser myParser = new IRCParser();
        myParser.getCallbackManager().addCallback(TestCallback.class, mock(TestCallback.class));
    }

    @Test
    public void testIssue1674() {
        // parser nick change error with dual 001
        final ErrorInfoListener error = mock(ErrorInfoListener.class);

        final TestParser myParser = new TestParser();
        myParser.getCallbackManager().addCallback(ErrorInfoListener.class, error);
        myParser.injectConnectionStrings();
        myParser.nick = "nick2";
        myParser.injectConnectionStrings();
        myParser.injectLine(":nick2!ident@host NICK :nick");

        verify(error, never()).onErrorInfo(same(myParser), (Date) anyObject(),
                (ParserError) anyObject());
    }

    @Test
    public void testTokeniser() {
        final String line1 = "a b c d e";
        final String line2 = "a b c :d e";
        final String line3 = ":a b:c :d e";
        final String line4 = null;

        final String[] res1 = IRCParser.tokeniseLine(line1);
        final String[] res2 = IRCParser.tokeniseLine(line2);
        final String[] res3 = IRCParser.tokeniseLine(line3);
        final String[] res4 = IRCParser.tokeniseLine(line4);

        assertTrue(Arrays.equals(res1, new String[]{"a", "b", "c", "d", "e"}));
        assertTrue(Arrays.equals(res2, new String[]{"a", "b", "c", "d e"}));
        assertTrue(Arrays.equals(res3, new String[]{":a", "b:c", "d e"}));
        assertTrue(Arrays.equals(res4, new String[]{""}));
    }

    @Test
    public void testSendConnectionStrings1() throws URISyntaxException {
        final MyInfo myInfo = new MyInfo();
        myInfo.setNickname("Nickname");
        myInfo.setRealname("Real name");
        myInfo.setUsername("Username");

        final TestParser parser = new TestParser(myInfo, new URI("irc://irc.testing.dmdirc:6667/"));
        parser.sendConnectionStrings();

        assertEquals(2, parser.sentLines.size());

        assertTrue("Should send nickname line",
                Arrays.equals(parser.getLine(0), new String[]{"NICK", "Nickname"}));

        final String[] userParts = parser.getLine(1);
        assertEquals("First token should be USER", "USER", userParts[0]);
        assertEquals("USER should contain username", myInfo.getUsername().toLowerCase(),
                userParts[1].toLowerCase());
        assertEquals("USER should contain server name", "irc.testing.dmdirc", userParts[3]);
        assertEquals("USER should contain real name", "Real name", userParts[4]);
    }

    @Test
    public void testSendConnectionStrings2() throws URISyntaxException {
        final MyInfo myInfo = new MyInfo();
        myInfo.setNickname("Nickname");
        myInfo.setRealname("Real name");
        myInfo.setUsername("Username");

        final TestParser parser = new TestParser(myInfo, new URI("irc://password@irc.testing.dmdirc:6667/"));
        parser.sendConnectionStrings();

        assertEquals(3, parser.sentLines.size());

        assertTrue("Should send password line",
                Arrays.equals(parser.getLine(0), new String[]{"PASS", "password"}));
    }

    @Test
    public void testPingPong() {
        final TestParser parser = new TestParser();

        parser.injectLine("PING :flubadee7291");

        assertTrue("Should reply to PINGs with PONGs",
                Arrays.equals(parser.getLine(0), new String[]{"PONG", "flubadee7291"}));
    }

    @Test
    public void testError() throws CallbackNotFoundException {
        final ServerErrorListener test = mock(ServerErrorListener.class);

        final TestParser parser = new TestParser();
        parser.getCallbackManager().addCallback(ServerErrorListener.class, test);
        parser.injectLine("ERROR :You smell of cheese");

        verify(test).onServerError(same(parser), (Date) anyObject(), eq("You smell of cheese"));
    }

    @Test
    public void testAuthNotice() throws CallbackNotFoundException {
        final AuthNoticeListener test = mock(AuthNoticeListener.class);
        final TestParser parser = new TestParser();
        parser.getCallbackManager().addCallback(AuthNoticeListener.class, test);
        parser.sendConnectionStrings();

        parser.injectLine("NOTICE AUTH :Random auth notice?");
        verify(test).onNoticeAuth(same(parser), (Date) anyObject(), eq("Random auth notice?"));
    }

    @Test
    public void testAuthNoticeTwenty() throws CallbackNotFoundException {
        final AuthNoticeListener test = mock(AuthNoticeListener.class);
        final TestParser parser = new TestParser();
        parser.getCallbackManager().addCallback(AuthNoticeListener.class, test);
        parser.sendConnectionStrings();

        parser.injectLine(":us.ircnet.org 020 * :Stupid notice");
        verify(test).onNoticeAuth(same(parser), (Date) anyObject(), eq("Stupid notice"));
    }

    @Test
    public void testPre001NickChange() throws CallbackNotFoundException {
        final AuthNoticeListener test = mock(AuthNoticeListener.class);
        final TestParser parser = new TestParser();
        parser.getCallbackManager().addCallback(AuthNoticeListener.class, test);
        parser.sendConnectionStrings();
        parser.injectLine(":chris!@ NICK :user2");

        verify(test, never()).onNoticeAuth((Parser) anyObject(), (Date) anyObject(), anyString());
    }

    @Test
    public void testNumeric() throws CallbackNotFoundException {
        final NumericListener test = mock(NumericListener.class);
        final TestParser parser = new TestParser();
        parser.getCallbackManager().addCallback(NumericListener.class, test);

        parser.injectLine(":server 001 nick :Hi there, nick");

        verify(test).onNumeric(same(parser), (Date) anyObject(), eq(1),
                eq(new String[]{":server", "001", "nick", "Hi there, nick"}));
    }

    @Test
    public void testServerReady() throws CallbackNotFoundException {
        final ServerReadyListener test = mock(ServerReadyListener.class);
        final TestParser parser = new TestParser();
        parser.getCallbackManager().addCallback(ServerReadyListener.class, test);

        final String[] strings = {
            "NOTICE AUTH :Blah, blah",
            ":server 020 * :Blah! Blah!",
            ":server 001 nick :Welcome to the Testing IRC Network, nick",
            ":server 002 nick :Your host is server.net, running version foo",
            "NOTICE AUTH :I'm a retarded server",
            ":server 003 nick :This server was created Sun Jan 6 2008 at 17:34:54 CET",
            ":server 004 nick server.net foo dioswkgxRXInP biklmnopstvrDcCNuMT bklov",
            ":server 005 nick WHOX WALLCHOPS WALLVOICES USERIP :are supported by this server",
            ":server 005 nick MAXNICKLEN=15 TOPICLEN=250 AWAYLEN=160 :are supported " +
                    "by this server",
            ":server 375 nick :zomg, motd!",
        };

        for (String string : strings) {
            verify(test, never()).onServerReady((Parser) anyObject(), (Date) anyObject());
            parser.injectLine(string);
        }

        verify(test).onServerReady(same(parser), (Date) anyObject());
    }

    @Test
    public void test005Parsing() {
        final TestParser parser = new TestParser();

        final String[] strings = {
            ":server 001 nick :Welcome to the Testing IRC Network, nick",
            ":server 002 nick :Your host is server.net, running version foo",
            ":server 003 nick :This server was created Sun Jan 6 2008 at 17:34:54 CET",
            ":server 004 nick server.net foo dioswkgxRXInP biklmnopstvrDcCNuMT bklov",
            ":server 005 nick WHOX WALLCHOPS WALLVOICES NETWORK=moo :are supported by" +
                    " this server",
            ":server 005 nick MAXNICKLEN=15 MAXLIST=b:10,e:22,I:45 :are supported by" +
                    " this server",
            ":server 375 nick :zomg, motd!",
        };

        for (String string : strings) {
            parser.injectLine(string);
        }

        assertEquals(10, parser.getMaxListModes('b'));
        assertEquals(22, parser.getMaxListModes('e'));
        assertEquals(45, parser.getMaxListModes('I'));
        assertEquals("getMaxListModes should return 0 for unknowns;", 0,
                parser.getMaxListModes('z'));
        assertEquals("moo", parser.getNetworkName());
        assertEquals("server", parser.getServerName());
    }

    @Test
    public void testBindIP() {
        final TestParser parser = new TestParser();

        parser.setBindIP("abc.def.ghi.123");
        assertEquals("abc.def.ghi.123", parser.getBindIP());
    }

    @Test
    public void testCreateFake() {
        final TestParser parser = new TestParser();

        parser.setCreateFake(false);
        assertFalse(parser.getCreateFake());
        parser.setCreateFake(true);
        assertTrue(parser.getCreateFake());
    }

    @Test
    public void testAutoListMode() {
        final TestParser parser = new TestParser();

        parser.setAutoListMode(false);
        assertFalse(parser.getAutoListMode());
        parser.setAutoListMode(true);
        assertTrue(parser.getAutoListMode());
    }

    @Test
    public void testRemoveAfterCallback() {
        final TestParser parser = new TestParser();

        parser.setRemoveAfterCallback(false);
        assertFalse(parser.getRemoveAfterCallback());
        parser.setRemoveAfterCallback(true);
        assertTrue(parser.getRemoveAfterCallback());
    }

    @Test
    public void testAddLastLine() {
        final TestParser parser = new TestParser();

        parser.setAddLastLine(false);
        assertFalse(parser.getAddLastLine());
        parser.setAddLastLine(true);
        assertTrue(parser.getAddLastLine());
    }

    @Test
    public void testDisconnectOnFatal() {
        final TestParser parser = new TestParser();

        parser.setDisconnectOnFatal(false);
        assertFalse(parser.getDisconnectOnFatal());
        parser.setDisconnectOnFatal(true);
        assertTrue(parser.getDisconnectOnFatal());
    }

    @Test
    public void testTrustManager() {
        final TestParser parser = new TestParser();

        assertTrue(Arrays.equals(parser.getDefaultTrustManager(), parser.getTrustManager()));

        parser.setTrustManagers(new TrustManager[0]);

        assertTrue(Arrays.equals(new TrustManager[0], parser.getTrustManager()));
    }

    @Test
    public void testGetParam() {
        assertEquals("abc def", TestParser.getParam("foo :abc def"));
        assertEquals("bar :abc def", TestParser.getParam("foo :bar :abc def"));
        assertEquals("abc def", TestParser.getParam("abc def"));
    }
    
    @Test
    public void testKick() throws CallbackNotFoundException {
        final TestParser parser = new TestParser();
        final ChannelKickListener ick = mock(ChannelKickListener.class);
        parser.injectConnectionStrings();

        parser.injectLine(":nick JOIN #D");
        parser.getCallbackManager().addCallback(ChannelKickListener.class, ick, "#D");
        parser.injectLine(":bar!me@moo KICK #D nick :Bye!");

        verify(ick).onChannelKick(same(parser), (Date) anyObject(), (IRCChannelInfo) anyObject(),
                (IRCChannelClientInfo) anyObject(), (IRCChannelClientInfo) anyObject(),
                anyString(), anyString());
    }
    
    @Test
    public void testIllegalPort2() throws URISyntaxException {
        final TestParser tp = new TestParser(new MyInfo(), new URI("irc://127.0.0.1:65570/"));
        final ConnectErrorListener tiei = mock(ConnectErrorListener.class);
        tp.getCallbackManager().addCallback(ConnectErrorListener.class, tiei);
        tp.runSuper();
        verify(tiei).onConnectError(same(tp), (Date) anyObject(), (ParserError) anyObject());
    }

}
