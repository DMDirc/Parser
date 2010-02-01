/*
 *  Copyright (c) 2006-2010 Chris Smith, Shane Mc Cormack, Gregory Holmes
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.dmdirc.parser.irc;

/**
 * This defines server types.
 * ServerTypes define a regex that will be matched against a lower-cased verison
 * of the input, an array of modes that only ops can request, and what they are
 * matching against.
 * Lowercaseing will be handled using javas built in toLowercase() rather than
 * usig the Server-Specific one.
 *
 * @author shane
 */
public enum ServerType {
    /** Unreal IRCD prior to version 4. */
    UNREAL("unreal", ".*unreal[^4-9].*"),
    /** Unreal IRCD after version 4 (inspircd based). */
    UNREAL4("unreal4", ".*unreal[4-9].*"),
    /** Bahamut. */
    BAHAMUT("bahamut", ".*bahamut.*"),
    /** Some IRCU Fork. */
    NEFARIOUS("nefarious", ".*nefarious.*"),
    /** Quakenets old IRCU Fork. */
    ASUKA("asuka", ".*asuka.*"),
    /** Quakenets current IRCU Fork. */
    SNIRCD("snircd", ".*snircd.*"),
    /** Windows IRCU-Based IRCD. */
    BIRCD("bircd", ".*beware.*"),
    /** Spanish IRCU Fork. */
    IRCHISPANO("irchispano", ".*u2\\.[0-9]+\\.H\\..*"),
    /** IRCU. */
    IRCU2("ircu", ".*u2\\.[0-9]+\\..*"),
    /** Some kind of IRCU Fork. */
    IRCU_GENERIC("ircu", ".*ircu.*"),
    /** Rizon. */
    IRCD_RIZON("ircd-rixon", ".*ircd-rizon.*", "eI"),
    /** Plexus. */
    PLEXUS("plexus", ".*plexus.*", "eI"),
    /** OFTCs hybrid fork. */
    OFTC_HYBRID("oftc-hybrid", ".*hybrid.*oftc.*", "eI"),
    /** hybrid7. */
    HYBRID7("hybrid7", ".*ircd.hybrid.*", "eI"),
    /** Older versions of Hybrid. */
    HYBRID("hybrid", ".*hybrid.*", "eI"),
    /** Charybdis */
    CHARYBDIS("charybdis", ".*charybdis.*", "eI"),
    /** Freenodes New IRCD. */
    IRCD_SEVEN("ircd-seven", ".*ircd-seven.*", "eI"),
    /** Freenodes Current IRCD. */
    HYPERION("hyperion", ".*hyperion.*", "eI"),
    /** Freenodes Old IRCD. */
    DANCER("dancer", ".*dancer.*", "eI"),
    /** Inspircd. */
    INSPIRCD("inspircd", ".*inspircd.*", "eIgb"),
    /** Ultimate IRCD. */
    ULTIMATEIRCD("ultimateircd", ".*ultimateircd.*"),
    /** Criten IRCD. */
    CRITENIRCD("critenircd", ".*critenircd.*"),
    /** fqircd. */
    FQIRCD("fqircd", ".*fqircd.*"),
    /** Microsoft's Conference Room "IRCD". */
    CONFERENCEROOM("conferenceroom", ".*conferenceroom.*"),
    /** AustHex8. */
    AUSTHEX8("austhex8", "running version 8.1.6$", "eI", MatchType.RAW002),
    /** AustHex custom IRCD. */
    AUSTHEX("austhex", ".*austhex.*"),
    /** AustIRC custom IRCD. */
    AUSTIRC("austirc", ".*austirc.*"),
    /** IRSEE custom IRCD. */
    IRSEE("irsee", ".*irsee.*"),
    /** Ratbox. */
    RATBOX("ratbox", ".*ratbox.*"),
    /** euircd. */
    EUIRCD("euircd", ".*euircd.*"),
    /** weircd. */
    WEIRCD("weircd", ".*weircd.*"),
    /** swiftirc's ircd. */
    SWIFTIRC("swiftirc", ".*swiftirc.*"),
    /** IRCNet. */
    IRCNET("ircnet", "ircnet", null, MatchType.NETWORK),
    /** Star Chat. */
    STARCHAT("starchat", "starchat", null, MatchType.NETWORK),
    /** Newer Bitlbee. */
    BITLBEE("bitlbee", "bitlbee", null, MatchType.NETWORK),
    /** Older Bitlbee. */
    BITLBEE_OLD("bitlbee", "bitlbee", null, MatchType.RAW003),
    /** Pastiche. */
    PASTICHE("bitlbee", "ircd-pastiche", null, MatchType.RAW002),
    /** Othernet. */
    OTHERNET("othernet", ".*Othernet.*"),
    /** Generic IRCD. */
    GENERIC("generic", "", null, MatchType.NEVER);

