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

import java.util.Arrays;
import java.util.List;

/**
 * Groups of servers do things the same way as each other.
 */
public enum ServerTypeGroup {

    /**
     * Freenode Group.
     */
    FREENODE("Freenode IRCDs", new ServerType[]{ServerType.HYPERION, ServerType.DANCER}),
    /**
     * Charybdis Group.
     */
    CHARYBDIS("Charybdis-esque IRCDs", new ServerType[]{ServerType.IRCD_SEVEN, ServerType.CHARYBDIS}),
    /**
     * Group for ircds that put the channel owners in a list under raw 386
     * rather than as a channel user mode.
     */
    OWNER_386("Owner List", new ServerType[]{ServerType.SWIFTIRC, ServerType.AUSTHEX8}),
    /**
     * Group for ircds that put the protected users in a list under raw 388
     * rather than as a channel user mode.
     */
    PROTECTED_388("Protected List", new ServerType[]{ServerType.SWIFTIRC, ServerType.AUSTHEX8}),
    /**
     * Group for ircds that require list modes to be sent one at a time.
     */
    SINGLE_LISTMODE("Single List Modes", new ServerType[]{ServerType.EUIRCD, ServerType.UNREAL, ServerType.IRSEE});

    /** Name of the group. */
    final String name;
    /** Group Members. */
    final List<ServerType> members;

    /**
     * Create a new ServerTypeGroup.
     *
     * @param name Name of this group.
     * @param members Members of this group.
     */
    ServerTypeGroup(final String name, final ServerType[] members) {
        this.name = name;
        this.members = Arrays.asList(members);
    }

    /**
     * Get the members of this group.
     *
     * @return The members of this group.
     */
    public List<ServerType> getMembers() {
        return members;
    }

    /**
     * Get the name of this group.
     *
     * @return The name of this group.
     */
    public String getName() {
        return name;
    }

    /**
     * Check if the given ServerType is a member of this group.
     *
     * @param type The type to check.
     * @return True of the given ServerType is a member of this group.
     */
    public boolean isMember(final ServerType type) {
        return members.contains(type);
    }
}
