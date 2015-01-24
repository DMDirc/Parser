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

import dagger.Module;
import dagger.Provides;

/**
 * Dagger module that supplies a set of processors.
 */
@Module(library = true, complete = false)
public class ProcessorsModule {

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final Process001 processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final Process464 processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final Process004005 processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessAccount processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessAway processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessCap processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessInvite processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessJoin processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessKick processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessList processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessListModes processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessMessage processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessMode processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessMOTD processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessNames processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessNick processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessNickInUse processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessNoticeAuth processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessPart processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessQuit processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessTopic processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessWallops processor) {
        return processor;
    }

    @Provides(type = Provides.Type.SET)
    public IRCProcessor getProcessor(final ProcessWho processor) {
        return processor;
    }

}
