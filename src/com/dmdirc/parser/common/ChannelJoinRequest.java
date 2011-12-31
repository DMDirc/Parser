/*
 * Copyright (c) 2006-2012 DMDirc Developers
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

package com.dmdirc.parser.common;

/**
 * Describes the information needed to try and join a channel.
 *
 * @author chris
 * @since 0.6.4
 */
public class ChannelJoinRequest {

    /** The name of the channel to join (required). */
    private final String name;
    /** The name of the password to use (optional). */
    private final String password;

    /**
     * Creates a new ChannelJoinRequest for a password-less channel with
     * the specified name.
     *
     * @param name The name of the channel to join
     */
    public ChannelJoinRequest(final String name) {
        this(name, null);
    }

    /**
     * Creates a new ChannelJoinRequest for a channel with the specified
     * password.
     *
     * @param name The name of the channel to join
     * @param password The password to use
     */
    public ChannelJoinRequest(final String name, final String password) {
        this.name = name;
        this.password = password;
    }

    /**
     * Retrieves the name of the channel this request will try to join.
     *
     * @return The name of the channel in this request
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the password which will be used to try to join the channel.
     *
     * @return The password to use, or null if none specified
     */
    public String getPassword() {
        return password;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof ChannelJoinRequest
                && ((ChannelJoinRequest) obj).getName().equals(name)
                && ((((ChannelJoinRequest) obj).getPassword() == null
                ? password == null : ((ChannelJoinRequest) obj).getPassword().equals(password)));
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int hash = name != null ? name.hashCode() : 0;
        hash = 13 * hash + (password != null ? password.hashCode() : 0);
        return hash;
    }

}
