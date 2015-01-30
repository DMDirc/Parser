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

import com.dmdirc.parser.common.CallbackManager;
import com.dmdirc.parser.events.NumericEvent;
import com.dmdirc.parser.events.UserInfoEvent;
import com.dmdirc.parser.events.UserInfoEvent.UserInfoType;
import com.dmdirc.parser.interfaces.Parser;

import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.engio.mbassy.listener.Handler;

/**
 * Monitors for whois responses and raises a {@link UserInfoEvent} with the results.
 */
public class WhoisResponseHandler {

    private final Parser parser;
    private final CallbackManager manager;

    private final Map<UserInfoType, String> info = new EnumMap<>(UserInfoType.class);

    @Nullable private String client;

    public WhoisResponseHandler(final Parser parser, final CallbackManager manager) {
        this.parser = parser;
        this.manager = manager;
    }

    public void start() {
        manager.subscribe(this);
    }

    public void stop() {
        manager.unsubscribe(this);
    }

    @Handler
    void handleNumericEvent(final NumericEvent event) {
        if (event.getNumeric() == 311) {
            // RPL_WHOISUSER
            client = event.getToken()[3];
            info.clear();
        }

        if (event.getNumeric() == 318 && client != null) {
            // RPL_ENDOFWHOIS
            sendEvent();
            client = null;
        }

        if (client != null && event.getToken().length > 4 && event.getToken()[3].equals(client)) {
            handleWhoisResponse(event.getNumeric(), event.getToken());
        }
    }

    private void handleWhoisResponse(final int numeric, final String... tokens) {
        switch (numeric) {
            case 311:
                // :server 311 DMDirc User ~Ident host.dmdirc.com * :Real name
                info.put(UserInfoType.ADDRESS, tokens[3] + '!' + tokens[4] + '@' + tokens[5]);
                info.put(UserInfoType.REAL_NAME, tokens[7]);
                break;

            case 319:
                // :server 319 DMDirc User :@#channel1 +#channel2 ...
                info.put(UserInfoType.GROUP_CHAT_LIST, tokens[4]);
                break;

            case 312:
                // :server 312 DMDirc User *.quakenet.org :QuakeNet IRC Server
                info.put(UserInfoType.SERVER_NAME, tokens[4]);
                info.put(UserInfoType.SERVER_INFO, tokens[5]);
                break;

            case 330:
                // :server 330 DMDirc User Account :is authed as
                info.put(UserInfoType.ACCOUNT_NAME, tokens[4]);
                break;

            case 317:
                // :server 317 DMDirc User 305 1422561556 :seconds idle, signon time
                info.put(UserInfoType.IDLE_TIME, tokens[4]);
                info.put(UserInfoType.CONNECTION_TIME, tokens[5]);
                break;
        }
    }

    private void sendEvent() {
        manager.publish(new UserInfoEvent(parser, new Date(), parser.getClient(client), info));
    }

}
