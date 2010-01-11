
package com.dmdirc.parser.irc;

import java.util.Arrays;
import java.util.List;

/**
 * Groups of servers do things the same way as each other.
 */
public enum ServerTypeGroup {

    /** Freenode Group. */
    FREENODE("Freenode IRCDs", new ServerType[]{ServerType.HYPERION, ServerType.DANCER}), /** Charybdis Group. */
    CHARYBDIS("Charybdis-esque IRCDs", new ServerType[]{ServerType.IRCD_SEVEN, ServerType.CHARYBDIS}), /**
     * Group for ircds that put the channel owners in a list under raw 386
     * rather than as a channel user mode.
     */
    OWNER_386("Owner List", new ServerType[]{ServerType.SWIFTIRC, ServerType.AUSTHEX8}), /**
     * Group for ircds that put the protected users in a list under raw 388
     * rather than as a channel user mode.
     */
    PROTECTED_388("Protected List", new ServerType[]{ServerType.SWIFTIRC, ServerType.AUSTHEX8});

    /** Name of the group. */
    final String name;

    /** Group Members. */
    final List<ServerType> members;

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
