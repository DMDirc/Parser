/*
 * Copyright (c) 2006-2017 DMDirc Developers
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
import com.dmdirc.parser.irc.IRCClientInfo;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.ProcessingManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Process001Test {

    @Mock private IRCParser parser;
    @Mock private ProcessingManager processingManager;
    @Mock private IRCClientInfo localClient;
    private Process001 processor;

    @Before
    public void setup() {
        when(parser.getLocalClient()).thenReturn(localClient);
        processor = new Process001(parser);
    }

    @Test
    public void testSets001Received() {
        when(localClient.isFake()).thenReturn(true);
        assumeFalse(parser.got001);
        processor.process(LocalDateTime.now(), "001", ":test.server.com", "001", "userName", "Hello!");
        assertTrue(parser.got001);
    }

    @Test
    public void testUpdatesServerName() {
        when(localClient.isFake()).thenReturn(true);
        processor.process(LocalDateTime.now(), "001", ":test.server.com", "001", "userName", "Hello!");
        verify(parser).updateServerName("test.server.com");
    }

    @Test
    public void testAddsLocalClientIfFake() {
        when(localClient.isFake()).thenReturn(true);
        processor.process(LocalDateTime.now(), "001", ":test.server.com", "001", "userName", "Hello!");
        verify(parser).addClient(localClient);
    }

    @Test
    public void testUpdatesLocalClientIfFake() {
        when(localClient.isFake()).thenReturn(true);
        processor.process(LocalDateTime.now(), "001", ":test.server.com", "001", "userName", "Hello!");
        verify(localClient).setUserBits(eq("userName"), eq(true), anyBoolean());
        verify(localClient).setFake(false);
    }

    @Test
    public void testIgnoresDuplicate001WithSameNick() {
        when(localClient.getNickname()).thenReturn("UsernaME");
        processor.process(LocalDateTime.now(), "001", ":test.server.com", "001", "userName", "Hello!");
        verify(parser, never()).addClient(any());
        verify(parser, never()).forceRemoveClient(any());
    }

    @Test
    public void testReplacesLocalUserOnDuplicate001() {
        when(localClient.getNickname()).thenReturn("UsernaME");
        processor.process(LocalDateTime.now(), "001", ":test.server.com", "001", "newName", "Hello!");
        verify(parser).forceRemoveClient(localClient);
        verify(localClient).setUserBits(eq("newName"), eq(true), anyBoolean());
        verify(parser).addClient(localClient);
    }

    @Test
    public void testRaisesFatalErrorIfDuplicate001CausesNicknameCollision() {
        setupLocalClientToTrackNicknameChanges();

        when(parser.isKnownClient("newName")).thenReturn(true);
        when(parser.getClient("newName")).thenReturn(mock(IRCClientInfo.class));

        processor.process(LocalDateTime.now(), "001", ":test.server.com", "001", "newName", "Hello!");

        final ArgumentCaptor<ParserError> errorCaptor = ArgumentCaptor.forClass(ParserError.class);
        verify(parser).callErrorInfo(errorCaptor.capture());
        assertEquals(ParserError.ERROR_FATAL, errorCaptor.getValue().getLevel());
    }

    /**
     * Ensures that the local client mock returns new nicknames after setUserBits is called.
     */
    private void setupLocalClientToTrackNicknameChanges() {
        final StringBuilder nickname = new StringBuilder("UserName");
        when(localClient.getNickname()).thenAnswer(invocation -> nickname.toString());
        doAnswer(invocation -> {
            nickname.setLength(0);
            nickname.append(invocation.<String>getArgument(0));
            return null;
        }).when(localClient).setUserBits(anyString(), eq(true), anyBoolean());
    }

}
