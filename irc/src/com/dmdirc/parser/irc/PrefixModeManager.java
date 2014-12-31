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

/**
 * Handles prefix modes (those that can be applied to a user in a channel, such as +ohv).
 */
public class PrefixModeManager {

    /** All known modes, in increasing order of importance. */
    private final ModeManager modes = new ModeManager();
    /** All known prefixes, in increasing order of importance. */
    private String prefixes = "";

    /**
     * Resets the state of this manager, clearing all known modes.
     */
    public void clear() {
        setModes("", "");
    }

    /**
     * Replaces all existing modes with the specified ones.
     *
     * @param modes The new modes, in increasing order of importance.
     * @param prefixes The corresponding new prefixes, in increasing order of importance.
     */
    public void setModes(final String modes, final String prefixes) {
        this.modes.set(modes);
        this.prefixes = prefixes;
    }

    /**
     * Determines if the specified character is a prefix mode (e.g. 'o', 'v').
     *
     * @param mode The mode to be tested
     * @return True if the mode is a prefix mode, false otherwise.
     */
    public boolean isPrefixMode(final char mode) {
        return modes.isMode(mode);
    }

    /**
     * Determines if the specified character is a prefix for a mode (e.g. '@', '+').
     *
     * @param prefix The prefix to be tested
     * @return True if the character is a prefix, false otherwise.
     */
    public boolean isPrefix(final char prefix) {
        return prefixes.indexOf(prefix) > -1;
    }

    /**
     * Returns the prefix corresponding to the specified mode (e.g. '@' given 'o').
     *
     * @param mode The mode to retrieve the prefix for.
     * @return The prefix corresponding to the mode.
     */
    public char getPrefixFor(final char mode) {
        return prefixes.charAt(modes.getModes().indexOf(mode));
    }

    /**
     * Converts a string containing prefix modes into a string containing the corresponding
     * prefixes (e.g. 'ov' becomes '@+').
     *
     * @param modeString The modes to retrieve prefixes for.
     * @return The prefixes corresponding to the modes.
     */
    public String getPrefixesFor(final String modeString) {
        final StringBuilder builder = new StringBuilder(modeString.length());
        for (char mode : modeString.toCharArray()) {
            builder.append(getPrefixFor(mode));
        }
        return builder.toString();
    }

    /**
     * Returns the mode corresponding to the specified prefix (e.g. 'o' given '@').
     *
     * @param prefix The prefix to retrieve the mode for.
     * @return The mode corresponding to the prefix.
     */
    public char getModeFor(final char prefix) {
        return modes.getModes().charAt(prefixes.indexOf(prefix));
    }

    /**
     * Gets the set of all known prefix modes.
     *
     * @return Set of known modes, in increasing order of importance.
     */
    public String getModes() {
        return modes.getModes();
    }

    /**
     * Gets the set of all known prefixes.
     *
     * @return Set of known prefixes, in increasing order of importance.
     */
    public String getPrefixes() {
        return prefixes;
    }

    /**
     * Adds a new mode. Modes must be added in increasing order of importance.
     *
     * @param mode The mode that appears in mode strings (e.g. 'o').
     * @param prefix The prefix that is used to show a user has the mode (e.g. '@')
     */
    public void add(final char mode, final char prefix) {
        modes.add(mode);
        prefixes += prefix;
    }

    /**
     * Compares the most important mode of the given mode lists.
     *
     * @param modes1 The first set of modes to compare. Must be ordered by importance.
     * @param modes2 The second set of modes to compare. Must be ordered by importance.
     * @return A negative number of modes2 is more important than modes1; a positive number if
     * modes1 is more important than modes2; zero if the two are equivalent.
     */
    public int compareImportantModes(final String modes1, final String modes2) {
        return modes.compareImportantModes(modes1, modes2);
    }

    /**
     * Determines if the specified mode string indicates a user is opped. An opped user is
     * considered one who has any mode greater than 'v' (voice), or if voice doesn't exist then
     * any mode at all.
     *
     * @param modeString The modes to test
     * @return True if the modes indicate the client is "opped", false otherwise.
     */
    public boolean isOpped(final String modeString) {
        return !modeString.isEmpty()
                && modes.getModes().indexOf(modeString.charAt(0)) > modes.getModes().indexOf('v');
    }

    /**
     * Inserts the specified mode into the correct place in the mode string, maintaining importance
     * order.
     *
     * @param modeString The existing modes to add the new one to.
     * @param mode The new mode to be added.
     * @return A mode string containing all the modes.
     */
    public String insertMode(final String modeString, final char mode) {
        return modes.insertMode(modeString, mode);
    }

    /**
     * Removes the specified mode from the mode string.
     *
     * @param modeString The mode string to modify.
     * @param mode The mode to be removed.
     * @return A copy of the mode string with the mode removed.
     */
    public String removeMode(final String modeString, final char mode) {
        return modes.removeMode(modeString, mode);
    }
}