    /** Define what this ServerType should match on. */
    private enum MatchType {
        /** Match using the ircd version from 004. */
        VERSION,
        /** Match using the network name. */
        NETWORK,
        /** Match using raw 003. */
        RAW003,
        /** Match using raw 002. */
        RAW002,
        /** Never Match (Used by generic). */
        NEVER;
    }

    /** Type of for this ServerType. */
    private final String type;

    /** Regex for this ServerType. */
    private final String regex;

    /** String of chars that only ops can access. */
    private final String opOnly;

    /** What does this ServerType match? */
    private final MatchType matchType;

    /**
     * Create a new server type.
     *
     * @param type The name for this type.
     * @param regex The regex for this type.
     */
    ServerType(final String type, final String regex) {
        this(type, regex, null);
    }


    /**
     * Create a new server type.
     *
     * @param type The name for this type.
     * @param regex The regex for this type.
     * @param opOnly Any mode chars that are op-only.
     */
    ServerType(final String type, final String regex, final String opOnly) {
        this(type, regex, opOnly, MatchType.VERSION);
    }

    /**
     * Create a new server type.
     *
     * @param type The name for this type.
     * @param regex The regex for this type.
     * @param opOnly Any mode chars that are op-only.
     * @param matchType What information should we match on?
     */
    ServerType(final String type, final String regex, final String opOnly, final MatchType matchType) {
        this.type = type;
        this.regex = regex;
        this.opOnly = (opOnly == null) ? "" : opOnly;
        this.matchType = matchType;
    }

    /**
     * Get a string that defines what type this server is.
     *
     * @return String that defines what type this server is.
     */
    public String getType() {
        return type;
    }

    /**
     * Get the MatchType of this ServerType
     *
     * @return The MatchType of this ServerType
     */
    private MatchType getMatchType() {
        return matchType;
    }

    /**
     * Get the Regex used by this ServerType.
     *
     * @return The Regex used by this ServerType.
     */
    private String getRegex() {
        return regex;
    }

    /**
     * Get the modes that are op-only.
     *
     * @return the modes that are op-only.
     */
    public String getOpOnly() {
        return opOnly;
    }

    /**
     * Check if the given mode is op-only.
     *
     * @param mode
     * @return
     */
    public boolean isOpOnly(final char mode) {
        return (opOnly.indexOf(mode) != -1);
    }

    /**
     * Find the first ServerType that matches the given details.
     *
     * @param versionInput Version from 004
     * @param networkInput Network Name
     * @param raw003Input 003 line
     * @param raw002Input 002 line
     * @return The Server type that matches the given details.
     */
    public static ServerType findServerType(final String versionInput, final String networkInput, final String raw003Input, final String raw002Input) {
        final String version = (versionInput == null) ? "" : versionInput.toLowerCase();
        final String network = (networkInput == null) ? "" : networkInput.toLowerCase();
        final String raw003 = (raw003Input == null) ? "" : raw003Input.toLowerCase();
        final String raw002 = (raw002Input == null) ? "" : raw002Input.toLowerCase();

        for (ServerType type : ServerType.values()) {
            switch (type.getMatchType()) {
                case VERSION:
                    if (version.matches(type.getRegex())) { return type; }
                    break;
                case NETWORK:
                    if (network.matches(type.getRegex())) { return type; }
                    break;
                case RAW003:
                    if (raw003.matches(type.getRegex())) { return type; }
                    break;
                case RAW002:
                    if (raw002.matches(type.getRegex())) { return type; }
                    break;
                case NEVER:
                    break;
                default:
                    /* Won't happen. */
                    break;
            }
        }

        // Return Generic IRCD.
        return ServerType.GENERIC;
    }
}
