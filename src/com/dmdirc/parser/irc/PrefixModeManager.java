/*
 * Copyright (c) 2006-2014 DMDirc Developers
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles prefix modes (those that can be applied to a user in a channel, such as +ohv).
 */
public class PrefixModeManager {

    /** Map storing known prefix modes (ohv). */
    private final Map<Character, Long> prefixModes = new HashMap<>();
    /**
     * Map of known prefix modes (ohv) to prefixes (@%+) - Both ways.
     * Prefix map contains 2 pairs for each mode. (eg @ => o and o => @)
     */
    private final Map<Character, Character> prefixMap = new HashMap<>();
    /** The next available number for a prefix mode. */
    private long nextKeyPrefix = 1;

    /**
     * Resets the state of this manager, clearing all known modes.
     */
    public void clear() {
        prefixMap.clear();
        prefixModes.clear();
        nextKeyPrefix = 1;
    }

    /**
     * Determines if the specified character is a prefix mode (e.g. 'o', 'v').
     *
     * @param mode The mode to be tested
     * @return True if the mode is a prefix mode, false otherwise.
     */
    public boolean isPrefixMode(final char mode) {
        return prefixModes.containsKey(mode);
    }

    /**
     * Determines if the specified character is a prefix for a mode (e.g. '@', '+').
     *
     * @param prefix The prefix to be tested
     * @return True if the character is a prefix, false otherwise.
     */
    public boolean isPrefix(final char prefix) {
        return !isPrefixMode(prefix) && prefixMap.containsKey(prefix);
    }

    /**
     * Returns the prefix corresponding to the specified mode (e.g. '@' given 'o').
     *
     * @param mode The mode to retrieve the prefix for.
     * @return The prefix corresponding to the mode.
     */
    public char getPrefixFor(final char mode) {
        return prefixMap.get(mode);
    }

    /**
     * Converts a string containing prefix modes into a string containing the corresponding
     * prefixes (e.g. 'ov' becomes '@+').
     *
     * @param modes The modes to retrieve prefixes for.
     * @return The prefixes corresponding to the modes.
     */
    public String getPrefixesFor(final String modes) {
        final StringBuilder builder = new StringBuilder(modes.length());
        for (char mode : modes.toCharArray()) {
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
        return prefixMap.get(prefix);
    }

    /**
     * Gets the set of all known prefix modes.
     *
     * @return Set of known modes.
     */
    public Set<Character> getModes() {
        return prefixModes.keySet();
    }

    /**
     * Adds a new mode.
     *
     * @param mode The mode that appears in mode strings (e.g. 'o').
     * @param prefix The prefix that is used to show a user has the mode (e.g. '@')
     */
    public void add(final char mode, final char prefix) {
        prefixModes.put(mode, nextKeyPrefix);
        prefixMap.put(mode, prefix);
        prefixMap.put(prefix, mode);
        nextKeyPrefix *= 2;
    }

    /**
     * Gets a unique numerical value for the specified mode. More important modes have higher
     * values.
     *
     * @param mode The mode to return the value of.
     * @return The value of that mode.
     * @deprecated These values are an implementation detail, and shouldn't be exposed.
     */
    @Deprecated
    private long getValueOf(final char mode) {
        return prefixModes.get(mode);
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
        final char mode1 = modes1.isEmpty() ? ' ' : modes1.charAt(0);
        final char mode2 = modes2.isEmpty() ? ' ' : modes2.charAt(0);
        final long modeValue1 = isPrefixMode(mode1) ? getValueOf(mode1) : 0;
        final long modeValue2 = isPrefixMode(mode2) ? getValueOf(mode2) : 0;
        return (int) (modeValue1 - modeValue2);
    }

    /**
     * Determines if the specified mode string indicates a user is opped. An opped user is
     * considered one who has any mode greater than 'v' (voice), or if voice doesn't exist then
     * any mode at all.
     *
     * @param modes The modes to test
     * @return True if the modes indicate the client is "opped", false otherwise.
     */
    public boolean isOpped(final String modes) {
        if (modes.isEmpty()) {
            return false;
        }

        final long voiceValue = isPrefixMode('v') ? prefixModes.get('v') : 0;
        return getValueOf(modes.charAt(0)) > voiceValue;
    }

    /**
     * Inserts the specified mode into the correct place in the mode string, maintaining importance
     * order.
     *
     * @param modes The existing modes to add the new one to.
     * @param mode The new mode to be added.
     * @return A mode string containing all the modes.
     */
    public String insertMode(final String modes, final char mode) {
        if (modes.indexOf(mode) > -1) {
            // Don't duplicate an existing mode
            return modes;
        }

        final StringBuilder result = new StringBuilder(modes.length() + 1);
        boolean found = false;
        final long value = getValueOf(mode);
        for (char existingMode : modes.toCharArray()) {
            if (getValueOf(existingMode) < value && !found) {
                // Our new mode is more important, insert it first.
                result.append(mode);
                found = true;
            }
            result.append(existingMode);
        }
        return result.toString();
    }
}
