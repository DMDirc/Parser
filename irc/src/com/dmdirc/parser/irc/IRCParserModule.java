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

package com.dmdirc.parser.irc;

import com.dmdirc.parser.irc.processors.ProcessorsModule;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Dagger module for injecting the main IRC parser and its dependencies.
 */
@Module(includes = ProcessorsModule.class, injects = ProcessingManager.class)
public class IRCParserModule {

    private final IRCParser parser;
    private final PrefixModeManager prefixModeManager;
    private final ModeManager userModeManager;
    private final ModeManager chanModeManager;

    public IRCParserModule(final IRCParser parser, final PrefixModeManager prefixModeManager,
            final ModeManager userModeManager, final ModeManager chanModeManager) {
        // TODO: Make all of these things injected.
        this.parser = parser;
        this.prefixModeManager = prefixModeManager;
        this.userModeManager = userModeManager;
        this.chanModeManager = chanModeManager;
    }

    @Provides
    public IRCParser getParser() {
        return parser;
    }

    @Provides
    public PrefixModeManager getPrefixModeManager() {
        return prefixModeManager;
    }

    @Provides
    @Named("user")
    public ModeManager getUserModeManager() {
        return userModeManager;
    }

    @Provides
    @Named("channel")
    public ModeManager getChanModeManager() {
        return chanModeManager;
    }

}
