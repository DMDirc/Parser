/*
 * Copyright (c) 2006-2011 DMDirc Developers
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

import com.dmdirc.harness.parser.TestParser;

import com.dmdirc.parser.interfaces.ChannelInfo;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChannelInfoTest {

    final IRCChannelInfo ci = new IRCChannelInfo(null, "name");

    @Test
    public void testGetName() {
        assertEquals("name", ci.getName());
    }

    @Test
    public void testAddingNames() {
        assertTrue(ci.isAddingNames());

        ci.setAddingNames(false);

        assertFalse(ci.isAddingNames());
    }

    @Test
    public void testCreateTime() {
        ci.setCreateTime(12345L);

        assertEquals(12345L, ci.getCreateTime());
    }

    @Test
    public void testTopicTime() {
        ci.setTopicTime(12345L);

        assertEquals(12345L, ci.getTopicTime());
    }

    @Test
    public void testTopic() {
        ci.setInternalTopic("abcdef");

        assertEquals("abcdef", ci.getTopic());
    }

    @Test
    public void testSendMessage() {
        final TestParser parser = new TestParser();
        getChannel(parser).sendMessage("hello");

        assertEquals("PRIVMSG #DMDirc_testing :hello", parser.sentLines.get(0));
    }

    @Test
    public void testSendNotice() {
        final TestParser parser = new TestParser();
        getChannel(parser).sendNotice("hello");

        assertEquals("NOTICE #DMDirc_testing :hello", parser.sentLines.get(0));
    }

    @Test
    public void testSendCTCP() {
        final TestParser parser = new TestParser();
        getChannel(parser).sendCTCP("type", "hello");

        assertEquals("PRIVMSG #DMDirc_testing :" + ((char) 1) + "TYPE hello" + ((char) 1),
                parser.sentLines.get(0));
    }

    @Test
    public void testSendCTCPEmpty() {
        final TestParser parser = new TestParser();
        getChannel(parser).sendCTCP("type", "");

        assertEquals("PRIVMSG #DMDirc_testing :" + ((char) 1) + "TYPE" + ((char) 1),
                parser.sentLines.get(0));
    }

    @Test
    public void testSendAction() {
        final TestParser parser = new TestParser();
        getChannel(parser).sendAction("moo");

        assertEquals("PRIVMSG #DMDirc_testing :" + ((char) 1) + "ACTION moo" + ((char) 1),
                parser.sentLines.get(0));
    }

    @Test
    public void testSendCTCPReply() {
        final TestParser parser = new TestParser();
        getChannel(parser).sendCTCPReply("type", "moo");

        assertEquals("NOTICE #DMDirc_testing :" + ((char) 1) + "TYPE moo" + ((char) 1),
                parser.sentLines.get(0));
    }

    @Test
    public void testSendCTCPReplyEmpty() {
        final TestParser parser = new TestParser();
        getChannel(parser).sendCTCPReply("type", "");

        assertEquals("NOTICE #DMDirc_testing :" + ((char) 1) + "TYPE" + ((char) 1),
                parser.sentLines.get(0));
    }

    @Test
    public void testSendEmptyMessages() {
        final TestParser parser = new TestParser();
        final IRCChannelInfo info = getChannel(parser);

        info.sendAction("");
        info.sendCTCP("", "");
        info.sendCTCPReply("", "");
        info.sendMessage("");
        info.sendNotice("");

        assertEquals(0, parser.sentLines.size());
    }

    @Test
    public void testGetSetParamMode() {
        final TestParser parser = new TestParser();
        final ChannelInfo info = getChannel(parser);
        parser.injectLine(":server 324 nick #DMDirc_testing +k lalala");
        parser.sentLines.clear();

        assertEquals("lalala", info.getMode('k'));
        assertEquals("", info.getMode('z'));

        parser.injectLine(":server MODE #DMDirc_testing -k *");

        assertEquals("", info.getMode('k'));
    }

    @Test
    public void testModeSendFull() {
        final TestParser parser = new TestParser();
        final ChannelInfo info = getChannel(parser);

        parser.sentLines.clear();
        info.alterMode(true, 'i', null);
        info.alterMode(true, 'm', null);
        info.alterMode(true, 'n', null);
        info.alterMode(true, 'p', null);
        info.alterMode(true, 't', null);
        info.alterMode(true, 'r', null);

        assertEquals("Parser must send modes as soon as the max number is reached",
                1, parser.sentLines.size());
        final String modes = getModes(parser.sentLines.get(0));

        assertTrue(modes.indexOf('i') > -1);
        assertTrue(modes.indexOf('m') > -1);
        assertTrue(modes.indexOf('n') > -1);
        assertTrue(modes.indexOf('p') > -1);
        assertTrue(modes.indexOf('t') > -1);
        assertTrue(modes.indexOf('r') > -1);
    }

    @Test
    public void testModeSendExtra() {
        final TestParser parser = new TestParser();
        final ChannelInfo info = getChannel(parser);

        parser.sentLines.clear();
        info.alterMode(true, 'i', null);
        info.alterMode(true, 'm', null);
        info.alterMode(true, 'n', null);
        info.alterMode(true, 'p', null);
        info.alterMode(true, 't', null);
        info.alterMode(true, 'r', null);
        info.alterMode(true, 'N', null);
        info.flushModes();

        assertEquals("sendModes must send modes",
                2, parser.sentLines.size());

        final String modes = getModes(parser.sentLines.get(0))
                + getModes(parser.sentLines.get(1));

        assertTrue(modes.indexOf('i') > -1);
        assertTrue(modes.indexOf('m') > -1);
        assertTrue(modes.indexOf('n') > -1);
        assertTrue(modes.indexOf('p') > -1);
        assertTrue(modes.indexOf('t') > -1);
        assertTrue(modes.indexOf('r') > -1);
        assertTrue(modes.indexOf('N') > -1);
    }

    @Test
    public void testModeSendOptimisation1() {
        final TestParser parser = new TestParser();
        final ChannelInfo info = getChannel(parser);

        parser.sentLines.clear();
        info.alterMode(true, 'i', null);
        info.alterMode(true, 'm', null);
        info.alterMode(true, 'n', null);
        info.alterMode(true, 'n', null);
        info.alterMode(false, 'i', null);
        info.flushModes();

        assertEquals("sendModes must send modes in one go",
                1, parser.sentLines.size());

        final String modes = getModes(parser.sentLines.get(0));

        assertEquals("Setting a negative mode should cancel a positive one",
                -1, modes.indexOf('i'));

        assertTrue(modes.indexOf('m') > -1);
    }

    @Test
    public void testModeSendOptimisation2() {
        final TestParser parser = new TestParser();
        final ChannelInfo info = getChannel(parser);

        parser.sentLines.clear();
        info.alterMode(true, 'm', null);
        info.alterMode(true, 'n', null);
        info.alterMode(true, 'n', null);
        info.flushModes();

        assertEquals("sendModes must send modes in one go",
                1, parser.sentLines.size());

        final String modes = getModes(parser.sentLines.get(0));

        assertEquals("Setting a mode twice should have no effect",
                modes.indexOf('n'), modes.lastIndexOf('n'));

        assertTrue(modes.indexOf('m') > -1);
    }

    @Test
    public void testModeUnsetKey() {
        final TestParser parser = new TestParser();
        final ChannelInfo info = getChannel(parser);
        parser.injectLine(":server 324 nick #DMDirc_testing +k lalala");
        parser.sentLines.clear();

        info.alterMode(true, 'k', "foobar");
        info.flushModes();

        assertEquals("sendModes must send modes in one go",
                1, parser.sentLines.size());
        assertEquals("Setting +k must set -k first",
                "-k+k lalala foobar", getModes(parser.sentLines.get(0)));
    }

    @Test
    public void testIssue1410() {
        final TestParser parser = new TestParser();

        parser.injectConnectionStrings();
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":foo!bar@baz JOIN #DMDirc_testing");
        parser.injectLine(":flub!floo@fleeee JOIN #DMDirc_testing");

        assertNotSame(parser.getChannel("#DMDirc_testing").getChannelClients(),
                parser.getChannel("#DMDirc_testing").getChannelClients());
        assertEquals(parser.getChannel("#DMDirc_testing").getChannelClients(),
                parser.getChannel("#DMDirc_testing").getChannelClients());
    }

    @Test @Ignore
    public void testModeUnsetKeyMultiple() {
        final TestParser parser = new TestParser();
        final ChannelInfo info = getChannel(parser);
        parser.injectLine(":server 324 nick #DMDirc_testing +k lalala");
        parser.sentLines.clear();

        info.alterMode(true, 'k', "foobar");
        info.alterMode(true, 'k', "blahblah");
        info.alterMode(true, 'k', "unittest");
        info.flushModes();

        assertEquals("sendModes must send modes in one go",
                1, parser.sentLines.size());
        assertEquals("Setting a mode multiple times should have no effect",
                "-k+k lalala unittest", getModes(parser.sentLines.get(0)));
    }

    @Test @Ignore
    public void testModeUnsetLimitMultiple() {
        final TestParser parser = new TestParser();
        final ChannelInfo info = getChannel(parser);
        parser.injectLine(":server 324 nick #DMDirc_testing +l 73");
        parser.sentLines.clear();

        info.alterMode(true, 'l', "74");
        info.alterMode(true, 'l', "75");
        info.alterMode(true, 'l', "76");
        info.flushModes();

        assertEquals("sendModes must send modes in one go",
                1, parser.sentLines.size());
        assertEquals("Setting a mode multiple times should have no effect",
                "+l 76", getModes(parser.sentLines.get(0)));
    }

    private String getModes(final String line) {
        final String res = line.substring("MODE #DMDirc_testing ".length());

        if (res.charAt(0) == '+') {
            return res.substring(1);
        }

        return res;
    }

    private IRCChannelInfo getChannel(final TestParser parser) {
        parser.injectConnectionStrings();
        parser.injectLine(":nick JOIN #DMDirc_testing");

        parser.sentLines.clear();
        return parser.getChannels().iterator().next();
    }

}