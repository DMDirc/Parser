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
package com.dmdirc.parser.common;

import java.util.ArrayList;
import java.util.Hashtable; // NOPMD - Required by InitialDirContext
import java.util.List;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;

/**
 * Class to represent an SRV Record.
 */
public class SRVRecord implements Comparable<SRVRecord> {
    /** Priority of this record. */
    private final int priority;

    /** Weight of this record. */
    private final int weight;

    /** Port of this record. */
    private final int port;

    /** Host of this record. */
    private final String host;

    /**
     * Create a new SRV Record from a String.
     *
     * @param record Record as a string.
     * @throws NamingException
     */
    public SRVRecord(final String record) throws NamingException {
        final String[] bits = record.split(" ", 4);
        if (bits.length < 4) {
            throw new NamingException("Invalid SRV Record.");
        }
        try {
            priority = Integer.parseInt(bits[0]);
            weight = Integer.parseInt(bits[1]);
            port = Integer.parseInt(bits[2]);
            host = (bits[3].charAt(bits[3].length() - 1) == '.') ? bits[3].substring(0, bits[3].length() - 1) : bits[3];
        } catch (final NumberFormatException nfe) {
            throw new NamingException("Unable to parse SRV Record parameters."); // NOPMD
        }
    }

    /**
     * Get the priority for this SRVRecord.
     *
     * @return The priority for this SRVRecord.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Get the weight for this SRVRecord.
     *
     * @return The weight for this SRVRecord.
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Get the port for this SRVRecord.
     *
     * @return The port for this SRVRecord.
     */
    public int getPort() {
        return port;
    }

    /**
     * Get the host for this SRVRecord.
     *
     * @return The host for this SRVRecord.
     */
    public String getHost() {
        return host;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return priority + " " + weight + " " + port + " " + host + ".";
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(final SRVRecord o) {
        if (this.priority < o.priority) { return -1; }
        if (this.priority > o.priority) { return 1; }
        return 0;
    }

    public static List<SRVRecord> getRecords(final String host) {
        final List<SRVRecord> result = new ArrayList<SRVRecord>();

        try {
            // Obsolete Collection. yeah yeah...
            final Hashtable<String, String> env = new Hashtable<String, String>(); // NOPMD - Required by InitialDirContext
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("java.naming.provider.url", "dns:");
            final Attribute attr = new InitialDirContext(env).getAttributes(host, new String [] { "SRV" }).get("SRV");

            if (attr != null) {
                final NamingEnumeration ne = attr.getAll();
                while (ne.hasMore()) {
                    try {
                        final SRVRecord record = new SRVRecord((String)ne.next());
                        result.add(record);
                    } catch (final NamingException nex) { /* Ignore if invalid. */ }
                }
            }
        } catch (final NamingException nex) { /* Ignore errors. */ }

        // Ideally we should be sorting this list so that it contains all
        // the servers with the lowest priority randomly sorted first, then the
        // next priority etc.
        // However, in general, the DNS resolver will take care of the ordering
        // for us, so we should already get it in a suitable order.
        return result;
    }
}
