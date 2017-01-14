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

import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.events.NumericEvent;
import com.dmdirc.parser.irc.processors.IRCProcessor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

/**
 * IRC Parser Processing Manager.
 * Manages adding/removing/calling processing stuff.
 */
public class ProcessingManager {

    /** Reference to the parser object that owns this ProcessingManager. */
    private final IRCParser parser;
    /** Hashtable used to store the different types of IRCProcessor known. */
    private final Map<String, IRCProcessor> processHash = new HashMap<>();

    /**
     * Constructor to create a ProcessingManager.
     *
     * @param parser IRCParser that owns this Processing Manager
     * @param processors The processors to add.
     */
    @Inject
    public ProcessingManager(final IRCParser parser, final Set<IRCProcessor> processors) {
        this.parser = parser;
        processors.forEach(this::addProcessor);
    }

    /**
     * Debugging Data to the console.
     */
    private void doDebug(final String line, final Object... args) {
        parser.callDebugInfo(IRCParser.DEBUG_PROCESSOR, line, args);
    }

    /**
     * Add new Process type.
     *
     * @param processor IRCProcessor subclass for the processor.
     */
    public void addProcessor(final IRCProcessor processor) {
        // handles() returns a String array of all the tokens
        // that this processor will parse.
        addProcessor(processor.handles(), processor);
    }

    /**
     * Add a processor to tokens not-specified in the handles() reply.
     *
     * @param processor IRCProcessor subclass for the processor.
     * @param handles String Array of tokens to add this processor as a hadler for
     */
    public void addProcessor(final String[] handles, final IRCProcessor processor) {
        doDebug("Adding processor: " + processor.getName());

        for (String handle : handles) {
            if (processHash.containsKey(handle.toLowerCase())) {
                // New Processors take priority over old ones
                processHash.remove(handle.toLowerCase());
            }
            doDebug("\t Added handler for: " + handle);
            processHash.put(handle.toLowerCase(), processor);
        }
    }

    /**
     * Remove a Process type.
     *
     * @param processor IRCProcessor subclass for the processor.
     */
    public void delProcessor(final IRCProcessor processor) {
        doDebug("Deleting processor: " + processor.getName());
        for (String elementName : processHash.keySet()) {
            doDebug("\t Checking handler for: " + elementName);
            final IRCProcessor testProcessor = processHash.get(elementName);
            if (testProcessor.getName().equalsIgnoreCase(processor.getName())) {
                doDebug("\t Removed handler for: " + elementName);
                processHash.remove(elementName);
            }
        }
    }

    /**
     * Get the processor used for a specified token.
     *
     * @param sParam Type of line to process ("005", "PRIVMSG" etc)
     * @return IRCProcessor for the given param.
     * @throws ProcessorNotFoundException if no processer exists for the param
     */
    public IRCProcessor getProcessor(final String sParam) throws ProcessorNotFoundException {
        if (processHash.containsKey(sParam.toLowerCase())) {
            return processHash.get(sParam.toLowerCase());
        } else {
            throw new ProcessorNotFoundException("No processors will handle " + sParam);
        }
    }

    /**
     * Process a Line.
     *
     * @param sParam Type of line to process ("005", "PRIVMSG" etc)
     * @param token IRCTokenised line to process
     * @throws ProcessorNotFoundException exception if no processors exists to handle the line
     */
    public void process(final String sParam, final String... token) throws ProcessorNotFoundException {
        process(LocalDateTime.now(), sParam, token);
    }

    /**
     * Process a Line.
     *
     * @param date Date of line.
     * @param sParam Type of line to process ("005", "PRIVMSG" etc)
     * @param token IRCTokenised line to process
     * @throws ProcessorNotFoundException exception if no processors exists to handle the line
     */
    public void process(final LocalDateTime date, final String sParam, final String... token)
            throws ProcessorNotFoundException {
        IRCProcessor messageProcessor = null;
        try {
            messageProcessor = getProcessor(sParam);
            if (messageProcessor instanceof TimestampedIRCProcessor) {
                ((TimestampedIRCProcessor)messageProcessor).process(date, sParam, token);
            } else {
                messageProcessor.process(sParam, token);
            }
        } catch (ProcessorNotFoundException p) {
            throw p;
        } catch (Exception e) {
            final ParserError ei = new ParserError(ParserError.ERROR_ERROR,
                    "Exception in Processor. [" + messageProcessor + "]: "
                    + e.getMessage(), parser.getLastLine());
            ei.setException(e);
            parser.callErrorInfo(ei);
        } finally {
            // Try to call callNumeric. We don't want this to work if sParam is a non
            // integer param, hense the empty catch
            try {
                callNumeric(Integer.parseInt(sParam), token);
            } catch (NumberFormatException e) {
            }
        }
    }

    /**
     * Callback to all objects implementing the onNumeric Callback.
     *
     * @param numeric What numeric is this for
     * @param token IRC Tokenised line
     */
    protected void callNumeric(final int numeric, final String... token) {
        parser.getCallbackManager().publish(new NumericEvent(parser, LocalDateTime.now(), numeric,
                token));
    }
}
