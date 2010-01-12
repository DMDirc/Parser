/*
 * Copyright (c) 2006-2010 Chris Smith, Shane Mc Cormack, Gregory Holmes
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
import com.dmdirc.parser.common.CallbackNotFoundException;
import com.dmdirc.parser.interfaces.callbacks.ChannelTopicListener;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ProcessTopicTest {
    
    @Test
    public void testBasicTopic() throws CallbackNotFoundException {
        final TestParser parser = new TestParser();
        final ChannelTopicListener test = mock(ChannelTopicListener.class);
        parser.injectConnectionStrings();
        parser.getCallbackManager().addCallback(ChannelTopicListener.class, test);
        
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":server 332 nick #DMDirc_testing :This be a topic");
        parser.injectLine(":server 333 nick #DMDirc_testing Q 1207350306");

        verify(test).onChannelTopic(same(parser), same(parser.getChannel("#DMDirc_testing")),
                eq(true));

        assertEquals("#DMDirc_testing", parser.getChannel("#DMDirc_testing").getName());
        assertEquals("This be a topic", parser.getChannel("#DMDirc_testing").getTopic());
        assertEquals("Q", parser.getChannel("#DMDirc_testing").getTopicSetter());
        assertEquals(1207350306l, parser.getChannel("#DMDirc_testing").getTopicTime());
    }
    
    @Test
    public void testTopicChange() throws CallbackNotFoundException {
        final TestParser parser = new TestParser();
        final ChannelTopicListener test = mock(ChannelTopicListener.class);
        parser.injectConnectionStrings();
        
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":server 332 nick #DMDirc_testing :This be a topic");
        parser.injectLine(":server 333 nick #DMDirc_testing Q 1207350306");
        
        parser.getCallbackManager().addCallback(ChannelTopicListener.class, test);
        
        parser.injectLine(":foobar TOPIC #DMDirc_testing :New topic here");

        verify(test).onChannelTopic(same(parser), same(parser.getChannel("#DMDirc_testing")),
                eq(false));
        
        assertEquals("#DMDirc_testing", parser.getChannel("#DMDirc_testing").getName());
        assertEquals("New topic here", parser.getChannel("#DMDirc_testing").getTopic());
        assertEquals("foobar", parser.getChannel("#DMDirc_testing").getTopicSetter());
        assertTrue(1207350306l < parser.getChannel("#DMDirc_testing").getTopicTime());
    }    

}
