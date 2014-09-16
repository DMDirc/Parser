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
    public long getValueOf(final char mode) {
        return prefixModes.get(mode);
    }

    /**
     * Gets the numerical value that will be assigned to the next mode.
     *
     * @return The next numerical value.
     * @deprecated These values are an implementation detail, and shouldn't be exposed.
     */
    @Deprecated
    public long getNextValue() {
        return nextKeyPrefix;
    }

}
