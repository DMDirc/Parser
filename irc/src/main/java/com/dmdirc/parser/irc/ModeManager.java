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

package com.dmdirc.parser.irc;

/**
 * Generic mode manager.
 */
public class ModeManager {

    /** All known modes, in increasing order of importance. */
    private String modes = "";

    /**
     * Resets the state of this manager, clearing all known modes.
     */
    public void clear() {
        modes = "";
    }

    /**
     * Replaces all existing modes with the specified ones.
     *
     * @param modes The new modes, in increasing order of importance.
     */
    public void set(final String modes) {
        this.modes = modes;
    }

    /**
     * Adds a new mode. Modes must be added in increasing order of importance.
     *
     * @param mode The mode that appears in mode strings (e.g. 'o').
     */
    public void add(final char mode) {
        modes += mode;
    }

    /**
     * Determines if the specified character is a mode (e.g. 'o', 'v').
     *
     * @param mode The mode to be tested
     * @return True if the mode is a mode, false otherwise.
     */
    public boolean isMode(final char mode) {
        return modes.indexOf(mode) > -1;
    }

    /**
     * Gets the set of all known prefix modes.
     *
     * @return Set of known modes, in increasing order of importance.
     */
    public String getModes() {
        return modes;
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
        final int modeValue1 = modes.indexOf(mode1);
        final int modeValue2 = modes.indexOf(mode2);
        return modeValue1 - modeValue2;
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
        if (modeString.indexOf(mode) > -1) {
            // Don't duplicate an existing mode
            return modeString;
        }

        final StringBuilder result = new StringBuilder(modeString.length() + 1);
        boolean missing = true;
        final int value = modes.indexOf(mode);
        for (char existingMode : modeString.toCharArray()) {
            if (modes.indexOf(existingMode) < value && missing) {
                // Our new mode is more important, insert it first.
                result.append(mode);
                missing = false;
            }
            result.append(existingMode);
        }

        if (missing) {
            result.append(mode);
        }

        return result.toString();
    }

    /**
     * Removes the specified mode from the mode string.
     *
     * @param modeString The mode string to modify.
     * @param mode The mode to be removed.
     * @return A copy of the mode string with the mode removed.
     */
    public String removeMode(final String modeString, final char mode) {
        return modeString.replace(Character.toString(mode), "");
    }

}
