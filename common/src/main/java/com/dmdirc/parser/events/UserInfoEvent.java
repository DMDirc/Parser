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

package com.dmdirc.parser.events;

import com.dmdirc.parser.interfaces.ClientInfo;
import com.dmdirc.parser.interfaces.Parser;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Event raised when detailed user info has been received for a user.
 */
public class UserInfoEvent extends ParserEvent {

    private final ClientInfo client;
    private final Map<UserInfoType, String> info;

    public UserInfoEvent(final Parser parser, final LocalDateTime date, final ClientInfo client,
            final Map<UserInfoType, String> info) {
        super(parser, date);
        this.client = client;
        this.info = new EnumMap<>(info);
    }

    /**
     * Gets the client that the event is for.
     *
     * @return The client this event is for.
     */
    public ClientInfo getClient() {
        return client;
    }

    /**
     * Gets a specific piece of information about the user.
     *
     * @param type The type of information to return.
     * @return An optional containing the information, if it was provided.
     */
    public Optional<String> getInfo(final UserInfoType type) {
        return Optional.ofNullable(info.get(type));
    }

    /**
     * Gets all the pieces of information about the user.
     *
     * @return Map of all information about a user, may be empty
     */
    public Map<UserInfoType, String> getInfo() {
        return Collections.unmodifiableMap(info);
    }

    /**
     * The types of user info that may be returned by the server.
     */
    public enum UserInfoType {

        /** The user's full address. */
        ADDRESS,
        /** The user's real address. */
        REAL_ADDRESS,
        /** The user's real name. */
        REAL_NAME,
        /** The list of group chats the user is in. */
        GROUP_CHAT_LIST,
        /** The name of the server the user is connected to. */
        SERVER_NAME,
        /** Information about the server the user is connected to. */
        SERVER_INFO,
        /** The account the user is authenticated as. */
        ACCOUNT_NAME,
        /** The current idle time for the user. */
        IDLE_TIME,
        /** The time the user connected at. */
        CONNECTION_TIME,
        /** The user's current away message. */
        AWAY_MESSAGE,
        /** The user's server operator status. */
        SERVER_OPER,
        /** Information about the security of the user's connection to the server. */
        CONNECTION_SECURITY,
        /** The language the user uses. */
        LANGUAGE,

    }

}
