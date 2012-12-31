/*
 * Copyright (c) 2006-2013 DMDirc Developers
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
 * Capability states.
 * See: http://ircv3.atheme.org/specification/capability-negotiation-3.1
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
 */
public enum CapabilityState {
    /** Capability is invalid*/
    INVALID("Invalid", (char)0),

    /** Capability is disabled */
    DISABLED("Disabled", '-'),

    /** Capability is pending enabling, (needs ACK) */
    NEED_ACK("Needs ack", '~'),

    /** Capability is enabled entirely */
    ENABLED("Enabled", '+');

    /** Description. */
    private String description;

    /**
     * Modifier.
     * This should probably be called symbol, but the docs refer to the symbols
     * as "capability modifiers". We just use them to make parsing the messages
     * a bit easier.
     */
    private Character modifier;

    /**
     * Create a CapabilityState.
     *
     * @param description Description of capability.
     */
    private CapabilityState(final String description, final Character modifier) {
        this.description = description;
        this.modifier = modifier;
    }

    /**
     * Get the description for this capability state.
     *
     * @return Description for this capability state.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the modifier for this capability state.
     *
     * @return modifier for this capability state.
     */
    public char getModifier() {
        return modifier;
    }

    /**
     * Get the capability state for the given modifier, or null.
     *
     * @return state for the given modifier, or null.
     */
    public static CapabilityState fromModifier(final char modifier) {
        for (CapabilityState cs : CapabilityState.values()) {
            if (cs.getModifier() == modifier) {
                return cs;
            }
        }

        return CapabilityState.INVALID;
    }
}
